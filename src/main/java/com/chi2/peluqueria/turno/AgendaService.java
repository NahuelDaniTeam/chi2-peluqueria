package com.chi2.peluqueria.turno;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.chi2.peluqueria.comun.AccionBloqueadaException;
import com.chi2.peluqueria.comun.DatoNoDisponibleException;
import com.chi2.peluqueria.comun.Formatos;
import com.chi2.peluqueria.comun.NoEncontradoException;
import com.chi2.peluqueria.comun.OperacionNoPermitidaException;
import com.chi2.peluqueria.negocio.NegocioRepository;
import com.chi2.peluqueria.servicio.CatalogoService;
import com.chi2.peluqueria.servicio.Servicio;
import com.chi2.peluqueria.usuario.EquipoService;
import com.chi2.peluqueria.usuario.Usuario;
import com.chi2.peluqueria.usuario.UsuarioRepository;

import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * La agenda de una barbería: ver, reservar, cancelar y reprogramar turnos.
 * <p>
 * Todos los métodos reciben el negocio del usuario con sesión iniciada y {@code soloDe}: {@code null}
 * para el dueño (ve y toca todo el negocio) o el id del barbero, que solo ve y toca sus turnos.
 * Un turno de otro barbero o de otra barbería responde 404.
 */
@Service
@Transactional
public class AgendaService {

	/** Hasta cuántos días para adelante se dan turnos. */
	public static final int DIAS_DE_ANTICIPACION = 90;

	/** Con menos aviso que esto, una cancelación queda marcada como tardía. */
	public static final Duration AVISO_PARA_CANCELAR = Duration.ofHours(24);

	static final String HORARIO_OCUPADO = "Ese horario ya no está libre. Elegí otro.";

	/** Un turno con sus horas ya pasadas a la hora de la barbería, para mostrar. */
	public record TurnoVista(Turno turno, LocalDate fecha, LocalTime desde, LocalTime hasta) {

		public String getFechaTexto() {
			return Formatos.fechaCorta(fecha);
		}

		public String getDesdeTexto() {
			return Formatos.hora(desde);
		}

		public String getHastaTexto() {
			return Formatos.hora(hasta);
		}

		/** Reservado y ya pasó su horario: falta marcar si se atendió, si no vino o si se canceló. */
		public boolean isSinCerrar() {
			return turno.isSinCerrar(Instant.now());
		}

	}

	/** Una entrada del historial, con la hora de la barbería. */
	public record CambioVista(TurnoCambio cambio, LocalDateTime cuando) {

		public String getCuandoTexto() {
			return Formatos.fechaHora(cuando);
		}

	}

	private final TurnoRepository turnos;
	private final TurnoCambioRepository cambios;
	private final Disponibilidad disponibilidad;
	private final CatalogoService catalogo;
	private final NegocioRepository negocios;
	private final UsuarioRepository usuarios;

	AgendaService(TurnoRepository turnos, TurnoCambioRepository cambios, Disponibilidad disponibilidad,
			CatalogoService catalogo, NegocioRepository negocios, UsuarioRepository usuarios) {
		this.turnos = turnos;
		this.cambios = cambios;
		this.disponibilidad = disponibilidad;
		this.catalogo = catalogo;
		this.negocios = negocios;
		this.usuarios = usuarios;
	}

	// ---- Ver ----

	@Transactional(readOnly = true)
	public ZoneId zona(Long negocioId) {
		return negocios.findById(negocioId).orElseThrow(NoEncontradoException::new).getZona();
	}

	@Transactional(readOnly = true)
	public LocalDate hoy(Long negocioId) {
		return LocalDate.now(zona(negocioId));
	}

	/** Los turnos que empiezan ese día, por hora ({@code barberoId} nulo = de todos). */
	@Transactional(readOnly = true)
	public List<TurnoVista> delDia(Long negocioId, LocalDate fecha, Long barberoId) {
		ZoneId zona = zona(negocioId);
		return turnos.queEmpiezanEntre(negocioId, barberoId, inicioDelDia(fecha, zona), inicioDelDia(fecha.plusDays(1), zona))
				.stream().map(t -> vista(t, zona)).toList();
	}

	/** Cuántos turnos (de los que ocupan lugar) empiezan cada día de [desde, hasta]. */
	@Transactional(readOnly = true)
	public Map<LocalDate, Long> cantidadPorDia(Long negocioId, LocalDate desde, LocalDate hasta, Long barberoId) {
		ZoneId zona = zona(negocioId);
		Map<LocalDate, Long> cantidades = new TreeMap<>(turnos
				.queEmpiezanEntre(negocioId, barberoId, inicioDelDia(desde, zona), inicioDelDia(hasta.plusDays(1), zona))
				.stream().filter(t -> t.getEstado().ocupaLugar())
				.collect(Collectors.groupingBy(t -> LocalDate.ofInstant(t.getInicio(), zona), Collectors.counting())));
		for (LocalDate d = desde; !d.isAfter(hasta); d = d.plusDays(1)) {
			cantidades.putIfAbsent(d, 0L);
		}
		return cantidades;
	}

	@Transactional(readOnly = true)
	public TurnoVista ver(Long negocioId, Long soloDe, Long turnoId) {
		return vista(buscar(negocioId, soloDe, turnoId), zona(negocioId));
	}

	@Transactional(readOnly = true)
	public List<CambioVista> historial(Long negocioId, Long soloDe, Long turnoId) {
		buscar(negocioId, soloDe, turnoId);
		ZoneId zona = zona(negocioId);
		return cambios.historial(negocioId, turnoId).stream()
				.map(c -> new CambioVista(c, LocalDateTime.ofInstant(c.getCreadoEn(), zona)))
				.toList();
	}

	/** Si cancelarlo ahora sería una cancelación tardía (faltan menos de 24 horas). */
	@Transactional(readOnly = true)
	public boolean cancelarSeriaTardio(Turno turno) {
		return Duration.between(Instant.now(), turno.getInicio()).compareTo(AVISO_PARA_CANCELAR) < 0;
	}

	/** Cuántos turnos reservados hay en esos días (de un barbero o, con {@code barberoId} nulo, de todos). */
	@Transactional(readOnly = true)
	public long reservadosEntre(Long negocioId, Long barberoId, LocalDate desde, LocalDate hasta) {
		ZoneId zona = zona(negocioId);
		return turnos.contarReservados(negocioId, barberoId, inicioDelDia(desde, zona),
				inicioDelDia(hasta.plusDays(1), zona));
	}

	// ---- Reservar, cancelar, reprogramar ----

	/**
	 * Reserva un turno. Con barbero "cualquiera", se lo da al que está libre a esa hora y tiene menos
	 * turnos ese día. Si el horario no está libre, {@link DatoNoDisponibleException} sobre "hora".
	 */
	public Turno reservar(Long negocioId, Long soloDe, Long quienLoCarga, ReservaForm form) {
		Long barberoId = form.getBarbero();
		if (soloDe != null) {
			if (barberoId != null && !barberoId.equals(soloDe)) {
				throw new OperacionNoPermitidaException("Un barbero solo carga turnos para él");
			}
			barberoId = soloDe;
		}
		if (form.getServicio() == null) {
			throw new OperacionNoPermitidaException("Falta el servicio");
		}
		Servicio servicio = catalogo.buscar(negocioId, form.getServicio());
		if (!servicio.isActivo()) {
			throw new OperacionNoPermitidaException("El servicio ya no se ofrece");
		}
		exigirFechaYHora(negocioId, form.getFecha(), form.getHora());

		Disponibilidad.Pedido pedido = new Disponibilidad.Pedido(negocioId, servicio.getId(),
				servicio.getDuracionMinutos(), barberoId, null);
		List<Usuario> libres = disponibilidad.barberosLibres(pedido, form.getFecha(), form.getHora());
		if (libres.isEmpty()) {
			throw new DatoNoDisponibleException("hora", HORARIO_OCUPADO);
		}
		Usuario barbero = libres.size() == 1 ? libres.get(0) : elMenosOcupado(negocioId, libres, form.getFecha());

		Turno turno = new Turno(negocioId, barbero, servicio, instante(negocioId, form.getFecha(), form.getHora()),
				Duration.ofMinutes(servicio.getDuracionMinutos()), form.getNombre(), form.getTelefono(), form.getNota(),
				quienLoCarga);
		guardar(turno);
		registrar(turno, quienLoCarga, TurnoCambio.Accion.CREADO, null);
		return turno;
	}

	/** Cancela un turno reservado. Con menos de 24 horas de aviso, queda como cancelación tardía. */
	public Turno cancelar(Long negocioId, Long soloDe, Long turnoId, Long quienCancela) {
		Turno turno = buscar(negocioId, soloDe, turnoId);
		exigirReservado(turno);
		boolean tardia = cancelarSeriaTardio(turno);
		turno.cancelar(tardia);
		registrar(turno, quienCancela, TurnoCambio.Accion.CANCELADO, tardia ? "Con menos de 24 h de aviso." : null);
		return turno;
	}

	/** Lo pasa a otro día u hora y, si lo pide el dueño, a otro barbero ({@code barbero} nulo = el mismo). */
	public Turno reprogramar(Long negocioId, Long soloDe, Long turnoId, Long quienLoCambia, ReprogramacionForm form) {
		Turno turno = buscar(negocioId, soloDe, turnoId);
		exigirReservado(turno);
		Long barberoId = form.getBarbero() != null ? form.getBarbero() : turno.getBarbero().getId();
		if (soloDe != null && !barberoId.equals(soloDe)) {
			throw new OperacionNoPermitidaException("Un barbero no pasa sus turnos a otro");
		}
		exigirFechaYHora(negocioId, form.getFecha(), form.getHora());

		Disponibilidad.Pedido pedido = new Disponibilidad.Pedido(negocioId, turno.getServicio().getId(),
				(int) turno.getDuracion().toMinutes(), barberoId, turno.getId());
		List<Usuario> libres = disponibilidad.barberosLibres(pedido, form.getFecha(), form.getHora());
		if (libres.isEmpty()) {
			throw new DatoNoDisponibleException("hora", HORARIO_OCUPADO);
		}
		Usuario barbero = libres.get(0);
		Instant inicio = instante(negocioId, form.getFecha(), form.getHora());
		if (barbero.getId().equals(turno.getBarbero().getId()) && inicio.equals(turno.getInicio())) {
			return turno; // Eligió el mismo horario: no cambia nada
		}
		TurnoVista antes = vista(turno, zona(negocioId));
		String detalle = "Antes: " + antes.getFechaTexto() + " " + antes.getDesdeTexto() + " con "
				+ turno.getBarbero().getNombre() + ".";
		turno.reprogramar(barbero, inicio);
		guardar(turno);
		registrar(turno, quienLoCambia, TurnoCambio.Accion.REPROGRAMADO, detalle);
		return turno;
	}

	/** Nadie sale de la agenda (baja o "no atiende") con turnos reservados de acá en adelante. */
	@EventListener
	void alSalirDeLaAgenda(EquipoService.SaleDeLaAgenda evento) {
		long pendientes = turnos.countByNegocioIdAndBarbero_IdAndEstadoAndFinAfter(evento.negocioId(),
				evento.usuarioId(), EstadoTurno.RESERVADO, Instant.now());
		if (pendientes > 0) {
			throw new AccionBloqueadaException(evento.nombre() + " tiene " + pendientes
					+ (pendientes == 1 ? " turno reservado" : " turnos reservados")
					+ " de hoy en adelante. Reprogramalos o cancelalos antes.");
		}
	}

	// ---- Ayudas ----

	Turno buscar(Long negocioId, Long soloDe, Long turnoId) {
		Turno turno = turnos.buscar(negocioId, turnoId).orElseThrow(NoEncontradoException::new);
		if (soloDe != null && !turno.getBarbero().getId().equals(soloDe)) {
			throw new NoEncontradoException();
		}
		return turno;
	}

	private void exigirReservado(Turno turno) {
		if (!turno.isReservado()) {
			throw new OperacionNoPermitidaException("El turno ya no está reservado: " + turno.getEstado());
		}
	}

	private void exigirFechaYHora(Long negocioId, LocalDate fecha, LocalTime hora) {
		if (hora == null) {
			throw new DatoNoDisponibleException("hora", "Elegí un horario.");
		}
		LocalDate hoy = hoy(negocioId);
		if (fecha == null || fecha.isBefore(hoy) || fecha.isAfter(hoy.plusDays(DIAS_DE_ANTICIPACION))) {
			throw new DatoNoDisponibleException("hora",
					"Elegí un día de hoy a " + DIAS_DE_ANTICIPACION + " días para adelante.");
		}
	}

	/** Entre los libres, el que tiene menos turnos ese día (si empatan, el primero por nombre). */
	private Usuario elMenosOcupado(Long negocioId, List<Usuario> libres, LocalDate fecha) {
		Map<Long, Long> cantidades = delDia(negocioId, fecha, null).stream()
				.filter(v -> v.turno().getEstado().ocupaLugar())
				.collect(Collectors.groupingBy(v -> v.turno().getBarbero().getId(), Collectors.counting()));
		return libres.stream()
				.min(Comparator.comparing((Usuario u) -> cantidades.getOrDefault(u.getId(), 0L)))
				.orElseThrow();
	}

	/** La base es la que de verdad impide superposiciones: si dos personas reservan a la vez, gana una. */
	private void guardar(Turno turno) {
		try {
			turnos.saveAndFlush(turno);
		}
		catch (DataIntegrityViolationException e) {
			throw new DatoNoDisponibleException("hora", HORARIO_OCUPADO);
		}
	}

	void registrar(Turno turno, Long usuarioId, TurnoCambio.Accion accion, String detalle) {
		Usuario quien = usuarioId == null ? null : usuarios.getReferenceById(usuarioId);
		cambios.save(new TurnoCambio(turno, quien, accion, detalle));
	}

	private Instant instante(Long negocioId, LocalDate fecha, LocalTime hora) {
		return fecha.atTime(hora).atZone(zona(negocioId)).toInstant();
	}

	static Instant inicioDelDia(LocalDate fecha, ZoneId zona) {
		return fecha.atStartOfDay(zona).toInstant();
	}

	static TurnoVista vista(Turno turno, ZoneId zona) {
		LocalDateTime desde = LocalDateTime.ofInstant(turno.getInicio(), zona);
		LocalDateTime hasta = LocalDateTime.ofInstant(turno.getFin(), zona);
		return new TurnoVista(turno, desde.toLocalDate(), desde.toLocalTime(), hasta.toLocalTime());
	}

}
