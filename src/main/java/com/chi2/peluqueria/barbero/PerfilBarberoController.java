package com.chi2.peluqueria.barbero;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.chi2.peluqueria.comun.AccionBloqueadaException;
import com.chi2.peluqueria.comun.Formatos;
import com.chi2.peluqueria.horario.AusenciaService;
import com.chi2.peluqueria.horario.HorarioService;
import com.chi2.peluqueria.seguridad.UsuarioAutenticado;
import com.chi2.peluqueria.usuario.EquipoService;
import com.chi2.peluqueria.usuario.Usuario;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Ficha de cada miembro del equipo, para el dueño (solo rol DUENO, ver SeguridadConfig). */
@Controller
@RequestMapping("/equipo/{id}")
class PerfilBarberoController {

	private final EquipoService equipo;
	private final PerfilBarberoService perfiles;
	private final HorarioService horarios;
	private final AusenciaService ausencias;

	PerfilBarberoController(EquipoService equipo, PerfilBarberoService perfiles, HorarioService horarios,
			AusenciaService ausencias) {
		this.equipo = equipo;
		this.perfiles = perfiles;
		this.horarios = horarios;
		this.ausencias = ausencias;
	}

	@GetMapping
	String ver(@AuthenticationPrincipal UsuarioAutenticado yo, @PathVariable Long id, Model model) {
		List<PerfilBarberoService.ServicioDelBarbero> servicios = perfiles.servicios(yo.getNegocioId(), id);
		model.addAttribute("form", ServiciosBarberoForm.de(servicios));
		return mostrar(yo, id, servicios, model);
	}

	@PostMapping("/atiende")
	String cambiarAtiende(@AuthenticationPrincipal UsuarioAutenticado yo, @PathVariable Long id,
			@RequestParam boolean valor, RedirectAttributes redirect) {
		try {
			Usuario usuario = perfiles.cambiarAtiende(yo.getNegocioId(), id, valor);
			redirect.addFlashAttribute("aviso", valor
					? usuario.getNombre() + " atiende clientes: va a aparecer en la agenda."
					: usuario.getNombre() + " ya no atiende clientes.");
		}
		catch (AccionBloqueadaException e) {
			redirect.addFlashAttribute("avisoError", e.getMessage());
		}
		return "redirect:/equipo/" + id;
	}

	@PostMapping("/servicios")
	String guardarServicios(@AuthenticationPrincipal UsuarioAutenticado yo, @PathVariable Long id,
			@ModelAttribute("form") ServiciosBarberoForm form, BindingResult errores, Model model,
			RedirectAttributes redirect) {
		Map<Long, Integer> elegidos = new HashMap<>();
		for (int i = 0; i < form.getItems().size(); i++) {
			ServiciosBarberoForm.Item item = form.getItems().get(i);
			if (!item.isHace() || item.getServicioId() == null) {
				continue;
			}
			Integer precio = null;
			if (StringUtils.hasText(item.getPrecioEspecial())) {
				Optional<Integer> leido = Formatos.leerPrecio(item.getPrecioEspecial());
				if (leido.isEmpty()) {
					errores.rejectValue("items[" + i + "].precioEspecial", "invalido",
							"Escribí el precio en pesos, sin centavos (ej. 15000).");
					continue;
				}
				precio = leido.get();
			}
			elegidos.put(item.getServicioId(), precio);
		}
		if (errores.hasErrors()) {
			return mostrar(yo, id, perfiles.servicios(yo.getNegocioId(), id), model);
		}
		perfiles.guardarServicios(yo.getNegocioId(), id, elegidos);
		redirect.addFlashAttribute("aviso", "Servicios guardados.");
		return "redirect:/equipo/" + id;
	}

	private String mostrar(UsuarioAutenticado yo, Long id, List<PerfilBarberoService.ServicioDelBarbero> servicios,
			Model model) {
		model.addAttribute("miembro", equipo.buscar(yo.getNegocioId(), id));
		model.addAttribute("servicios", servicios);
		model.addAttribute("horarioPropio", horarios.tieneHorarioPropio(yo.getNegocioId(), id));
		model.addAttribute("ausencias", ausencias.proximas(yo.getNegocioId(), id));
		return "perfil";
	}

}
