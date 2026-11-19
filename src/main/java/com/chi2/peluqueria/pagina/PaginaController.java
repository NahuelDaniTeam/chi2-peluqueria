package com.chi2.peluqueria.pagina;

import java.util.List;

import com.chi2.peluqueria.horario.HorarioService;
import com.chi2.peluqueria.seguridad.UsuarioAutenticado;
import com.chi2.peluqueria.servicio.ServicioRepository;
import com.chi2.peluqueria.usuario.UsuarioRepository;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Entrada al sistema: el login y la pantalla de inicio de cada rol.
 * Los endpoints JSON van en el paquete {@code api}, bajo {@code /api}.
 */
@Controller
public class PaginaController {

	/** Un paso de la puesta en marcha de la barbería, para el dueño. */
	public record Paso(String texto, String enlace, boolean hecho) {
	}

	private final ServicioRepository servicios;
	private final HorarioService horarios;
	private final UsuarioRepository usuarios;

	PaginaController(ServicioRepository servicios, HorarioService horarios, UsuarioRepository usuarios) {
		this.servicios = servicios;
		this.horarios = horarios;
		this.usuarios = usuarios;
	}

	@GetMapping("/login")
	public String login(@AuthenticationPrincipal UsuarioAutenticado usuario) {
		return usuario == null ? "login" : "redirect:/";
	}

	@GetMapping("/")
	public String inicio(@AuthenticationPrincipal UsuarioAutenticado usuario, Model model) {
		if (usuario.isAdmin()) {
			return "redirect:/admin/negocios";
		}
		if (usuario.isDueno()) {
			Long negocioId = usuario.getNegocioId();
			List<Paso> pasos = List.of(
					new Paso("Cargá tus servicios y precios", "/barberia/servicios",
							servicios.countByNegocioIdAndActivoTrue(negocioId) > 0),
					new Paso("Cargá el horario de atención", "/barberia/horario",
							horarios.localTieneHorario(negocioId)),
					new Paso("Sumá a tu equipo y marcá quién atiende", "/equipo",
							!usuarios.findAllByNegocioIdAndActivoTrueAndAtiendeTrueOrderByNombreAsc(negocioId).isEmpty()));
			model.addAttribute("pasos", pasos);
			model.addAttribute("pasosCompletos", pasos.stream().allMatch(Paso::hecho));
		}
		return "inicio";
	}

}
