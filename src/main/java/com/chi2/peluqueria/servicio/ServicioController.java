package com.chi2.peluqueria.servicio;

import com.chi2.peluqueria.comun.DatoNoDisponibleException;
import com.chi2.peluqueria.seguridad.UsuarioAutenticado;

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

/** Catálogo de servicios del dueño (solo rol DUENO, ver SeguridadConfig). */
@Controller
@RequestMapping("/barberia/servicios")
class ServicioController {

	private final CatalogoService catalogo;

	ServicioController(CatalogoService catalogo) {
		this.catalogo = catalogo;
	}

	@GetMapping
	String lista(@AuthenticationPrincipal UsuarioAutenticado yo, Model model) {
		model.addAttribute("form", new ServicioForm());
		return mostrarLista(yo, model);
	}

	@PostMapping
	String crear(@AuthenticationPrincipal UsuarioAutenticado yo, @Valid @ModelAttribute("form") ServicioForm form,
			BindingResult errores, Model model, RedirectAttributes redirect) {
		validar(form, errores);
		if (!errores.hasErrors()) {
			try {
				Servicio nuevo = catalogo.crear(yo.getNegocioId(), form.getNombre(), form.getDuracionMinutos(),
						form.precioEnPesos().orElseThrow());
				redirect.addFlashAttribute("aviso", nuevo.getNombre() + " ya está en el catálogo.");
				return "redirect:/barberia/servicios";
			}
			catch (DatoNoDisponibleException e) {
				errores.rejectValue(e.getCampo(), "noDisponible", e.getMessage());
			}
		}
		return mostrarLista(yo, model);
	}

	@GetMapping("/{id}")
	String editar(@AuthenticationPrincipal UsuarioAutenticado yo, @PathVariable Long id, Model model) {
		Servicio servicio = catalogo.buscar(yo.getNegocioId(), id);
		model.addAttribute("form", ServicioForm.de(servicio));
		return mostrarEdicion(servicio, model);
	}

	@PostMapping("/{id}")
	String actualizar(@AuthenticationPrincipal UsuarioAutenticado yo, @PathVariable Long id,
			@Valid @ModelAttribute("form") ServicioForm form, BindingResult errores, Model model,
			RedirectAttributes redirect) {
		Servicio servicio = catalogo.buscar(yo.getNegocioId(), id);
		validar(form, errores);
		if (!errores.hasErrors()) {
			try {
				catalogo.actualizar(yo.getNegocioId(), id, form.getNombre(), form.getDuracionMinutos(),
						form.precioEnPesos().orElseThrow());
				redirect.addFlashAttribute("aviso", "Guardado.");
				return "redirect:/barberia/servicios";
			}
			catch (DatoNoDisponibleException e) {
				errores.rejectValue(e.getCampo(), "noDisponible", e.getMessage());
			}
		}
		return mostrarEdicion(servicio, model);
	}

	@PostMapping("/{id}/quitar")
	String quitar(@AuthenticationPrincipal UsuarioAutenticado yo, @PathVariable Long id, RedirectAttributes redirect) {
		Servicio servicio = catalogo.quitar(yo.getNegocioId(), id);
		redirect.addFlashAttribute("aviso", servicio.getNombre() + " ya no se ofrece. Lo podés volver a ofrecer desde abajo.");
		return "redirect:/barberia/servicios";
	}

	@PostMapping("/{id}/reactivar")
	String reactivar(@AuthenticationPrincipal UsuarioAutenticado yo, @PathVariable Long id, RedirectAttributes redirect) {
		try {
			Servicio servicio = catalogo.reactivar(yo.getNegocioId(), id);
			redirect.addFlashAttribute("aviso", servicio.getNombre() + " se ofrece de nuevo.");
		}
		catch (DatoNoDisponibleException e) {
			redirect.addFlashAttribute("avisoError", e.getMessage());
		}
		return "redirect:/barberia/servicios";
	}

	private void validar(ServicioForm form, BindingResult errores) {
		if (!errores.hasFieldErrors("duracionMinutos") && !Duraciones.esValida(form.getDuracionMinutos())) {
			errores.rejectValue("duracionMinutos", "invalida", "Elegí una duración de la lista.");
		}
		if (!errores.hasFieldErrors("precio") && form.precioEnPesos().isEmpty()) {
			errores.rejectValue("precio", "invalido", "Escribí el precio en pesos, sin centavos (ej. 12000).");
		}
	}

	private String mostrarLista(UsuarioAutenticado yo, Model model) {
		model.addAttribute("servicios", catalogo.activos(yo.getNegocioId()));
		model.addAttribute("quitados", catalogo.quitados(yo.getNegocioId()));
		model.addAttribute("duraciones", Duraciones.opciones());
		return "barberia/servicios";
	}

	private String mostrarEdicion(Servicio servicio, Model model) {
		model.addAttribute("servicio", servicio);
		model.addAttribute("duraciones", Duraciones.opciones());
		return "barberia/servicio";
	}

}
