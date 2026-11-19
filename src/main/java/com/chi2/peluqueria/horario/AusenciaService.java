package com.chi2.peluqueria.horario;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

import com.chi2.peluqueria.comun.NoEncontradoException;
import com.chi2.peluqueria.negocio.NegocioRepository;
import com.chi2.peluqueria.usuario.EquipoService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;

/**
 * Cierres del local y ausencias de cada barbero. {@code usuarioId == null} significa "el local".
 * Solo se listan las que todavía no pasaron.
 */
@Service
@Transactional
public class AusenciaService {

	private final AusenciaRepository ausencias;
	private final NegocioRepository negocios;
	private final EquipoService equipo;

	AusenciaService(AusenciaRepository ausencias, NegocioRepository negocios, EquipoService equipo) {
		this.ausencias = ausencias;
		this.negocios = negocios;
		this.equipo = equipo;
	}

	/** La fecha de hoy en la zona horaria de la barbería. */
	@Transactional(readOnly = true)
	public LocalDate hoy(Long negocioId) {
		String zona = negocios.findById(negocioId).orElseThrow(NoEncontradoException::new).getZonaHoraria();
		return LocalDate.now(ZoneId.of(zona));
	}

	@Transactional(readOnly = true)
	public List<Ausencia> proximas(Long negocioId, Long usuarioId) {
		LocalDate hoy = hoy(negocioId);
		if (usuarioId == null) {
			return ausencias.findAllByNegocioIdAndUsuarioIdIsNullAndHastaGreaterThanEqualOrderByDesdeAsc(negocioId, hoy);
		}
		equipo.buscar(negocioId, usuarioId);
		return ausencias.findAllByNegocioIdAndUsuarioIdAndHastaGreaterThanEqualOrderByDesdeAsc(negocioId, usuarioId, hoy);
	}

	public void validar(Long negocioId, AusenciaForm form, Errors errores) {
		if (form.getDesde() == null || form.getHasta() == null) {
			return;
		}
		if (form.getHasta().isBefore(form.getDesde())) {
			errores.rejectValue("hasta", "antes", "Tiene que ser el mismo día o después de \"desde\".");
		}
		else if (form.getHasta().isBefore(hoy(negocioId))) {
			errores.rejectValue("hasta", "pasado", "Esa fecha ya pasó.");
		}
	}

	public Ausencia crear(Long negocioId, Long usuarioId, AusenciaForm form) {
		if (usuarioId != null) {
			equipo.buscar(negocioId, usuarioId);
		}
		return ausencias.save(new Ausencia(negocioId, usuarioId, form.getDesde(), form.getHasta(), form.getMotivo()));
	}

	/** Borra una ausencia del local ({@code usuarioId == null}) o de ese barbero; si no coincide, 404. */
	public void borrar(Long negocioId, Long usuarioId, Long ausenciaId) {
		Ausencia ausencia = ausencias.findByIdAndNegocioId(ausenciaId, negocioId).orElseThrow(NoEncontradoException::new);
		if (!Objects.equals(ausencia.getUsuarioId(), usuarioId)) {
			throw new NoEncontradoException();
		}
		ausencias.delete(ausencia);
	}

}
