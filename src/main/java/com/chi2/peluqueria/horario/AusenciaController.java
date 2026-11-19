package com.chi2.peluqueria.horario;

import com.chi2.peluqueria.seguridad.UsuarioAutenticado;
import com.chi2.peluqueria.usuario.EquipoService;
import com.chi2.peluqueria.usuario.Usuario;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Días cerrados del local (/barberia/cierres) y ausencias de cada barbero (/equipo/{id}/ausencias). Solo DUENO. */
@Controller
class AusenciaController {

	private final AusenciaService ausencias;
	private final EquipoService equipo;

	AusenciaController(AusenciaService ausencias, EquipoService equipo) {
		this.ausencias = ausencias;
		this.equipo = equipo;
	}

	// ---- Local ----

	@GetMapping("/barberia/cierres")
	String local(@AuthenticationPrincipal UsuarioAutenticado yo, Model model) {
		model.addAttribute("form", new AusenciaForm());
		return mostrar(yo, null, model);
	}

	@PostMapping("/barberia/cierres")
	String crearLocal(@AuthenticationPrincipal UsuarioAutenticado yo, @Valid @ModelAttribute("form") AusenciaForm form,
			BindingResult errores, Model model, RedirectAttributes redirect) {
		return crear(yo, null, form, errores, model, redirect);
	}

	@PostMapping("/barberia/cierres/{ausenciaId}/borrar")
	String borrarLocal(@AuthenticationPrincipal UsuarioAutenticado yo, @PathVariable Long ausenciaId,
			RedirectAttributes redirect) {
		ausencias.borrar(yo.getNegocioId(), null, ausenciaId);
		redirect.addFlashAttribute("aviso", "Borrado.");
		return "redirect:/barberia/cierres";
	}

	// ---- Barbero ----

	@GetMapping("/equipo/{id}/ausencias")
	String barbero(@AuthenticationPrincipal UsuarioAutenticado yo, @PathVariable Long id, Model model) {
		model.addAttribute("form", new AusenciaForm());
		return mostrar(yo, id, model);
	}

	@PostMapping("/equipo/{id}/ausencias")
	String crearBarbero(@AuthenticationPrincipal UsuarioAutenticado yo, @PathVariable Long id,
			@Valid @ModelAttribute("form") AusenciaForm form, BindingResult errores, Model model,
			RedirectAttributes redirect) {
		return crear(yo, id, form, errores, model, redirect);
	}

	@PostMapping("/equipo/{id}/ausencias/{ausenciaId}/borrar")
	String borrarBarbero(@AuthenticationPrincipal UsuarioAutenticado yo, @PathVariable Long id,
			@PathVariable Long ausenciaId, RedirectAttributes redirect) {
		ausencias.borrar(yo.getNegocioId(), id, ausenciaId);
		redirect.addFlashAttribute("aviso", "Borrado.");
		return "redirect:/equipo/" + id + "/ausencias";
	}

	// ---- Comunes ----

	private String crear(UsuarioAutenticado yo, Long usuarioId, AusenciaForm form, BindingResult errores, Model model,
			RedirectAttributes redirect) {
		ausencias.validar(yo.getNegocioId(), form, errores);
		if (errores.hasErrors()) {
			return mostrar(yo, usuarioId, model);
		}
		Ausencia nueva = ausencias.crear(yo.getNegocioId(), usuarioId, form);
		redirect.addFlashAttribute("aviso", "Listo: " + nueva.getFechasTexto() + ".");
		return usuarioId == null ? "redirect:/barberia/cierres" : "redirect:/equipo/" + usuarioId + "/ausencias";
	}

	private String mostrar(UsuarioAutenticado yo, Long usuarioId, Model model) {
		model.addAttribute("ausencias", ausencias.proximas(yo.getNegocioId(), usuarioId));
		model.addAttribute("hoy", ausencias.hoy(yo.getNegocioId()));
		if (usuarioId == null) {
			model.addAttribute("titulo", "Días cerrados");
			model.addAttribute("descripcion", "Feriados, vacaciones o cualquier día en que la barbería no abre. Esos días no se dan turnos.");
			model.addAttribute("base", "/barberia/cierres");
			model.addAttribute("volverA", "/barberia");
			model.addAttribute("volverTexto", "Barbería");
			model.addAttribute("pestana", "barberia");
		}
		else {
			Usuario miembro = equipo.buscar(yo.getNegocioId(), usuarioId);
			model.addAttribute("titulo", "Ausencias de " + miembro.getNombre());
			model.addAttribute("descripcion", "Vacaciones, francos sueltos o días en que no viene. Esos días no se le dan turnos.");
			model.addAttribute("base", "/equipo/" + usuarioId + "/ausencias");
			model.addAttribute("volverA", "/equipo/" + usuarioId);
			model.addAttribute("volverTexto", miembro.getNombre());
			model.addAttribute("pestana", "equipo");
		}
		return "ausencias";
	}

}
