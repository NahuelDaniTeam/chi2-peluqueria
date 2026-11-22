package com.chi2.peluqueria.metrica;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.chi2.peluqueria.comun.Formatos;
import com.chi2.peluqueria.seguridad.UsuarioAutenticado;
import com.chi2.peluqueria.turno.AgendaService;
import com.chi2.peluqueria.turno.Opcion;
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
 * Los números: de entrada, los del mes. Un barbero ve solo los suyos; el dueño, los de toda la barbería
 * comparando barberos, o los de uno.
 */
@Controller
@RequestMapping("/numeros")
class MetricasController {

	/** Una columna del gráfico de cobrado por día. */
	record Columna(String etiqueta, boolean conEtiqueta, int altura, String detalle, boolean hoy) {
	}

	private static final String[] LETRAS = { "L", "M", "M", "J", "V", "S", "D" };

	/** En el mes se rotulan solo algunos días, para que entren. */
	private static final Set<Integer> DIAS_ROTULADOS = Set.of(1, 8, 15, 22, 29);

	private final MetricasService metricas;
	private final AgendaService agenda;
	private final UsuarioRepository usuarios;

	MetricasController(MetricasService metricas, AgendaService agenda, UsuarioRepository usuarios) {
		this.metricas = metricas;
		this.agenda = agenda;
		this.usuarios = usuarios;
	}

	@GetMapping
	String ver(@AuthenticationPrincipal UsuarioAutenticado yo, @RequestParam(required = false) String periodo,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
			@RequestParam(required = false) Long barbero, Model model) {
		Long negocioId = yo.getNegocioId();
		LocalDate hoy = agenda.hoy(negocioId);
		Periodo elegido = Periodo.de(Periodo.Tipo.desde(periodo), fecha != null ? fecha : hoy);

		// El barbero ve lo suyo; el dueño, todo o el de alguien de su barbería
		Usuario de = !yo.isDueno() || barbero == null ? null
				: usuarios.findByIdAndNegocioId(barbero, negocioId).orElse(null);
		Long filtro = yo.isDueno() ? (de == null ? null : de.getId()) : yo.getId();
		Long enLinks = yo.isDueno() ? filtro : null;

		MetricasService.Resumen resumen = metricas.resumen(negocioId, filtro, elegido);
		model.addAttribute("r", resumen);
		model.addAttribute("periodo", elegido);
		model.addAttribute("etiqueta", elegido.etiqueta(hoy));
		model.addAttribute("de", de);
		model.addAttribute("verTodos", yo.isDueno() && filtro == null);
		model.addAttribute("anterior", enlace(elegido.tipo(), elegido.anterior().desde(), enLinks));
		model.addAttribute("siguiente", elegido.siguiente().desde().isAfter(hoy) ? null
				: enlace(elegido.tipo(), elegido.siguiente().desde(), enLinks));
		model.addAttribute("enlaceHoy", elegido.contiene(hoy) ? null : enlace(elegido.tipo(), hoy, enLinks));
		model.addAttribute("tipos", List.of(Periodo.Tipo.values()).stream()
				.map(t -> new Opcion(t.getEtiqueta(), null, enlace(t, elegido.contiene(hoy) ? hoy : elegido.desde(), enLinks),
						t == elegido.tipo(), false))
				.toList());
		if (yo.isDueno()) {
			List<Usuario> atienden = usuarios.findAllByNegocioIdAndActivoTrueAndAtiendeTrueOrderByNombreAsc(negocioId);
			if (atienden.size() > 1 || de != null) {
				List<Opcion> barberos = new ArrayList<>();
				barberos.add(new Opcion("Todos", null, enlace(elegido.tipo(), elegido.desde(), null), filtro == null, false));
				for (Usuario u : atienden) {
					barberos.add(new Opcion(u.getNombre(), null, enlace(elegido.tipo(), elegido.desde(), u.getId()),
							u.getId().equals(filtro), false));
				}
				model.addAttribute("barberos", barberos);
			}
			model.addAttribute("enlaceBarbero", enlace(elegido.tipo(), elegido.desde(), null));
		}
		if (elegido.tipo() != Periodo.Tipo.DIA) {
			model.addAttribute("columnas", columnas(resumen, elegido, hoy));
			resumen.dias().stream().filter(d -> d.cobrado() > 0)
					.max((a, b) -> Long.compare(a.cobrado(), b.cobrado()))
					.ifPresent(d -> model.addAttribute("mejorDia",
							Formatos.fechaCorta(d.fecha()) + " · " + d.getCobradoTexto()));
		}
		return "numeros/numeros";
	}

	private static List<Columna> columnas(MetricasService.Resumen resumen, Periodo periodo, LocalDate hoy) {
		long maximo = resumen.getMaximoPorDia();
		List<Columna> columnas = new ArrayList<>();
		for (MetricasService.PorDia d : resumen.dias()) {
			boolean semana = periodo.tipo() == Periodo.Tipo.SEMANA;
			String etiqueta = semana ? LETRAS[d.fecha().getDayOfWeek().getValue() - 1]
					: String.valueOf(d.fecha().getDayOfMonth());
			int altura = maximo == 0 ? 0 : (int) Math.round(d.cobrado() * 100.0 / maximo);
			String detalle = Formatos.fechaCorta(d.fecha()) + ": " + d.getCobradoTexto() + " · " + d.atenciones()
					+ (d.atenciones() == 1 ? " atención" : " atenciones");
			columnas.add(new Columna(etiqueta, semana || DIAS_ROTULADOS.contains(d.fecha().getDayOfMonth()), altura,
					detalle, d.fecha().equals(hoy)));
		}
		return columnas;
	}

	private static String enlace(Periodo.Tipo tipo, LocalDate fecha, Long barbero) {
		UriComponentsBuilder uri = UriComponentsBuilder.fromPath("/numeros").queryParam("periodo", tipo.getClave())
				.queryParam("fecha", fecha);
		if (barbero != null) {
			uri.queryParam("barbero", barbero);
		}
		return uri.toUriString();
	}

}
