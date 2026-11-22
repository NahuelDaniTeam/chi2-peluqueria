package com.chi2.peluqueria.turno;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import com.chi2.peluqueria.comun.DatoNoDisponibleException;
import com.chi2.peluqueria.comun.Formatos;
import com.chi2.peluqueria.seguridad.UsuarioAutenticado;
import com.chi2.peluqueria.servicio.CatalogoService;
import com.chi2.peluqueria.servicio.Servicio;
import com.chi2.peluqueria.usuario.Usuario;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Un turno: cargarlo, verlo, cancelarlo y reprogramarlo. El dueño toca los de todos; un barbero, solo
 * los suyos (ver {@link AgendaService}).
 * <p>
 * Elegir servicio, barbero y día son links que recargan la pantalla con lo elegido en la URL; el horario y
 * los datos del cliente van en el formulario. Así funciona sin JavaScript y con el botón "atrás".
 */
@Controller
@RequestMapping("/agenda")
class TurnoController {

	/** Cuántos días se muestran para elegir (para más adelante está "otra fecha"). */
	static final int DIAS_A_LA_VISTA = 14;

	/** Las horas libres de un día, separadas en mañana y tarde para que se lean rápido. */
	record GrupoDeHoras(String nombre, List<String> horas) {
	}

	private final AgendaService agenda;
	private final AtencionService atencion;
	private final Disponibilidad disponibilidad;
	private final CatalogoService catalogo;

	TurnoController(AgendaService agenda, AtencionService atencion, Disponibilidad disponibilidad,
			CatalogoService catalogo) {
		this.agenda = agenda;
		this.atencion = atencion;
		this.disponibilidad = disponibilidad;
		this.catalogo = catalogo;
	}

	// ---- Nuevo turno ----

	@GetMapping("/nuevo")
	String nuevo(@AuthenticationPrincipal UsuarioAutenticado yo, @ModelAttribute("form") ReservaForm form,
			BindingResult parametrosRaros, Model model) {
		// Un parámetro inválido en la URL queda vacío y se elige de nuevo
		return mostrarNuevo(yo, form, model);
	}

	@PostMapping("/nuevo")
	String reservar(@AuthenticationPrincipal UsuarioAutenticado yo, @Valid @ModelAttribute("form") ReservaForm form,
			BindingResult errores, Model model, RedirectAttributes redirect) {
		if (form.getHora() == null && !errores.hasFieldErrors("hora")) {
			errores.rejectValue("hora", "falta", "Elegí un horario.");
		}
		if (!errores.hasErrors()) {
			try {
				Turno turno = agenda.reservar(yo.getNegocioId(), soloDe(yo), yo.getId(), form);
				AgendaService.TurnoVista vista = agenda.ver(yo.getNegocioId(), soloDe(yo), turno.getId());
				redirect.addFlashAttribute("aviso", "Turno reservado: " + turno.getClienteNombre() + ", "
						+ vista.getFechaTexto() + " a las " + vista.getDesdeTexto() + " con "
						+ turno.getBarbero().getNombre() + ".");
				return "redirect:" + enlace("/agenda", "fecha", vista.fecha());
			}
			catch (DatoNoDisponibleException e) {
				errores.rejectValue(e.getCampo(), "noDisponible", e.getMessage());
				form.setHora(null);
			}
		}
		return mostrarNuevo(yo, form, model);
	}

	private String mostrarNuevo(UsuarioAutenticado yo, ReservaForm form, Model model) {
		Long negocioId = yo.getNegocioId();
		Long soloDe = soloDe(yo);
		LocalDate hoy = agenda.hoy(negocioId);

		// Solo los servicios que alguien hace (o, para un barbero, los que hace él)
		Set<Long> conQuien = disponibilidad.serviciosConQuienLosHaga(negocioId, soloDe);
		List<Servicio> servicios = catalogo.activos(negocioId).stream().filter(s -> conQuien.contains(s.getId())).toList();
		Servicio servicio = servicios.stream().filter(s -> s.getId().equals(form.getServicio())).findFirst()
				.orElse(servicios.size() == 1 ? servicios.get(0) : null);
		form.setServicio(servicio == null ? null : servicio.getId());
		model.addAttribute("hayServicios", !servicios.isEmpty());
		model.addAttribute("servicioElegido", servicio);
		model.addAttribute("cambiarServicio", enlace("/agenda/nuevo", "barbero", form.getBarbero(), "fecha", form.getFecha()));
		model.addAttribute("volverA", enlace("/agenda", "fecha", form.getFecha() != null ? form.getFecha() : hoy));

		if (servicio == null) {
			model.addAttribute("servicios", opcionesDeServicios(servicios, form));
			return "agenda/nuevo";
		}

		List<Usuario> barberos = disponibilidad.barberos(negocioId, servicio.getId());
		if (soloDe != null) {
			form.setBarbero(soloDe);
		}
		else if (barberos.stream().noneMatch(b -> b.getId().equals(form.getBarbero()))) {
			form.setBarbero(null);
		}
		Disponibilidad.Pedido pedido = new Disponibilidad.Pedido(negocioId, servicio.getId(),
				servicio.getDuracionMinutos(), form.getBarbero(), null);
		Map<LocalDate, Disponibilidad.DiaLibre> dias = elegirDia(pedido, hoy, form.getFecha(), form::setFecha);

		model.addAttribute("servicios", opcionesDeServicios(servicios, form));
		if (soloDe == null) {
			List<Opcion> opciones = new ArrayList<>();
			opciones.add(new Opcion("Cualquiera", null, enlaceNuevo(servicio.getId(), null,
					form.getFecha()), form.getBarbero() == null, false));
			for (Usuario b : barberos) {
				opciones.add(new Opcion(b.getNombre(), null, enlaceNuevo(servicio.getId(), b.getId(), form.getFecha()),
						b.getId().equals(form.getBarbero()), false));
			}
			model.addAttribute("barberos", opciones);
		}
		agregarDiasYHoras(dias, hoy, form.getFecha(), form.getHora(),
				d -> enlaceNuevo(servicio.getId(), form.getBarbero(), d), model);
		return "agenda/nuevo";
	}

	private List<Opcion> opcionesDeServicios(List<Servicio> servicios, ReservaForm form) {
		return servicios.stream()
				.map(s -> new Opcion(s.getNombre(), s.getDuracionTexto() + " · " + s.getPrecioTexto(),
						enlaceNuevo(s.getId(), form.getBarbero(), form.getFecha()),
						s.getId().equals(form.getServicio()), false))
				.toList();
	}

	private static String enlaceNuevo(Long servicio, Long barbero, LocalDate fecha) {
		return enlace("/agenda/nuevo", "servicio", servicio, "barbero", barbero, "fecha", fecha);
	}

	// ---- Ver, cancelar ----

	/** La pantalla de un turno: sus datos, lo que se puede hacer ahora (ver {@link AtencionService.Acciones}) y su historial. */
	@GetMapping("/turnos/{id}")
	String ver(@AuthenticationPrincipal UsuarioAutenticado yo, @PathVariable Long id, Model model) {
		Long negocioId = yo.getNegocioId();
		AgendaService.TurnoVista turno = agenda.ver(negocioId, soloDe(yo), id);
		AtencionService.Acciones acciones = atencion.acciones(turno.turno(), agenda.zona(negocioId));
		model.addAttribute("t", turno);
		model.addAttribute("acciones", acciones);
		if (acciones.cobrar()) {
			// Finalizar en un toque: el servicio reservado, al precio de este barbero
			int precio = atencion.precio(negocioId, turno.turno().getBarbero().getId(), turno.turno().getServicio());
			model.addAttribute("precio", precio);
			model.addAttribute("precioTexto", Formatos.precio(precio));
		}
		model.addAttribute("historial", agenda.historial(negocioId, soloDe(yo), id));
		model.addAttribute("cancelacionTardia", agenda.cancelarSeriaTardio(turno.turno()));
		model.addAttribute("volverA", enlace("/agenda", "fecha", turno.fecha()));
		return "agenda/turno";
	}

	@PostMapping("/turnos/{id}/cancelar")
	String cancelar(@AuthenticationPrincipal UsuarioAutenticado yo, @PathVariable Long id, RedirectAttributes redirect) {
		Turno turno = agenda.cancelar(yo.getNegocioId(), soloDe(yo), id, yo.getId());
		redirect.addFlashAttribute("aviso", turno.isCancelacionTardia()
				? "Turno cancelado. Quedó como cancelación tardía: faltaban menos de 24 horas."
				: "Turno cancelado. El horario quedó libre.");
		return "redirect:/agenda/turnos/" + id;
	}

	// ---- Reprogramar ----

	@GetMapping("/turnos/{id}/reprogramar")
	String reprogramar(@AuthenticationPrincipal UsuarioAutenticado yo, @PathVariable Long id,
			@ModelAttribute("form") ReprogramacionForm form, BindingResult parametrosRaros, Model model) {
		return mostrarReprogramar(yo, id, form, model);
	}

	@PostMapping("/turnos/{id}/reprogramar")
	String guardarReprogramacion(@AuthenticationPrincipal UsuarioAutenticado yo, @PathVariable Long id,
			@ModelAttribute("form") ReprogramacionForm form, BindingResult errores, Model model,
			RedirectAttributes redirect) {
		if (form.getHora() == null && !errores.hasFieldErrors("hora")) {
			errores.rejectValue("hora", "falta", "Elegí un horario.");
		}
		if (!errores.hasErrors()) {
			try {
				Turno turno = agenda.reprogramar(yo.getNegocioId(), soloDe(yo), id, yo.getId(), form);
				AgendaService.TurnoVista vista = agenda.ver(yo.getNegocioId(), soloDe(yo), turno.getId());
				redirect.addFlashAttribute("aviso", "Turno reprogramado: " + vista.getFechaTexto() + " a las "
						+ vista.getDesdeTexto() + " con " + turno.getBarbero().getNombre() + ".");
				return "redirect:/agenda/turnos/" + id;
			}
			catch (DatoNoDisponibleException e) {
				errores.rejectValue(e.getCampo(), "noDisponible", e.getMessage());
				form.setHora(null);
			}
		}
		return mostrarReprogramar(yo, id, form, model);
	}

	private String mostrarReprogramar(UsuarioAutenticado yo, Long id, ReprogramacionForm form, Model model) {
		Long negocioId = yo.getNegocioId();
		AgendaService.TurnoVista turno = agenda.ver(negocioId, soloDe(yo), id);
		if (!turno.turno().isReservado()) {
			return "redirect:/agenda/turnos/" + id;
		}
		LocalDate hoy = agenda.hoy(negocioId);
		Long actual = turno.turno().getBarbero().getId();
		String base = "/agenda/turnos/" + id + "/reprogramar";

		// Un barbero reprograma dentro de su agenda; el dueño puede pasarlo a otro
		List<Usuario> barberos = disponibilidad.barberos(negocioId, turno.turno().getServicio().getId());
		if (soloDe(yo) != null || barberos.stream().noneMatch(b -> b.getId().equals(form.getBarbero()))) {
			form.setBarbero(actual);
		}
		Disponibilidad.Pedido pedido = new Disponibilidad.Pedido(negocioId, turno.turno().getServicio().getId(),
				(int) turno.turno().getDuracion().toMinutes(), form.getBarbero(), turno.turno().getId());
		LocalDate pedida = form.getFecha() != null ? form.getFecha() : turno.fecha();
		Map<LocalDate, Disponibilidad.DiaLibre> dias = elegirDia(pedido, hoy, pedida, form::setFecha);

		model.addAttribute("t", turno);
		model.addAttribute("volverA", "/agenda/turnos/" + id);
		if (yo.isDueno()) {
			model.addAttribute("barberos", barberos.stream()
					.map(b -> new Opcion(b.getNombre(), null,
							enlace(base, "barbero", b.getId(), "fecha", form.getFecha()),
							b.getId().equals(form.getBarbero()), false))
					.toList());
		}
		agregarDiasYHoras(dias, hoy, form.getFecha(), form.getHora(),
				d -> enlace(base, "barbero", form.getBarbero(), "fecha", d), model);
		return "agenda/reprogramar";
	}

	// ---- Comunes: días y horas ----

	/**
	 * Calcula los días a la vista (y el pedido, si está más adelante) y deja elegido uno: el pedido si se
	 * puede, si no el primero con lugar, si no hoy.
	 */
	private Map<LocalDate, Disponibilidad.DiaLibre> elegirDia(Disponibilidad.Pedido pedido, LocalDate hoy,
			LocalDate pedida, Consumer<LocalDate> elegir) {
		LocalDate ultimo = hoy.plusDays(AgendaService.DIAS_DE_ANTICIPACION);
		boolean valida = pedida != null && !pedida.isBefore(hoy) && !pedida.isAfter(ultimo);
		LocalDate hasta = hoy.plusDays(DIAS_A_LA_VISTA - 1);
		Map<LocalDate, Disponibilidad.DiaLibre> dias = new LinkedHashMap<>(
				disponibilidad.libres(pedido, hoy, hasta));
		if (valida && pedida.isAfter(hasta)) {
			dias.put(pedida, disponibilidad.libres(pedido, pedida));
		}
		elegir.accept(valida ? pedida
				: dias.values().stream().filter(d -> !d.horas().isEmpty()).map(Disponibilidad.DiaLibre::fecha)
						.findFirst().orElse(hoy));
		return dias;
	}

	private void agregarDiasYHoras(Map<LocalDate, Disponibilidad.DiaLibre> dias, LocalDate hoy, LocalDate elegida,
			LocalTime horaElegida, Function<LocalDate, String> enlaceDelDia, Model model) {
		List<Opcion> opciones = new ArrayList<>();
		for (Disponibilidad.DiaLibre dia : dias.values()) {
			String texto = dia.fecha().equals(hoy) ? "Hoy"
					: dia.fecha().equals(hoy.plusDays(1)) ? "Mañana"
							: Formatos.fechaCorta(dia.fecha()).split(" ")[0];
			String detalle = !dia.abierto() ? "Cerrado"
					: dia.horas().isEmpty() ? "Sin lugar" : dia.fecha().getDayOfMonth() + "/" + dia.fecha().getMonthValue();
			opciones.add(new Opcion(texto, detalle, enlaceDelDia.apply(dia.fecha()), dia.fecha().equals(elegida),
					dia.horas().isEmpty()));
		}
		Disponibilidad.DiaLibre delDia = dias.get(elegida);
		List<String> manana = new ArrayList<>();
		List<String> tarde = new ArrayList<>();
		for (LocalTime hora : delDia.horas()) {
			(hora.isBefore(LocalTime.of(13, 0)) ? manana : tarde).add(Formatos.hora(hora));
		}
		List<GrupoDeHoras> grupos = new ArrayList<>();
		if (!manana.isEmpty()) {
			grupos.add(new GrupoDeHoras("Mañana", manana));
		}
		if (!tarde.isEmpty()) {
			grupos.add(new GrupoDeHoras("Tarde", tarde));
		}
		model.addAttribute("dias", opciones);
		model.addAttribute("diaElegido", delDia);
		model.addAttribute("fechaTexto", Formatos.fechaLarga(elegida));
		model.addAttribute("grupos", grupos);
		model.addAttribute("horaElegida", horaElegida == null ? null : Formatos.hora(horaElegida));
		model.addAttribute("hoy", hoy);
		model.addAttribute("ultimoDia", hoy.plusDays(AgendaService.DIAS_DE_ANTICIPACION));
	}

	/** {@code null} para el dueño (todo el negocio); el propio id para un barbero. */
	private static Long soloDe(UsuarioAutenticado yo) {
		return yo.isDueno() ? null : yo.getId();
	}

	/** Arma un link con los parámetros que tienen valor: enlace("/x", "a", 1, "b", null) → "/x?a=1". */
	static String enlace(String ruta, Object... parametros) {
		UriComponentsBuilder uri = UriComponentsBuilder.fromPath(ruta);
		for (int i = 0; i < parametros.length; i += 2) {
			if (parametros[i + 1] != null) {
				uri.queryParam((String) parametros[i], parametros[i + 1]);
			}
		}
		return uri.toUriString();
	}

}
