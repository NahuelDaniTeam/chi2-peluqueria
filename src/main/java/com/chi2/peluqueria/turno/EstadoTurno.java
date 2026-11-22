package com.chi2.peluqueria.turno;

import java.util.List;

/**
 * En qué anda un turno: Reservado → En curso → Finalizado, o bien Cancelado / Ausente.
 * Los de la atención (en curso, finalizado, ausente) los marca el barbero (ver {@link AtencionService}).
 */
public enum EstadoTurno {

	RESERVADO("Reservado"),
	EN_CURSO("En curso"),
	FINALIZADO("Finalizado"),
	CANCELADO("Cancelado"),
	/** El cliente no vino. */
	AUSENTE("Ausente");

	/**
	 * Los que ocupan su lugar en la agenda. Igual que la restricción {@code turno_sin_superposicion} de la base
	 * (que además deja afuera la atención sin turno, ver V7__atencion.sql).
	 */
	static final List<EstadoTurno> OCUPAN_LUGAR = List.of(RESERVADO, EN_CURSO, FINALIZADO);

	private final String etiqueta;

	EstadoTurno(String etiqueta) {
		this.etiqueta = etiqueta;
	}

	public String getEtiqueta() {
		return etiqueta;
	}

	public boolean ocupaLugar() {
		return OCUPAN_LUGAR.contains(this);
	}

}
