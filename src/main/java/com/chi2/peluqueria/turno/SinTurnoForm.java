package com.chi2.peluqueria.turno;

import jakarta.validation.constraints.Size;

/**
 * Alguien que entró sin reservar. Quién y qué servicio viajan en la URL mientras se eligen
 * ({@code /agenda/sin-turno?barbero=2&servicio=1}), como en {@link ReservaForm}. Si trae medio de pago,
 * se carga ya atendido; si no, empieza ahora.
 */
public class SinTurnoForm extends CobroForm {

	private Long barbero;

	@Size(max = 80, message = "Hasta 80 letras.")
	private String nombre;

	public Long getBarbero() {
		return barbero;
	}

	public void setBarbero(Long barbero) {
		this.barbero = barbero;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

}
