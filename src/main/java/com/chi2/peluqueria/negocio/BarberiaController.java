package com.chi2.peluqueria.negocio;

import com.chi2.peluqueria.horario.AusenciaService;
import com.chi2.peluqueria.horario.HorarioService;
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
	private final HorarioService horarios;
	private final AusenciaService ausencias;

	BarberiaController(ServicioRepository servicios, HorarioService horarios, AusenciaService ausencias) {
		this.servicios = servicios;
		this.horarios = horarios;
		this.ausencias = ausencias;
	}

	@GetMapping("/barberia")
	String ver(@AuthenticationPrincipal UsuarioAutenticado yo, Model model) {
		model.addAttribute("cantidadServicios", servicios.countByNegocioIdAndActivoTrue(yo.getNegocioId()));
		model.addAttribute("tieneHorario", horarios.localTieneHorario(yo.getNegocioId()));
		model.addAttribute("cierres", ausencias.proximas(yo.getNegocioId(), null));
		return "barberia/inicio";
	}

}
