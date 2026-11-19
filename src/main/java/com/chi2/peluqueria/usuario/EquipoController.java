package com.chi2.peluqueria.usuario;

import com.chi2.peluqueria.comun.AccionBloqueadaException;
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

/** Pantalla "Equipo" del dueño (solo rol DUENO, ver SeguridadConfig). */
@Controller
@RequestMapping("/equipo")
class EquipoController {

	private final EquipoService equipo;

	EquipoController(EquipoService equipo) {
		this.equipo = equipo;
	}

	@GetMapping
	String ver(@AuthenticationPrincipal UsuarioAutenticado yo, Model model) {
		model.addAttribute("form", new NuevoMiembroForm());
		return mostrar(yo, model);
	}

	@PostMapping
	String agregar(@AuthenticationPrincipal UsuarioAutenticado yo, @Valid @ModelAttribute("form") NuevoMiembroForm form,
			BindingResult errores, Model model, RedirectAttributes redirect) {
		if (form.getRol() != null && !form.getRol().esDeNegocio()) {
			errores.rejectValue("rol", "noPermitido", "Elegí un rol válido.");
		}
		if (!errores.hasErrors()) {
			try {
				Usuario nuevo = equipo.agregar(yo.getNegocioId(), form);
				redirect.addFlashAttribute("aviso",
						nuevo.getNombre() + " ya puede entrar con " + nuevo.getEmail() + " y la contraseña que elegiste.");
				return "redirect:/equipo";
			}
			catch (DatoNoDisponibleException e) {
				errores.rejectValue(e.getCampo(), "noDisponible", e.getMessage());
			}
		}
		return mostrar(yo, model);
	}

	@PostMapping("/{id}/baja")
	String darDeBaja(@AuthenticationPrincipal UsuarioAutenticado yo, @PathVariable Long id, RedirectAttributes redirect) {
		try {
			Usuario usuario = equipo.darDeBaja(yo.getNegocioId(), id, yo.getId());
			redirect.addFlashAttribute("aviso", usuario.getNombre() + " ya no puede entrar.");
		}
		catch (AccionBloqueadaException e) {
			redirect.addFlashAttribute("avisoError", e.getMessage());
		}
		return "redirect:/equipo";
	}

	@PostMapping("/{id}/alta")
	String reactivar(@AuthenticationPrincipal UsuarioAutenticado yo, @PathVariable Long id, RedirectAttributes redirect) {
		Usuario usuario = equipo.reactivar(yo.getNegocioId(), id);
		redirect.addFlashAttribute("aviso", usuario.getNombre() + " puede volver a entrar.");
		return "redirect:/equipo";
	}

	private String mostrar(UsuarioAutenticado yo, Model model) {
		model.addAttribute("miembros", equipo.listar(yo.getNegocioId()));
		model.addAttribute("roles", Rol.deNegocio());
		return "equipo";
	}

}
