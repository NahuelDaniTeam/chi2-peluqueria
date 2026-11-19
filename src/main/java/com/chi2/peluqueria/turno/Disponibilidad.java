package com.chi2.peluqueria.turno;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.chi2.peluqueria.barbero.BarberoServicio;
import com.chi2.peluqueria.barbero.BarberoServicioRepository;
import com.chi2.peluqueria.comun.NoEncontradoException;
import com.chi2.peluqueria.horario.Ausencia;
import com.chi2.peluqueria.horario.AusenciaRepository;
import com.chi2.peluqueria.horario.FranjaHoraria;
import com.chi2.peluqueria.horario.FranjaHorariaRepository;
import com.chi2.peluqueria.negocio.NegocioRepository;
import com.chi2.peluqueria.turno.Tramos.Tramo;
import com.chi2.peluqueria.usuario.Usuario;
import com.chi2.peluqueria.usuario.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Qué horarios quedan libres para un servicio. Un barbero puede atender a una hora si:
 * <ul>
 * <li>atiende, está activo y hace ese servicio;</li>
 * <li>ni el local está cerrado ni él está ausente ese día;</li>
 * <li>la hora cae en su horario: el del local o, si tiene uno propio, el propio dentro del del local;</li>
 * <li>no tiene otro turno en ese momento, y la hora no pasó.</li>
 * </ul>
 * Se ofrecen horarios cada {@link #PASO_MINUTOS} minutos y también justo cuando termina otro turno.
 */
@Service
@Transactional(readOnly = true)
public class Disponibilidad {

	public static final int PASO_MINUTOS = 15;

	/**
	 * Qué se quiere agendar.
	 *
	 * @param barberoId     {@code null} = cualquiera
	 * @param turnoExcluido al reprogramar, el propio turno (no se cuenta como ocupado); si no, {@code null}
	 */
	public record Pedido(Long negocioId, Long servicioId, int duracionMinutos, Long barberoId, Long turnoExcluido) {
	}

	/**
	 * Un día: si el local abre y a qué horas se puede empezar.
	 * Si abre y no hay horas, está completo (o el barbero elegido no viene).
	 */
	public record DiaLibre(LocalDate fecha, boolean abierto, List<LocalTime> horas) {
	}

	private final NegocioRepository negocios;
	private final UsuarioRepository usuarios;
	private final BarberoServicioRepository barberoServicios;
	private final FranjaHorariaRepository franjas;
	private final AusenciaRepository ausencias;
	private final TurnoRepository turnos;

	Disponibilidad(NegocioRepository negocios, UsuarioRepository usuarios, BarberoServicioRepository barberoServicios,
			FranjaHorariaRepository franjas, AusenciaRepository ausencias, TurnoRepository turnos) {
		this.negocios = negocios;
		this.usuarios = usuarios;
		this.barberoServicios = barberoServicios;
		this.franjas = franjas;
		this.ausencias = ausencias;
		this.turnos = turnos;
	}

	/** Los barberos que podrían hacer el pedido (sin mirar horarios): activos, que atienden y hacen el servicio. */
	public List<Usuario> barberos(Long negocioId, Long servicioId) {
		Set<Long> loHacen = barberoServicios.findAllByNegocioIdAndServicioId(negocioId, servicioId).stream()
				.map(BarberoServicio::getUsuarioId).collect(Collectors.toSet());
		return usuarios.findAllByNegocioIdAndActivoTrueAndAtiendeTrueOrderByNombreAsc(negocioId).stream()
				.filter(u -> loHacen.contains(u.getId()))
				.toList();
	}

	/**
	 * Los servicios que alguien que atiende hace (o, con {@code barberoId}, que hace ese barbero): los únicos
	 * para los que se pueden dar turnos.
	 */
	public Set<Long> serviciosConQuienLosHaga(Long negocioId, Long barberoId) {
		Set<Long> atienden = usuarios.findAllByNegocioIdAndActivoTrueAndAtiendeTrueOrderByNombreAsc(negocioId).stream()
				.map(Usuario::getId).filter(id -> barberoId == null || id.equals(barberoId))
				.collect(Collectors.toSet());
		return barberoServicios.findAllByNegocioId(negocioId).stream()
				.filter(bs -> atienden.contains(bs.getUsuarioId()))
				.map(BarberoServicio::getServicioId)
				.collect(Collectors.toSet());
	}

	/** Para cada día de [desde, hasta], las horas libres (de ese barbero o, con cualquiera, de alguno). */
	public Map<LocalDate, DiaLibre> libres(Pedido pedido, LocalDate desde, LocalDate hasta) {
		Calculo calculo = new Calculo(pedido, desde, hasta);
		Map<LocalDate, DiaLibre> dias = new LinkedHashMap<>();
		for (LocalDate fecha = desde; !fecha.isAfter(hasta); fecha = fecha.plusDays(1)) {
			List<Tramo> local = calculo.local(fecha);
			TreeSet<Integer> inicios = new TreeSet<>();
			for (Usuario barbero : calculo.barberos) {
				inicios.addAll(Tramos.inicios(calculo.libresDe(barbero, fecha, local), pedido.duracionMinutos(),
						PASO_MINUTOS, calculo.noAntesDe(fecha)));
			}
			dias.put(fecha, new DiaLibre(fecha, !local.isEmpty(), inicios.stream().map(Tramos::hora).toList()));
		}
		return dias;
	}

	public DiaLibre libres(Pedido pedido, LocalDate fecha) {
		return libres(pedido, fecha, fecha).get(fecha);
	}

	/** Los barberos que pueden atender el pedido empezando a esa hora, por nombre. */
	public List<Usuario> barberosLibres(Pedido pedido, LocalDate fecha, LocalTime hora) {
		Calculo calculo = new Calculo(pedido, fecha, fecha);
		int inicio = Tramos.minutos(hora);
		if (inicio < calculo.noAntesDe(fecha)) {
			return List.of();
		}
		List<Tramo> local = calculo.local(fecha);
		return calculo.barberos.stream()
				.filter(b -> Tramos.entra(calculo.libresDe(b, fecha, local), inicio, pedido.duracionMinutos()))
				.toList();
	}

	/** Todo lo que hace falta para calcular unos días, leído de una vez. */
	private final class Calculo {

		private final ZoneId zona;
		private final LocalDate hoy;
		private final int ahora;
		private final List<Usuario> barberos;
		private final List<FranjaHoraria> franjasDelNegocio;
		private final Set<Long> conHorarioPropio;
		private final List<Ausencia> ausenciasDelRango;
		private final List<Turno> ocupados;

		Calculo(Pedido pedido, LocalDate desde, LocalDate hasta) {
			this.zona = negocios.findById(pedido.negocioId()).orElseThrow(NoEncontradoException::new).getZona();
			LocalDateTime ahoraAca = LocalDateTime.now(zona);
			this.hoy = ahoraAca.toLocalDate();
			// Redondeado para arriba: a las 10:14:30 ya no se ofrece las 10:14
			this.ahora = Tramos.minutos(ahoraAca.toLocalTime()) + 1;
			this.barberos = barberos(pedido.negocioId(), pedido.servicioId()).stream()
					.filter(b -> pedido.barberoId() == null || b.getId().equals(pedido.barberoId()))
					.toList();
			this.franjasDelNegocio = franjas.findAllByNegocioId(pedido.negocioId());
			this.conHorarioPropio = franjasDelNegocio.stream().map(FranjaHoraria::getUsuarioId)
					.filter(Objects::nonNull).collect(Collectors.toSet());
			this.ausenciasDelRango = ausencias.queTocan(pedido.negocioId(), desde, hasta);
			this.ocupados = barberos.isEmpty() ? List.of()
					: turnos.ocupados(pedido.negocioId(), barberos.stream().map(Usuario::getId).toList(),
									EstadoTurno.OCUPAN_LUGAR, inicioDelDia(desde), inicioDelDia(hasta.plusDays(1)))
							.stream().filter(t -> !t.getId().equals(pedido.turnoExcluido())).toList();
		}

		/** El horario del local ese día; vacío si está cerrado. */
		List<Tramo> local(LocalDate fecha) {
			if (ausente(null, fecha)) {
				return List.of();
			}
			return franjasDe(null, fecha);
		}

		/** Lo que tiene libre ese barbero ese día. */
		List<Tramo> libresDe(Usuario barbero, LocalDate fecha, List<Tramo> local) {
			if (local.isEmpty() || ausente(barbero.getId(), fecha)) {
				return List.of();
			}
			List<Tramo> trabaja = conHorarioPropio.contains(barbero.getId())
					? Tramos.interseccion(franjasDe(barbero.getId(), fecha), local)
					: local;
			return Tramos.restar(trabaja, ocupadosDe(barbero.getId(), fecha));
		}

		/** Desde qué minuto de ese día se puede empezar: ya pasado, nada; hoy, desde ahora; después, todo. */
		int noAntesDe(LocalDate fecha) {
			if (fecha.isBefore(hoy)) {
				return Tramos.MINUTOS_DEL_DIA;
			}
			return fecha.equals(hoy) ? ahora : 0;
		}

		private boolean ausente(Long usuarioId, LocalDate fecha) {
			return ausenciasDelRango.stream().anyMatch(a -> Objects.equals(a.getUsuarioId(), usuarioId)
					&& !fecha.isBefore(a.getDesde()) && !fecha.isAfter(a.getHasta()));
		}

		private List<Tramo> franjasDe(Long usuarioId, LocalDate fecha) {
			int dia = fecha.getDayOfWeek().getValue();
			return franjasDelNegocio.stream()
					.filter(f -> Objects.equals(f.getUsuarioId(), usuarioId) && f.getDiaSemana() == dia)
					.map(f -> Tramo.de(f.getDesde(), f.getHasta()))
					.sorted((a, b) -> Integer.compare(a.desde(), b.desde()))
					.toList();
		}

		/** Los turnos de ese barbero, recortados a ese día. */
		private List<Tramo> ocupadosDe(Long barberoId, LocalDate fecha) {
			Instant empieza = inicioDelDia(fecha);
			Instant termina = inicioDelDia(fecha.plusDays(1));
			List<Tramo> tramos = new ArrayList<>();
			for (Turno t : ocupados) {
				if (!t.getBarbero().getId().equals(barberoId) || !t.getInicio().isBefore(termina)
						|| !t.getFin().isAfter(empieza)) {
					continue;
				}
				int desde = t.getInicio().isAfter(empieza) ? minutosDelDia(t.getInicio()) : 0;
				int hasta = t.getFin().isBefore(termina) ? minutosDelDia(t.getFin()) : Tramos.MINUTOS_DEL_DIA;
				if (desde < hasta) {
					tramos.add(new Tramo(desde, hasta));
				}
			}
			return tramos;
		}

		private int minutosDelDia(Instant instante) {
			return Tramos.minutos(LocalDateTime.ofInstant(instante, zona).toLocalTime());
		}

		private Instant inicioDelDia(LocalDate fecha) {
			return fecha.atStartOfDay(zona).toInstant();
		}

	}

}
