package com.chi2.peluqueria.horario;

import java.util.ArrayList;
import java.util.List;

import com.chi2.peluqueria.usuario.EquipoService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;

/**
 * Horario de atención del local y horario propio de cada barbero.
 * Un barbero sin franjas propias trabaja en el horario del local.
 */
@Service
@Transactional
public class HorarioService {

	private final FranjaHorariaRepository franjas;
	private final EquipoService equipo;

	HorarioService(FranjaHorariaRepository franjas, EquipoService equipo) {
		this.franjas = franjas;
		this.equipo = equipo;
	}

	// ---- Local ----

	@Transactional(readOnly = true)
	public boolean localTieneHorario(Long negocioId) {
		return franjas.existsByNegocioIdAndUsuarioIdIsNull(negocioId);
	}

	@Transactional(readOnly = true)
	public HorarioSemanalForm formLocal(Long negocioId) {
		List<FranjaHoraria> delLocal = franjas.findAllByNegocioIdAndUsuarioIdIsNullOrderByDiaSemanaAscDesdeAsc(negocioId);
		return delLocal.isEmpty() ? HorarioSemanalForm.sugerido() : HorarioSemanalForm.de(delLocal);
	}

	/** Reemplaza el horario del local. El formulario ya tiene que estar validado. */
	public void guardarLocal(Long negocioId, HorarioSemanalForm form) {
		franjas.deleteAllByNegocioIdAndUsuarioIdIsNull(negocioId);
		franjas.flush();
		franjas.saveAll(armarFranjas(negocioId, null, form));
	}

	// ---- Barbero ----

	@Transactional(readOnly = true)
	public boolean tieneHorarioPropio(Long negocioId, Long usuarioId) {
		equipo.buscar(negocioId, usuarioId);
		return franjas.existsByNegocioIdAndUsuarioId(negocioId, usuarioId);
	}

	/** Su horario propio, o el del local para arrancar a editar. */
	@Transactional(readOnly = true)
	public HorarioSemanalForm formBarbero(Long negocioId, Long usuarioId) {
		equipo.buscar(negocioId, usuarioId);
		List<FranjaHoraria> propias = franjas.findAllByNegocioIdAndUsuarioIdOrderByDiaSemanaAscDesdeAsc(negocioId, usuarioId);
		return propias.isEmpty() ? formLocal(negocioId) : HorarioSemanalForm.de(propias);
	}

	public void guardarBarbero(Long negocioId, Long usuarioId, HorarioSemanalForm form) {
		equipo.buscar(negocioId, usuarioId);
		franjas.deleteAllByNegocioIdAndUsuarioId(negocioId, usuarioId);
		franjas.flush();
		franjas.saveAll(armarFranjas(negocioId, usuarioId, form));
	}

	/** Borra el horario propio: vuelve a trabajar en el horario del local. */
	public void usarElDelLocal(Long negocioId, Long usuarioId) {
		equipo.buscar(negocioId, usuarioId);
		franjas.deleteAllByNegocioIdAndUsuarioId(negocioId, usuarioId);
	}

	// ---- Validación ----

	/** Deja un error por día (en {@code dias[i].abierto}) y uno general si no hay ningún día abierto. */
	public void validar(HorarioSemanalForm form, Errors errores) {
		boolean algunoAbierto = false;
		for (int i = 0; i < form.getDias().size(); i++) {
			HorarioSemanalForm.Dia dia = form.getDias().get(i);
			if (!dia.isAbierto()) {
				continue;
			}
			algunoAbierto = true;
			String error = errorDelDia(dia);
			if (error != null) {
				errores.rejectValue("dias[" + i + "].abierto", "horario", error);
			}
		}
		if (!algunoAbierto) {
			errores.reject("sinDias", "Marcá al menos un día en que se atiende.");
		}
	}

	private String errorDelDia(HorarioSemanalForm.Dia dia) {
		if (dia.getDesde1() == null || dia.getHasta1() == null) {
			return "Completá a qué hora abre y a qué hora cierra.";
		}
		if (!dia.getDesde1().isBefore(dia.getHasta1())) {
			return "La hora de cierre tiene que ser después de la de apertura.";
		}
		boolean hayDesde2 = dia.getDesde2() != null;
		boolean hayHasta2 = dia.getHasta2() != null;
		if (hayDesde2 != hayHasta2) {
			return "Completá las dos horas del segundo turno, o dejalas vacías.";
		}
		if (hayDesde2) {
			if (!dia.getDesde2().isBefore(dia.getHasta2())) {
				return "En el segundo turno, la hora de cierre tiene que ser después de la de apertura.";
			}
			if (dia.getDesde2().isBefore(dia.getHasta1())) {
				return "El segundo turno tiene que empezar después de que termina el primero.";
			}
		}
		return null;
	}

	private List<FranjaHoraria> armarFranjas(Long negocioId, Long usuarioId, HorarioSemanalForm form) {
		List<FranjaHoraria> nuevas = new ArrayList<>();
		for (HorarioSemanalForm.Dia dia : form.getDias()) {
			if (!dia.isAbierto() || dia.getNumero() < 1 || dia.getNumero() > 7) {
				continue;
			}
			nuevas.add(new FranjaHoraria(negocioId, usuarioId, dia.getNumero(), dia.getDesde1(), dia.getHasta1()));
			if (dia.getDesde2() != null) {
				nuevas.add(new FranjaHoraria(negocioId, usuarioId, dia.getNumero(), dia.getDesde2(), dia.getHasta2()));
			}
		}
		return nuevas;
	}

}
