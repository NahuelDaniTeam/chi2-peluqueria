package com.chi2.peluqueria.turno;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.chi2.peluqueria.comun.Formatos;
import com.chi2.peluqueria.horario.Ausencia;
import com.chi2.peluqueria.horario.AusenciaRepository;
import com.chi2.peluqueria.pagina.PuestaEnMarcha;
import com.chi2.peluqueria.seguridad.UsuarioAutenticado;
import com.chi2.peluqueria.usuario.Usuario;
import com.chi2.peluqueria.usuario.UsuarioRepository;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * La agenda de un día, con la semana arriba para saltar de un día a otro. El dueño ve la de todos (o
 * filtra por barbero); un barbero ve solo la suya. Es la pantalla con la que arranca todo el mundo.
 */
@Controller
@RequestMapping("/agenda")
class AgendaController {

	/** Un día en la tira de la semana. */
	record DiaDeLaSemana(String letra, int numero, long turnos, String enlace, boolean elegido, boolean hoy) {
	}

	private static final String[] LETRAS = { "L", "M", "M", "J", "V", "S", "D" };

	private final AgendaService agenda;
	private final UsuarioRepository usuarios;
	private final AusenciaRepository ausencias;
	private final PuestaEnMarcha puestaEnMarcha;

	AgendaController(AgendaService agenda, UsuarioRepository usuarios, AusenciaRepository ausencias,
			PuestaEnMarcha puestaEnMarcha) {
		this.agenda = agenda;
		this.usuarios = usuarios;
		this.ausencias = ausencias;
		this.puestaEnMarcha = puestaEnMarcha;
	}

	@GetMapping
	String ver(@AuthenticationPrincipal UsuarioAutenticado yo,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
			@RequestParam(required = false) Long barbero, Model model) {
		Long negocioId = yo.getNegocioId();
		LocalDate hoy = agenda.hoy(negocioId);
		LocalDate dia = fecha != null ? fecha : hoy;
		List<Usuario> atienden = usuarios.findAllByNegocioIdAndActivoTrueAndAtiendeTrueOrderByNombreAsc(negocioId);

		// El barbero ve solo lo suyo; el dueño, todo o el barbero que elija
		Long filtro = yo.isDueno() ? (atienden.stream().anyMatch(u -> u.getId().equals(barbero)) ? barbero : null)
				: yo.getId();
		// En los links solo viaja el barbero que eligió el dueño
		Long enLinks = yo.isDueno() ? filtro : null;

		List<AgendaService.TurnoVista> turnos = agenda.delDia(negocioId, dia, filtro);
		model.addAttribute("turnos", turnos.stream().filter(t -> t.turno().getEstado().ocupaLugar()).toList());
		model.addAttribute("sinLugar", turnos.stream().filter(t -> !t.turno().getEstado().ocupaLugar()).toList());

		model.addAttribute("fecha", dia);
		model.addAttribute("fechaTexto", Formatos.fechaLarga(dia));
		model.addAttribute("esHoy", dia.equals(hoy));
		model.addAttribute("enlaceHoy", enlace("/agenda", hoy, enLinks));
		model.addAttribute("semana", semana(negocioId, dia, hoy, filtro, enLinks));
		model.addAttribute("semanaAnterior", enlace("/agenda", dia.minusWeeks(1), enLinks));
		model.addAttribute("semanaSiguiente", enlace("/agenda", dia.plusWeeks(1), enLinks));
		model.addAttribute("verTodos", yo.isDueno() && filtro == null);
		if (yo.isDueno()) {
			model.addAttribute("barberos", barberos(atienden, dia, filtro));
			List<PuestaEnMarcha.Paso> pasos = puestaEnMarcha.pasos(negocioId);
			model.addAttribute("pasos", pasos);
			model.addAttribute("pasosCompletos", pasos.stream().allMatch(PuestaEnMarcha.Paso::hecho));
		}
		agregarAusencias(negocioId, dia, filtro, atienden, model);

		boolean atiendo = atienden.stream().anyMatch(u -> u.getId().equals(yo.getId()));
		model.addAttribute("puedeReservar", yo.isDueno() || atiendo);
		model.addAttribute("atiendo", atiendo);
		model.addAttribute("enlaceNuevo", enlace("/agenda/nuevo", dia.isBefore(hoy) ? hoy : dia, enLinks));
		return "agenda/agenda";
	}

	/** Lunes a domingo de la semana de {@code dia}, con cuántos turnos tiene cada uno. */
	private List<DiaDeLaSemana> semana(Long negocioId, LocalDate dia, LocalDate hoy, Long filtro, Long enLinks) {
		LocalDate lunes = dia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		Map<LocalDate, Long> cantidades = agenda.cantidadPorDia(negocioId, lunes, lunes.plusDays(6), filtro);
		List<DiaDeLaSemana> semana = new ArrayList<>();
		for (int i = 0; i < 7; i++) {
			LocalDate d = lunes.plusDays(i);
			semana.add(new DiaDeLaSemana(LETRAS[i], d.getDayOfMonth(), cantidades.getOrDefault(d, 0L),
					enlace("/agenda", d, enLinks), d.equals(dia), d.equals(hoy)));
		}
		return semana;
	}

	/** "Todos" y cada uno que atiende (solo para el dueño). */
	private List<Opcion> barberos(List<Usuario> atienden, LocalDate dia, Long filtro) {
		List<Opcion> opciones = new ArrayList<>();
		opciones.add(new Opcion("Todos", null, enlace("/agenda", dia, null), filtro == null, false));
		for (Usuario u : atienden) {
			opciones.add(new Opcion(u.getNombre(), null, enlace("/agenda", dia, u.getId()), u.getId().equals(filtro),
					false));
		}
		return opciones;
	}

	/** Si la barbería cierra ese día y quiénes no vienen. */
	private void agregarAusencias(Long negocioId, LocalDate dia, Long filtro, List<Usuario> atienden, Model model) {
		List<Ausencia> delDia = ausencias.queTocan(negocioId, dia, dia);
		model.addAttribute("cierre", delDia.stream().filter(a -> a.getUsuarioId() == null).findFirst().orElse(null));
		List<String> noVienen = new ArrayList<>();
		for (Usuario u : atienden) {
			if (filtro != null && !u.getId().equals(filtro)) {
				continue;
			}
			delDia.stream().filter(a -> Objects.equals(a.getUsuarioId(), u.getId())).findFirst()
					.ifPresent(a -> noVienen.add(a.getMotivo() == null ? u.getNombre()
							: u.getNombre() + " (" + a.getMotivo() + ")"));
		}
		model.addAttribute("noVienen", noVienen);
	}

	private static String enlace(String ruta, LocalDate dia, Long barbero) {
		UriComponentsBuilder uri = UriComponentsBuilder.fromPath(ruta).queryParam("fecha", dia);
		if (barbero != null) {
			uri.queryParam("barbero", barbero);
		}
		return uri.toUriString();
	}

}
