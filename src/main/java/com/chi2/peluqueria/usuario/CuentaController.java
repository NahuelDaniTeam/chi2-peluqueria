package com.chi2.peluqueria.usuario;

import com.chi2.peluqueria.seguridad.UsuarioAutenticado;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Pantalla "Mi cuenta": datos propios, cambio de contraseña y salir. */
@Controller
@RequestMapping("/cuenta")
class CuentaController {

	private final CuentaService cuenta;

	CuentaController(CuentaService cuenta) {
		this.cuenta = cuenta;
	}

	@GetMapping
	String ver(Model model) {
		model.addAttribute("form", new CambioContrasenaForm());
		return "cuenta";
	}

	@PostMapping("/contrasena")
	String cambiarContrasena(@AuthenticationPrincipal UsuarioAutenticado yo,
			@Valid @ModelAttribute("form") CambioContrasenaForm form, BindingResult errores,
			RedirectAttributes redirect) {
		if (form.getNueva() != null && !form.getNueva().equals(form.getRepetida())) {
			errores.rejectValue("repetida", "noCoincide", "Las dos contraseñas nuevas no coinciden.");
		}
		if (!errores.hasErrors() && !cuenta.cambiarContrasena(yo.getId(), form.getActual(), form.getNueva())) {
			errores.rejectValue("actual", "incorrecta", "La contraseña actual no es correcta.");
		}
		if (errores.hasErrors()) {
			return "cuenta";
		}
		redirect.addFlashAttribute("aviso", "Listo, tu contraseña cambió.");
		return "redirect:/cuenta";
	}

}
