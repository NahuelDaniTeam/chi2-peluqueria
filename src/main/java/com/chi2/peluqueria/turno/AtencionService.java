package com.chi2.peluqueria.turno;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.chi2.peluqueria.barbero.BarberoServicio;
import com.chi2.peluqueria.barbero.BarberoServicioRepository;
import com.chi2.peluqueria.comun.AccionBloqueadaException;
import com.chi2.peluqueria.comun.DatoNoDisponibleException;
import com.chi2.peluqueria.comun.NoEncontradoException;
import com.chi2.peluqueria.comun.OperacionNoPermitidaException;
import com.chi2.peluqueria.servicio.CatalogoService;
import com.chi2.peluqueria.servicio.Servicio;
import com.chi2.peluqueria.usuario.Usuario;
import com.chi2.peluqueria.usuario.UsuarioRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * La atención "tipo Uber": empezar, finalizar con el cobro marcado, "no vino", corregir un cobro y la
 * atención sin turno. Igual que {@link AgendaService}, recibe {@code soloDe}: {@code null} para el dueño,
 * el id del barbero para un barbero (que solo toca lo suyo; lo demás le da 404).
 */
@Service
@Transactional
public class AtencionService {

	/** Hasta cuántos días para atrás se buscan turnos que quedaron sin cerrar. */
	public static final int DIAS_SIN_CERRAR = 30;

	static final String HORARIO_TOMADO = "Ese horario ya lo tomó otro turno. Si el cliente vino, cargalo como"
			+ " atención sin turno.";

	/** Lo que se hizo y se cobró. */
	public record Cobro(Long servicio, int monto, MedioPago medioPago) {
	}

	/** Qué se puede hacer con un turno ahora mismo (lo que muestra su pantalla). */
	public record Acciones(boolean empezar, boolean cobrar, boolean ausente, boolean volverAReservado, boolean anular,
			boolean reprogramar, boolean cancelar, boolean corregir) {
	}

	private final AgendaService agenda;
	private final TurnoRepository turnos;
	private final CatalogoService catalogo;
	private final UsuarioRepository usuarios;
	private final BarberoServicioRepository barberoServicios;

	AtencionService(AgendaService agenda, TurnoRepository turnos, CatalogoService catalogo, UsuarioRepository usuarios,
			BarberoServicioRepository barberoServicios) {
		this.agenda = agenda;
		this.turnos = turnos;
		this.catalogo = catalogo;
		this.usuarios = usuarios;
		this.barberoServicios = barberoServicios;
	}

	// ---- Ver ----

	@Transactional(readOnly = true)
	public Acciones acciones(Turno turno, ZoneId zona) {
		Instant ahora = Instant.now();
		boolean sinCerrar = turno.isSinCerrar(ahora);
		return new Acciones(turno.puedeEmpezar(ahora, zona) && !sinCerrar, turno.isEnCurso() || sinCerrar,
				turno.puedeMarcarAusente(ahora), turno.puedeVolverAReservado(), turno.puedeAnularse(),
				turno.isReservado(), turno.isReservado(), turno.isFinalizado());
	}

	/** Lo que cobra ese barbero por ese servicio: su precio especial o el del local. */
	@Transactional(readOnly = true)
	public int precio(Long negocioId, Long barberoId, Servicio servicio) {
		Integer especial = barberoServicios.findByNegocioIdAndUsuarioIdAndServicioId(negocioId, barberoId,
				servicio.getId()).map(BarberoServicio::getPrecioEspecial).orElse(null);
		return especial != null ? especial : servicio.getPrecio();
	}

	/**
	 * Los turnos de días anteriores (hasta {@value #DIAS_SIN_CERRAR} días) que quedaron reservados o en curso:
	 * nadie marcó si se atendieron. Del más nuevo al más viejo.
	 */
	@Transactional(readOnly = true)
	public List<AgendaService.TurnoVista> sinCerrar(Long negocioId, Long barberoId) {
		ZoneId zona = agenda.zona(negocioId);
		LocalDate hoy = LocalDate.now(zona);
		return turnos.conEstadoEntre(negocioId, barberoId, List.of(EstadoTurno.RESERVADO, EstadoTurno.EN_CURSO),
				AgendaService.inicioDelDia(hoy.minusDays(DIAS_SIN_CERRAR), zona), AgendaService.inicioDelDia(hoy, zona))
				.stream().map(t -> AgendaService.vista(t, zona)).toList();
	}

	// ---- Atender ----

	public Turno empezar(Long negocioId, Long soloDe, Long turnoId, Long quien) {
		Turno turno = agenda.buscar(negocioId, soloDe, turnoId);
		Instant ahora = Instant.now();
		if (!turno.puedeEmpezar(ahora, agenda.zona(negocioId))) {
			throw new OperacionNoPermitidaException("Un turno se empieza el mismo día y mientras está reservado");
		}
		turno.empezar(ahora);
		agenda.registrar(turno, quien, TurnoCambio.Accion.EMPEZADO, null);
		return turno;
	}

	/** Lo cierra con el servicio que se hizo (puede ser otro que el reservado), cuánto se cobró y cómo. */
	public Turno finalizar(Long negocioId, Long soloDe, Long turnoId, Long quien, Cobro cobro) {
		Turno turno = agenda.buscar(negocioId, soloDe, turnoId);
		Instant ahora = Instant.now();
		if (!turno.puedeFinalizar(ahora)) {
			throw new OperacionNoPermitidaException("El turno no se puede finalizar: " + turno.getEstado());
		}
		String reservado = turno.getServicio().getNombre();
		Servicio servicio = servicioCobrado(negocioId, turno, cobro);
		turno.finalizar(servicio, cobro.monto(), cobro.medioPago(), ahora);
		agenda.registrar(turno, quien, TurnoCambio.Accion.FINALIZADO, detalleDelCobro(turno,
				turno.isSinTurno() || servicio.getNombre().equals(reservado) ? null : reservado));
		return turno;
	}

	/** Corrige un turno ya finalizado (otro servicio, monto o medio de pago). Si no cambia nada, no hace nada. */
	public Turno corregirCobro(Long negocioId, Long soloDe, Long turnoId, Long quien, Cobro cobro) {
		Turno turno = agenda.buscar(negocioId, soloDe, turnoId);
		if (!turno.isFinalizado()) {
			throw new OperacionNoPermitidaException("Solo se corrige un turno finalizado: " + turno.getEstado());
		}
		Servicio servicio = servicioCobrado(negocioId, turno, cobro);
		if (servicio.getId().equals(turno.getServicio().getId()) && cobro.monto() == turno.getMonto()
				&& cobro.medioPago() == turno.getMedioPago()) {
			return turno;
		}
		String antes = "Antes: " + detalleDelCobro(turno, null);
		turno.corregirCobro(servicio, cobro.monto(), cobro.medioPago());
		agenda.registrar(turno, quien, TurnoCambio.Accion.COBRO_CORREGIDO, antes);
		return turno;
	}

	/** El cliente no vino: el horario que queda vuelve a estar libre. */
	public Turno ausente(Long negocioId, Long soloDe, Long turnoId, Long quien) {
		Turno turno = agenda.buscar(negocioId, soloDe, turnoId);
		Instant ahora = Instant.now();
		if (!turno.puedeMarcarAusente(ahora)) {
			throw new OperacionNoPermitidaException("Todavía no es la hora del turno o ya no está reservado");
		}
		turno.marcarAusente(ahora);
		agenda.registrar(turno, quien, TurnoCambio.Accion.AUSENTE, null);
		return turno;
	}

	/**
	 * Deshace un "Empezar" o un "No vino". Si mientras tanto otro turno tomó ese horario,
	 * {@link AccionBloqueadaException}.
	 */
	public Turno volverAReservado(Long negocioId, Long soloDe, Long turnoId, Long quien) {
		Turno turno = agenda.buscar(negocioId, soloDe, turnoId);
		if (!turno.puedeVolverAReservado()) {
			throw new OperacionNoPermitidaException("No se puede volver a reservado: " + turno.getEstado());
		}
		// Un "No vino" liberó el horario: puede que ya lo haya tomado otro (también alguien sin turno)
		boolean ocupado = turnos.ocupados(negocioId, List.of(turno.getBarbero().getId()), EstadoTurno.OCUPAN_LUGAR,
				turno.getInicio(), turno.getFin()).stream().anyMatch(t -> !t.getId().equals(turno.getId()));
		if (ocupado) {
			throw new AccionBloqueadaException(HORARIO_TOMADO);
		}
		turno.volverAReservado();
		try {
			turnos.saveAndFlush(turno);
		}
		catch (DataIntegrityViolationException e) {
			throw new AccionBloqueadaException(HORARIO_TOMADO);
		}
		agenda.registrar(turno, quien, TurnoCambio.Accion.VUELTO_A_RESERVADO, null);
		return turno;
	}

	/** Una atención sin turno cargada por error (o el cliente se fue sin atenderse). */
	public Turno anular(Long negocioId, Long soloDe, Long turnoId, Long quien) {
		Turno turno = agenda.buscar(negocioId, soloDe, turnoId);
		if (!turno.puedeAnularse()) {
			throw new OperacionNoPermitidaException("Solo se anula una atención sin turno en curso");
		}
		turno.anular();
		agenda.registrar(turno, quien, TurnoCambio.Accion.ANULADO, null);
		return turno;
	}

	/**
	 * Alguien que entró sin reservar. Sin {@code cobro} empieza ahora y se finaliza después, como cualquier
	 * turno; con {@code cobro} se carga ya atendido (terminó recién).
	 *
	 * @param barberoId quién lo atiende ({@code null} para un barbero, que se lo carga a sí mismo)
	 */
	public Turno sinTurno(Long negocioId, Long soloDe, Long quien, Long barberoId, Long servicioId, String nombre,
			Cobro cobro) {
		if (soloDe != null) {
			if (barberoId != null && !barberoId.equals(soloDe)) {
				throw new OperacionNoPermitidaException("Un barbero solo se carga atenciones a sí mismo");
			}
			barberoId = soloDe;
		}
		if (barberoId == null) {
			throw new DatoNoDisponibleException("barbero", "Elegí quién lo atiende.");
		}
		Usuario barbero = usuarios.findByIdAndNegocioId(barberoId, negocioId).orElseThrow(NoEncontradoException::new);
		if (!barbero.isActivo() || !barbero.isAtiende()) {
			throw new OperacionNoPermitidaException("Solo atiende quien figura como que atiende");
		}
		if (servicioId == null) {
			throw new DatoNoDisponibleException("servicio", "Elegí el servicio.");
		}
		Servicio servicio = catalogo.buscar(negocioId, servicioId);
		if (!servicio.isActivo()) {
			throw new OperacionNoPermitidaException("El servicio ya no se ofrece");
		}

		Instant ahora = Instant.now().truncatedTo(ChronoUnit.MINUTES);
		Duration duracion = Duration.ofMinutes(servicio.getDuracionMinutos());
		// Ya atendido: ocupó el rato que acaba de pasar
		Turno turno = Turno.sinTurno(negocioId, barbero, servicio, cobro == null ? ahora : ahora.minus(duracion),
				duracion, nombre, quien);
		turnos.saveAndFlush(turno);
		agenda.registrar(turno, quien, TurnoCambio.Accion.SIN_TURNO, null);
		if (cobro != null) {
			turno.finalizar(servicioCobrado(negocioId, turno, cobro), cobro.monto(), cobro.medioPago(), ahora);
			agenda.registrar(turno, quien, TurnoCambio.Accion.FINALIZADO, detalleDelCobro(turno, null));
		}
		return turno;
	}

	// ---- Ayudas ----

	/** El servicio del cobro: uno del catálogo que se ofrezca, o el que ya tenía el turno aunque se haya quitado. */
	private Servicio servicioCobrado(Long negocioId, Turno turno, Cobro cobro) {
		if (cobro.servicio() == null || cobro.servicio().equals(turno.getServicio().getId())) {
			return turno.getServicio();
		}
		Servicio servicio = catalogo.buscar(negocioId, cobro.servicio());
		if (!servicio.isActivo()) {
			throw new OperacionNoPermitidaException("El servicio ya no se ofrece");
		}
		return servicio;
	}

	/** "Corte + barba · $ 15.000 · Efectivo." o, si cambió el servicio, "... (reservado: Corte)." */
	private static String detalleDelCobro(Turno turno, String servicioReservado) {
		String detalle = turno.getServicio().getNombre() + " · " + turno.getCobroTexto();
		return servicioReservado == null ? detalle + "." : detalle + " (reservado: " + servicioReservado + ").";
	}

}
