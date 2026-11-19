package com.chi2.peluqueria.horario;

import com.chi2.peluqueria.seguridad.UsuarioAutenticado;
import com.chi2.peluqueria.usuario.EquipoService;
import com.chi2.peluqueria.usuario.Usuario;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Horario del local (/barberia/horario) y de cada barbero (/equipo/{id}/horario). Solo rol DUENO. */
@Controller
class HorarioController {

	private final HorarioService horarios;
	private final EquipoService equipo;

	HorarioController(HorarioService horarios, EquipoService equipo) {
		this.horarios = horarios;
		this.equipo = equipo;
	}

	// ---- Local ----

	@GetMapping("/barberia/horario")
	String local(@AuthenticationPrincipal UsuarioAutenticado yo, Model model) {
		model.addAttribute("form", horarios.formLocal(yo.getNegocioId()));
		return mostrarLocal(model);
	}

	@PostMapping("/barberia/horario")
	String guardarLocal(@AuthenticationPrincipal UsuarioAutenticado yo, @ModelAttribute("form") HorarioSemanalForm form,
			BindingResult errores, Model model, RedirectAttributes redirect) {
		horarios.validar(form, errores);
		if (errores.hasErrors()) {
			return mostrarLocal(model);
		}
		horarios.guardarLocal(yo.getNegocioId(), form);
		redirect.addFlashAttribute("aviso", "Horario de atención guardado.");
		return "redirect:/barberia";
	}

	// ---- Barbero ----

	@GetMapping("/equipo/{id}/horario")
	String barbero(@AuthenticationPrincipal UsuarioAutenticado yo, @PathVariable Long id, Model model) {
		model.addAttribute("form", horarios.formBarbero(yo.getNegocioId(), id));
		return mostrarBarbero(yo, id, model);
	}

	@PostMapping("/equipo/{id}/horario")
	String guardarBarbero(@AuthenticationPrincipal UsuarioAutenticado yo, @PathVariable Long id,
			@ModelAttribute("form") HorarioSemanalForm form, BindingResult errores, Model model,
			RedirectAttributes redirect) {
		horarios.validar(form, errores);
		if (errores.hasErrors()) {
			return mostrarBarbero(yo, id, model);
		}
		horarios.guardarBarbero(yo.getNegocioId(), id, form);
		redirect.addFlashAttribute("aviso", "Horario guardado.");
		return "redirect:/equipo/" + id;
	}

	@PostMapping("/equipo/{id}/horario/usar-el-de-la-barberia")
	String usarElDelLocal(@AuthenticationPrincipal UsuarioAutenticado yo, @PathVariable Long id,
			RedirectAttributes redirect) {
		horarios.usarElDelLocal(yo.getNegocioId(), id);
		redirect.addFlashAttribute("aviso", "Ahora trabaja en el horario de la barbería.");
		return "redirect:/equipo/" + id;
	}

	private String mostrarLocal(Model model) {
		model.addAttribute("titulo", "Horario de atención");
		model.addAttribute("descripcion", "Los días y horas en que abre la barbería. Si cortan al mediodía, usá el segundo turno.");
		model.addAttribute("accion", "/barberia/horario");
		model.addAttribute("volverA", "/barberia");
		model.addAttribute("volverTexto", "Barbería");
		model.addAttribute("pestana", "barberia");
		return "horario";
	}

	private String mostrarBarbero(UsuarioAutenticado yo, Long id, Model model) {
		Usuario miembro = equipo.buscar(yo.getNegocioId(), id);
		boolean propio = horarios.tieneHorarioPropio(yo.getNegocioId(), id);
		model.addAttribute("titulo", "Horario de " + miembro.getNombre());
		model.addAttribute("descripcion", propio
				? "Tiene un horario propio. Los turnos también respetan el horario de la barbería."
				: "Hoy trabaja en el horario de la barbería. Si guardás cambios acá, pasa a tener uno propio.");
		model.addAttribute("accion", "/equipo/" + id + "/horario");
		model.addAttribute("volverA", "/equipo/" + id);
		model.addAttribute("volverTexto", miembro.getNombre());
		model.addAttribute("pestana", "equipo");
		model.addAttribute("accionUsarLocal", propio ? "/equipo/" + id + "/horario/usar-el-de-la-barberia" : null);
		return "horario";
	}

}
