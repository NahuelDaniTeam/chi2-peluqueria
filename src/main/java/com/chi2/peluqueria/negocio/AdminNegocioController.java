package com.chi2.peluqueria.negocio;

import com.chi2.peluqueria.comun.DatoNoDisponibleException;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Pantalla de CHI2 para manejar las barberías (solo rol ADMIN, ver SeguridadConfig). */
@Controller
@RequestMapping("/admin/negocios")
class AdminNegocioController {

	private final AltaNegocioService alta;

	AdminNegocioController(AltaNegocioService alta) {
		this.alta = alta;
	}

	@GetMapping
	String ver(Model model) {
		model.addAttribute("form", new NuevoNegocioForm());
		return mostrar(model);
	}

	@PostMapping
	String crear(@Valid @ModelAttribute("form") NuevoNegocioForm form, BindingResult errores, Model model,
			RedirectAttributes redirect) {
		if (!errores.hasErrors()) {
			try {
				Negocio negocio = alta.crear(form);
				redirect.addFlashAttribute("aviso", negocio.getNombre() + " está lista. El dueño ya puede entrar.");
				return "redirect:/admin/negocios";
			}
			catch (DatoNoDisponibleException e) {
				errores.rejectValue(e.getCampo(), "noDisponible", e.getMessage());
			}
		}
		return mostrar(model);
	}

	@PostMapping("/{id}/suspender")
	String suspender(@PathVariable Long id, RedirectAttributes redirect) {
		Negocio negocio = alta.suspender(id);
		redirect.addFlashAttribute("aviso", negocio.getNombre() + " quedó suspendida: nadie de la barbería puede entrar.");
		return "redirect:/admin/negocios";
	}

	@PostMapping("/{id}/reactivar")
	String reactivar(@PathVariable Long id, RedirectAttributes redirect) {
		Negocio negocio = alta.reactivar(id);
		redirect.addFlashAttribute("aviso", negocio.getNombre() + " está activa de nuevo.");
		return "redirect:/admin/negocios";
	}

	private String mostrar(Model model) {
		model.addAttribute("negocios", alta.listar());
		return "admin/negocios";
	}

}
