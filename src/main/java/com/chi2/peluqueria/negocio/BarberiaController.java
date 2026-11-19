package com.chi2.peluqueria.negocio;

import com.chi2.peluqueria.seguridad.UsuarioAutenticado;
import com.chi2.peluqueria.servicio.ServicioRepository;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Pestaña "Barbería" del dueño: desde acá se configura todo el local (solo rol DUENO). */
@Controller
class BarberiaController {

	private final ServicioRepository servicios;

	BarberiaController(ServicioRepository servicios) {
		this.servicios = servicios;
	}

	@GetMapping("/barberia")
	String ver(@AuthenticationPrincipal UsuarioAutenticado yo, Model model) {
		model.addAttribute("cantidadServicios", servicios.countByNegocioIdAndActivoTrue(yo.getNegocioId()));
		return "barberia/inicio";
	}

}
