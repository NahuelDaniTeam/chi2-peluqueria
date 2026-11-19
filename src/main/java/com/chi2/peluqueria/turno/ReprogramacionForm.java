package com.chi2.peluqueria.turno;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.format.annotation.DateTimeFormat;

/** Nuevo horario (y quizás otro barbero) para un turno. Como en {@link ReservaForm}, viaja en la URL mientras se elige. */
public class ReprogramacionForm {

	/** {@code null} = el mismo barbero. */
	private Long barbero;

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate fecha;

	@DateTimeFormat(pattern = "HH:mm")
	private LocalTime hora;

	public Long getBarbero() {
		return barbero;
	}

	public void setBarbero(Long barbero) {
		this.barbero = barbero;
	}

	public LocalDate getFecha() {
		return fecha;
	}

	public void setFecha(LocalDate fecha) {
		this.fecha = fecha;
	}

	public LocalTime getHora() {
		return hora;
	}

	public void setHora(LocalTime hora) {
		this.hora = hora;
	}

}
