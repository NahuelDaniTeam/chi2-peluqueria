package com.chi2.peluqueria.turno;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import com.chi2.peluqueria.comun.AccionBloqueadaException;
import com.chi2.peluqueria.comun.DatoNoDisponibleException;
import com.chi2.peluqueria.comun.Formatos;
import com.chi2.peluqueria.seguridad.UsuarioAutenticado;
import com.chi2.peluqueria.servicio.CatalogoService;
import com.chi2.peluqueria.servicio.Servicio;
import com.chi2.peluqueria.usuario.Usuario;
import com.chi2.peluqueria.usuario.UsuarioRepository;

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

/**
 * La atención "tipo Uber" desde la pantalla de cada turno: empezar, finalizar marcando cómo pagó, "no vino",
 * deshacer y corregir el cobro. Además, la atención sin turno. Ver {@link AtencionService}.
 * <p>
 * Lo común (finalizar con el servicio y el precio de siempre) se hace con un toque desde el turno; cambiar el
 * servicio o el monto lleva a la pantalla del cobro.
 */
@Controller
@RequestMapping("/agenda")
class AtencionController {

	private final AtencionService atencion;
	private final AgendaService agenda;
	private final CatalogoService catalogo;
	private final UsuarioRepository usuarios;

	AtencionController(AtencionService atencion, AgendaService agenda, CatalogoService catalogo,
			UsuarioRepository usuarios) {
		this.atencion = atencion;
		this.agenda = agenda;
		this.catalogo = catalogo;
		this.usuarios = usuarios;
	}

	// ---- Empezar, no vino, deshacer, anular ----

	@PostMapping("/turnos/{id}/empezar")
	String empezar(@AuthenticationPrincipal UsuarioAutenticado yo, @PathVariable Long id, RedirectAttributes redirect) {
		Turno turno = atencion.empezar(yo.getNegocioId(), soloDe(yo), id, yo.getId());
		redirect.addFlashAttribute("aviso", "Empezado el turno de " + turno.getClienteTexto()
				+ ". Al terminar, marcá cómo pagó.");
		return "redirect:/agenda/turnos/" + id;
	}

	@PostMapping("/turnos/{id}/ausente")
	String ausente(@AuthenticationPrincipal UsuarioAutenticado yo, @PathVariable Long id, RedirectAttributes redirect) {
		atencion.ausente(yo.getNegocioId(), soloDe(yo), id, yo.getId());
		redirect.addFlashAttribute("aviso", "Quedó como que no vino. El horario quedó libre.");
		return "redirect:/agenda/turnos/" + id;
	}

	@PostMapping("/turnos/{id}/reservado")
	String volverAReservado(@AuthenticationPrincipal UsuarioAutenticado yo, @PathVariable Long id,
			RedirectAttributes redirect) {
		try {
			atencion.volverAReservado(yo.getNegocioId(), soloDe(yo), id, yo.getId());
			redirect.addFlashAttribute("aviso", "Listo, el turno volvió a quedar reservado.");
		}
		catch (AccionBloqueadaException e) {
			redirect.addFlashAttribute("avisoError", e.getMessage());
		}
		return "redirect:/agenda/turnos/" + id;
	}

	@PostMapping("/turnos/{id}/anular")
	String anular(@AuthenticationPrincipal UsuarioAutenticado yo, @PathVariable Long id, RedirectAttributes redirect) {
		atencion.anular(yo.getNegocioId(), soloDe(yo), id, yo.getId());
		redirect.addFlashAttribute("aviso", "Atención anulada.");
		return "redirect:/agenda";
	}

	// ---- Finalizar y corregir el cobro ----

	@PostMapping("/turnos/{id}/finalizar")
	String finalizar(@AuthenticationPrincipal UsuarioAutenticado yo, @PathVariable Long id,
			@ModelAttribute("form") CobroForm form, BindingResult errores, Model model, RedirectAttributes redirect) {
		return form.leer(errores).map(cobro -> {
			Turno turno = atencion.finalizar(yo.getNegocioId(), soloDe(yo), id, yo.getId(), cobro);
			redirect.addFlashAttribute("aviso", "Listo: " + turno.getClienteTexto() + " · "
					+ turno.getServicio().getNombre() + " · " + turno.getCobroTexto() + ".");
			return "redirect:/agenda";
		}).orElseGet(() -> mostrarCobro(yo, id, form, model));
	}

	@GetMapping("/turnos/{id}/cobro")
	String cobro(@AuthenticationPrincipal UsuarioAutenticado yo, @PathVariable Long id,
			@ModelAttribute("form") CobroForm form, BindingResult parametrosRaros, Model model) {
		return mostrarCobro(yo, id, form, model);
	}

	@PostMapping("/turnos/{id}/cobro")
	String corregirCobro(@AuthenticationPrincipal UsuarioAutenticado yo, @PathVariable Long id,
			@ModelAttribute("form") CobroForm form, BindingResult errores, Model model, RedirectAttributes redirect) {
		return form.leer(errores).map(cobro -> {
			atencion.corregirCobro(yo.getNegocioId(), soloDe(yo), id, yo.getId(), cobro);
			redirect.addFlashAttribute("aviso", "Cobro corregido.");
			return "redirect:/agenda/turnos/" + id;
		}).orElseGet(() -> mostrarCobro(yo, id, form, model));
	}

	/**
	 * Elegir otro servicio recarga la pantalla con su precio (para este barbero). Al corregir, arranca con lo
	 * que se había marcado.
	 */
	private String mostrarCobro(UsuarioAutenticado yo, Long id, CobroForm form, Model model) {
		Long negocioId = yo.getNegocioId();
		AgendaService.TurnoVista vista = agenda.ver(negocioId, soloDe(yo), id);
		Turno turno = vista.turno();
		AtencionService.Acciones acciones = atencion.acciones(turno, agenda.zona(negocioId));
		if (!acciones.cobrar() && !acciones.corregir()) {
			return "redirect:/agenda/turnos/" + id;
		}

		// El servicio que ya tenía el turno siempre se puede elegir, aunque se haya quitado del catálogo
		List<Servicio> servicios = new ArrayList<>(catalogo.activos(negocioId));
		if (servicios.stream().noneMatch(s -> s.getId().equals(turno.getServicio().getId()))) {
			servicios.add(0, turno.getServicio());
		}
		Servicio elegido = servicios.stream().filter(s -> s.getId().equals(form.getServicio())).findFirst()
				.orElse(turno.getServicio());
		form.setServicio(elegido.getId());
		boolean mismoServicio = elegido.getId().equals(turno.getServicio().getId());
		if (form.getMonto() == null) {
			int pesos = turno.isFinalizado() && mismoServicio ? turno.getMonto()
					: atencion.precio(negocioId, turno.getBarbero().getId(), elegido);
			form.setMonto(Formatos.miles(pesos));
		}
		if (form.getMedioPago() == null && turno.isFinalizado()) {
			form.setMedioPago(turno.getMedioPago());
		}

		String base = "/agenda/turnos/" + id + "/cobro";
		model.addAttribute("t", vista);
		model.addAttribute("corregir", acciones.corregir());
		model.addAttribute("servicios", servicios.stream()
				.map(s -> new Opcion(s.getNombre(),
						Formatos.precio(atencion.precio(negocioId, turno.getBarbero().getId(), s)),
						TurnoController.enlace(base, "servicio", s.getId()), s.getId().equals(elegido.getId()), false))
				.toList());
		model.addAttribute("medios", MedioPago.values());
		model.addAttribute("volverA", "/agenda/turnos/" + id);
		return "agenda/cobro";
	}

	// ---- Sin turno ----

	@GetMapping("/sin-turno")
	String sinTurno(@AuthenticationPrincipal UsuarioAutenticado yo, @ModelAttribute("form") SinTurnoForm form,
			BindingResult parametrosRaros, Model model) {
		return mostrarSinTurno(yo, form, model);
	}

	@PostMapping("/sin-turno")
	String cargarSinTurno(@AuthenticationPrincipal UsuarioAutenticado yo, @Valid @ModelAttribute("form") SinTurnoForm form,
			BindingResult errores, Model model, RedirectAttributes redirect) {
		// Con medio de pago, ya lo atendió: hace falta también el monto
		AtencionService.Cobro cobro = form.getMedioPago() == null ? null : form.leer(errores).orElse(null);
		if (!errores.hasErrors()) {
			try {
				Turno turno = atencion.sinTurno(yo.getNegocioId(), soloDe(yo), yo.getId(), form.getBarbero(),
						form.getServicio(), form.getNombre(), cobro);
				if (turno.isFinalizado()) {
					redirect.addFlashAttribute("aviso", "Listo: " + turno.getServicio().getNombre() + " con "
							+ turno.getBarbero().getNombre() + " · " + turno.getCobroTexto() + ".");
					return "redirect:/agenda";
				}
				redirect.addFlashAttribute("aviso", "Empezado: " + turno.getServicio().getNombre() + " con "
						+ turno.getBarbero().getNombre() + ". Al terminar, marcá cómo pagó.");
				return "redirect:/agenda/turnos/" + turno.getId();
			}
			catch (DatoNoDisponibleException e) {
				errores.rejectValue(e.getCampo(), "noDisponible", e.getMessage());
			}
		}
		return mostrarSinTurno(yo, form, model);
	}

	private String mostrarSinTurno(UsuarioAutenticado yo, SinTurnoForm form, Model model) {
		Long negocioId = yo.getNegocioId();
		List<Usuario> atienden = usuarios.findAllByNegocioIdAndActivoTrueAndAtiendeTrueOrderByNombreAsc(negocioId);
		boolean atiendo = atienden.stream().anyMatch(u -> u.getId().equals(yo.getId()));

		// Quién: un barbero, él mismo; el dueño elige (de entrada él, si atiende, o el único que hay)
		if (!yo.isDueno()) {
			form.setBarbero(yo.getId());
		}
		else if (atienden.stream().noneMatch(u -> u.getId().equals(form.getBarbero()))) {
			form.setBarbero(atiendo ? yo.getId() : atienden.size() == 1 ? atienden.get(0).getId() : null);
		}
		Long barbero = form.getBarbero();

		List<Servicio> servicios = catalogo.activos(negocioId);
		Servicio elegido = servicios.stream().filter(s -> s.getId().equals(form.getServicio())).findFirst()
				.orElse(servicios.size() == 1 ? servicios.get(0) : null);
		form.setServicio(elegido == null ? null : elegido.getId());
		if (elegido != null && form.getMonto() == null) {
			form.setMonto(Formatos.miles(barbero == null ? elegido.getPrecio()
					: atencion.precio(negocioId, barbero, elegido)));
		}

		model.addAttribute("puedeAtender", yo.isDueno() ? !atienden.isEmpty() : atiendo);
		if (yo.isDueno() && atienden.size() > 1) {
			model.addAttribute("barberos", atienden.stream()
					.map(u -> new Opcion(u.getNombre(), null, enlaceSinTurno(u.getId(), form.getServicio()),
							u.getId().equals(barbero), false))
					.toList());
		}
		model.addAttribute("servicios", servicios.stream()
				.map(s -> new Opcion(s.getNombre(), s.getDuracionTexto() + " · " + Formatos.precio(
						barbero == null ? s.getPrecio() : atencion.precio(negocioId, barbero, s)),
						enlaceSinTurno(barbero, s.getId()), elegido != null && s.getId().equals(elegido.getId()), false))
				.toList());
		model.addAttribute("servicioElegido", elegido);
		model.addAttribute("cambiarServicio", enlaceSinTurno(barbero, null));
		model.addAttribute("ahora", Formatos.hora(LocalTime.now(agenda.zona(negocioId))));
		model.addAttribute("medios", MedioPago.values());
		return "agenda/sin-turno";
	}

	private static String enlaceSinTurno(Long barbero, Long servicio) {
		return TurnoController.enlace("/agenda/sin-turno", "barbero", barbero, "servicio", servicio);
	}

	/** {@code null} para el dueño (todo el negocio); el propio id para un barbero. */
	private static Long soloDe(UsuarioAutenticado yo) {
		return yo.isDueno() ? null : yo.getId();
	}

}
