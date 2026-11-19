package com.chi2.peluqueria.pagina;

import java.util.List;

import com.chi2.peluqueria.horario.HorarioService;
import com.chi2.peluqueria.servicio.ServicioRepository;
import com.chi2.peluqueria.usuario.UsuarioRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Los pasos para que una barbería nueva pueda empezar a dar turnos. Se le muestran al dueño hasta que estén todos. */
@Component
public class PuestaEnMarcha {

	public record Paso(String texto, String enlace, boolean hecho) {
	}

	private final ServicioRepository servicios;
	private final HorarioService horarios;
	private final UsuarioRepository usuarios;

	PuestaEnMarcha(ServicioRepository servicios, HorarioService horarios, UsuarioRepository usuarios) {
		this.servicios = servicios;
		this.horarios = horarios;
		this.usuarios = usuarios;
	}

	@Transactional(readOnly = true)
	public List<Paso> pasos(Long negocioId) {
		return List.of(
				new Paso("Cargá tus servicios y precios", "/barberia/servicios",
						servicios.countByNegocioIdAndActivoTrue(negocioId) > 0),
				new Paso("Cargá el horario de atención", "/barberia/horario", horarios.localTieneHorario(negocioId)),
				new Paso("Sumá a tu equipo y marcá quién atiende", "/equipo",
						!usuarios.findAllByNegocioIdAndActivoTrueAndAtiendeTrueOrderByNombreAsc(negocioId).isEmpty()));
	}

}
