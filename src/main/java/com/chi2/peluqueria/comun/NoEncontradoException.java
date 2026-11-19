package com.chi2.peluqueria.comun;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * El recurso no existe o pertenece a otra barbería. Las dos situaciones responden igual (404)
 * para no revelar qué ids existen en otros negocios.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class NoEncontradoException extends RuntimeException {

	public NoEncontradoException() {
		super("No encontrado");
	}

}
