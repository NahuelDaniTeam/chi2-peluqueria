package com.chi2.peluqueria.comun;

/**
 * Un dato que tiene que ser único ya está en uso (ej. un email o el link de una barbería).
 * {@code campo} es el nombre del campo del formulario, para mostrar el error al lado.
 */
public class DatoNoDisponibleException extends RuntimeException {

	private final String campo;

	public DatoNoDisponibleException(String campo, String mensaje) {
		super(mensaje);
		this.campo = campo;
	}

	public String getCampo() {
		return campo;
	}

}
