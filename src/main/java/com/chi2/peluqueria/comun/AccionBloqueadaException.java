package com.chi2.peluqueria.comun;

/**
 * Algo que en general se puede hacer, pero ahora no, por cómo están los datos (ej. dar de baja a un
 * barbero que tiene turnos reservados). El mensaje es para el usuario: el controlador lo muestra como aviso.
 */
public class AccionBloqueadaException extends RuntimeException {

	public AccionBloqueadaException(String mensaje) {
		super(mensaje);
	}

}
