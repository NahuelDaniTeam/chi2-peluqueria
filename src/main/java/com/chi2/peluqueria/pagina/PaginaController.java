package com.chi2.peluqueria.pagina;

import com.chi2.peluqueria.seguridad.UsuarioAutenticado;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Entrada al sistema: el login y a dónde va cada rol al entrar (CHI2 a las barberías; el resto, a la agenda).
 * Los endpoints JSON van en el paquete {@code api}, bajo {@code /api}.
 */
@Controller
public class PaginaController {

	@GetMapping("/login")
	public String login(@AuthenticationPrincipal UsuarioAutenticado usuario) {
		return usuario == null ? "login" : "redirect:/";
	}

	@GetMapping("/")
	public String inicio(@AuthenticationPrincipal UsuarioAutenticado usuario) {
		return usuario.isAdmin() ? "redirect:/admin/negocios" : "redirect:/agenda";
	}

}
