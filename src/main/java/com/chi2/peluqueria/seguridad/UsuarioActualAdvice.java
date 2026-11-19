package com.chi2.peluqueria.seguridad;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Deja el usuario con sesión iniciada disponible en todas las vistas como {@code usuario}. */
@ControllerAdvice
class UsuarioActualAdvice {

	@ModelAttribute("usuario")
	UsuarioAutenticado usuario(@AuthenticationPrincipal UsuarioAutenticado usuario) {
		return usuario;
	}

}
