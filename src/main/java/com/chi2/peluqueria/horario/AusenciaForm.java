package com.chi2.peluqueria.horario;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;

public class AusenciaForm {

	@NotNull(message = "Elegí desde qué día.")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate desde;

	@NotNull(message = "Elegí hasta qué día (si es uno solo, el mismo).")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate hasta;

	@Size(max = 120, message = "Máximo 120 caracteres.")
	private String motivo;

	public LocalDate getDesde() {
		return desde;
	}

	public void setDesde(LocalDate desde) {
		this.desde = desde;
	}

	public LocalDate getHasta() {
		return hasta;
	}

	public void setHasta(LocalDate hasta) {
		this.hasta = hasta;
	}

	public String getMotivo() {
		return motivo;
	}

	public void setMotivo(String motivo) {
		this.motivo = motivo;
	}

}
