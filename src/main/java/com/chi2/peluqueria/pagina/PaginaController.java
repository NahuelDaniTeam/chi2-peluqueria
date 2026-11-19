package com.chi2.peluqueria.pagina;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Pantallas web del sistema (renderizadas con Thymeleaf).
 * Los endpoints JSON van en el paquete {@code api}, bajo {@code /api}.
 */
@Controller
public class PaginaController {

	@GetMapping("/")
	public String inicio() {
		return "inicio";
	}

}
