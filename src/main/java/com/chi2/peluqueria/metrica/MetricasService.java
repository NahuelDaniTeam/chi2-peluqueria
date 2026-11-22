package com.chi2.peluqueria.metrica;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.chi2.peluqueria.comun.Formatos;
import com.chi2.peluqueria.turno.AgendaService;
import com.chi2.peluqueria.turno.EstadoTurno;
import com.chi2.peluqueria.turno.MedioPago;
import com.chi2.peluqueria.turno.Turno;
import com.chi2.peluqueria.turno.TurnoRepository;
import com.chi2.peluqueria.usuario.Usuario;
import com.chi2.peluqueria.usuario.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Los números de un período: cuánto se atendió y se cobró (y cómo), por servicio, por barbero y por día,
 * más los que no vinieron y las cancelaciones. Un turno cuenta en el día en que empezó, en la hora de la
 * barbería. Lo cobrado sale de los turnos finalizados (con o sin turno); el sistema no maneja dinero, así que
 * es lo que cada barbero marcó.
 */
@Service
@Transactional(readOnly = true)
public class MetricasService {

	/** Lo que se hizo de un servicio. */
	public record PorServicio(String nombre, int cantidad, long cobrado) {

		public String getCobradoTexto() {
			return Formatos.precio(cobrado);
		}

	}

	/** Lo de un barbero, para compararlo con los demás. */
	public record PorBarbero(Long id, String nombre, int atenciones, long cobrado, int ausentes) {

		public String getCobradoTexto() {
			return Formatos.precio(cobrado);
		}

		/** "15 atenciones · 1 no vino" */
		public String getDetalle() {
			String texto = atenciones + (atenciones == 1 ? " atención" : " atenciones");
			return ausentes == 0 ? texto : texto + " · " + ausentes + (ausentes == 1 ? " no vino" : " no vinieron");
		}

	}

	/** Un día del período. */
	public record PorDia(LocalDate fecha, int atenciones, long cobrado) {

		public String getCobradoTexto() {
			return Formatos.precio(cobrado);
		}

		public String getFechaTexto() {
			return Formatos.fechaCorta(fecha);
		}

	}

	/**
	 * Todos los números de un período.
	 *
	 * @param sinTurno     de las atenciones, cuántas fueron de gente que entró sin reservar
	 * @param cancelados   cancelados a tiempo y tarde (sin contar la atención sin turno anulada)
	 * @param sinCerrar    turnos cuya hora ya pasó y nadie marcó si se atendieron: todavía no cuentan
	 * @param porDelante   turnos reservados que todavía no llegaron
	 */
	public record Resumen(int atenciones, int sinTurno, long cobrado, long efectivo, long transferencia,
			int ausentes, int cancelados, int canceladosTarde, int sinCerrar, int porDelante,
			List<PorServicio> servicios, List<PorBarbero> barberos, List<PorDia> dias) {

		public String getCobradoTexto() {
			return Formatos.precio(cobrado);
		}

		public String getEfectivoTexto() {
			return Formatos.precio(efectivo);
		}

		public String getTransferenciaTexto() {
			return Formatos.precio(transferencia);
		}

		/** Lo cobrado en promedio por atención. */
		public String getPromedioTexto() {
			return atenciones == 0 ? Formatos.precio(0) : Formatos.precio(Math.round((double) cobrado / atenciones));
		}

		/** Qué parte de lo cobrado fue en efectivo, de 0 a 100. */
		public int getPorcentajeEfectivo() {
			return cobrado == 0 ? 0 : (int) Math.round(efectivo * 100.0 / cobrado);
		}

		public int getPorcentajeTransferencia() {
			return cobrado == 0 ? 0 : 100 - getPorcentajeEfectivo();
		}

		/** Lo más cobrado en un día (para la escala del gráfico). */
		public long getMaximoPorDia() {
			return dias.stream().mapToLong(PorDia::cobrado).max().orElse(0);
		}

		public long getMaximoPorBarbero() {
			return barberos.stream().mapToLong(PorBarbero::cobrado).max().orElse(0);
		}

		public int getMaximoPorServicio() {
			return servicios.stream().mapToInt(PorServicio::cantidad).max().orElse(0);
		}

		public boolean isVacio() {
			return atenciones == 0 && ausentes == 0 && cancelados == 0 && sinCerrar == 0 && porDelante == 0;
		}

	}

	private final TurnoRepository turnos;
	private final AgendaService agenda;
	private final UsuarioRepository usuarios;

	MetricasService(TurnoRepository turnos, AgendaService agenda, UsuarioRepository usuarios) {
		this.turnos = turnos;
		this.agenda = agenda;
		this.usuarios = usuarios;
	}

	/**
	 * Los números del período, de un barbero o de todos ({@code barberoId} nulo). Con todos, la comparación por
	 * barbero incluye a los que atienden aunque no hayan hecho nada.
	 */
	public Resumen resumen(Long negocioId, Long barberoId, Periodo periodo) {
		ZoneId zona = agenda.zona(negocioId);
		List<Turno> delPeriodo = turnos.queEmpiezanEntre(negocioId, barberoId,
				periodo.desde().atStartOfDay(zona).toInstant(), periodo.hasta().plusDays(1).atStartOfDay(zona).toInstant());
		Instant ahora = Instant.now();

		int atenciones = 0;
		int sinTurno = 0;
		long efectivo = 0;
		long transferencia = 0;
		int ausentes = 0;
		int cancelados = 0;
		int canceladosTarde = 0;
		int sinCerrar = 0;
		int porDelante = 0;
		Map<Long, int[]> cantidadPorServicio = new LinkedHashMap<>();
		Map<Long, long[]> cobradoPorServicio = new LinkedHashMap<>();
		Map<Long, String> nombreDeServicio = new LinkedHashMap<>();
		Map<Long, long[]> porBarbero = new LinkedHashMap<>(); // atenciones, cobrado, ausentes
		Map<Long, String> nombreDeBarbero = new LinkedHashMap<>();
		Map<LocalDate, long[]> porDia = new LinkedHashMap<>(); // atenciones, cobrado
		periodo.dias().forEach(d -> porDia.put(d, new long[2]));

		for (Turno t : delPeriodo) {
			Long barbero = t.getBarbero().getId();
			nombreDeBarbero.putIfAbsent(barbero, t.getBarbero().getNombre());
			long[] delBarbero = porBarbero.computeIfAbsent(barbero, k -> new long[3]);
			switch (t.getEstado()) {
				case FINALIZADO -> {
					int monto = t.getMonto();
					atenciones++;
					if (t.isSinTurno()) {
						sinTurno++;
					}
					if (t.getMedioPago() == MedioPago.EFECTIVO) {
						efectivo += monto;
					}
					else {
						transferencia += monto;
					}
					Long servicio = t.getServicio().getId();
					nombreDeServicio.putIfAbsent(servicio, t.getServicio().getNombre());
					cantidadPorServicio.computeIfAbsent(servicio, k -> new int[1])[0]++;
					cobradoPorServicio.computeIfAbsent(servicio, k -> new long[1])[0] += monto;
					delBarbero[0]++;
					delBarbero[1] += monto;
					long[] delDia = porDia.get(LocalDate.ofInstant(t.getInicio(), zona));
					if (delDia != null) {
						delDia[0]++;
						delDia[1] += monto;
					}
				}
				case AUSENTE -> {
					ausentes++;
					delBarbero[2]++;
				}
				case CANCELADO -> {
					if (!t.isSinTurno()) {
						cancelados++;
						if (t.isCancelacionTardia()) {
							canceladosTarde++;
						}
					}
				}
				case RESERVADO, EN_CURSO -> {
					if (t.getFin().isAfter(ahora)) {
						if (t.getEstado() == EstadoTurno.RESERVADO) {
							porDelante++;
						}
					}
					else {
						sinCerrar++;
					}
				}
			}
		}

		List<PorServicio> servicios = cantidadPorServicio.entrySet().stream()
				.map(e -> new PorServicio(nombreDeServicio.get(e.getKey()), e.getValue()[0],
						cobradoPorServicio.get(e.getKey())[0]))
				.sorted(Comparator.comparingInt(PorServicio::cantidad).reversed()
						.thenComparing(Comparator.comparingLong(PorServicio::cobrado).reversed()))
				.toList();

		List<PorBarbero> barberos = new ArrayList<>();
		if (barberoId == null) {
			for (Usuario u : usuarios.findAllByNegocioIdAndActivoTrueAndAtiendeTrueOrderByNombreAsc(negocioId)) {
				nombreDeBarbero.putIfAbsent(u.getId(), u.getNombre());
				porBarbero.computeIfAbsent(u.getId(), k -> new long[3]);
			}
			porBarbero.forEach((id, n) -> barberos.add(new PorBarbero(id, nombreDeBarbero.get(id), (int) n[0], n[1],
					(int) n[2])));
			barberos.sort(Comparator.comparingLong(PorBarbero::cobrado).reversed()
					.thenComparing(Comparator.comparingInt(PorBarbero::atenciones).reversed())
					.thenComparing(PorBarbero::nombre));
		}

		List<PorDia> dias = porDia.entrySet().stream()
				.map(e -> new PorDia(e.getKey(), (int) e.getValue()[0], e.getValue()[1])).toList();

		return new Resumen(atenciones, sinTurno, efectivo + transferencia, efectivo, transferencia, ausentes,
				cancelados, canceladosTarde, sinCerrar, porDelante, servicios, barberos, dias);
	}

}
