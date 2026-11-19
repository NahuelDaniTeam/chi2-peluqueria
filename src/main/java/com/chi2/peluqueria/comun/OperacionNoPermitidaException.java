package com.chi2.peluqueria.comun;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Un pedido que la pantalla nunca ofrece, pero que alguien podría mandar a mano (ej. darse de baja a sí mismo). */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class OperacionNoPermitidaException extends RuntimeException {

	public OperacionNoPermitidaException(String mensaje) {
		super(mensaje);
	}

}
