package com.chi2.peluqueria.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CambioContrasenaForm {

	@NotBlank(message = "Completá tu contraseña actual.")
	private String actual;

	@NotBlank(message = "Elegí una contraseña nueva.")
	@Size(min = 8, max = 72, message = "Tiene que tener entre 8 y 72 caracteres.")
	private String nueva;

	@NotBlank(message = "Repetí la contraseña nueva.")
	private String repetida;

	public String getActual() {
		return actual;
	}

	public void setActual(String actual) {
		this.actual = actual;
	}

	public String getNueva() {
		return nueva;
	}

	public void setNueva(String nueva) {
		this.nueva = nueva;
	}

	public String getRepetida() {
		return repetida;
	}

	public void setRepetida(String repetida) {
		this.repetida = repetida;
	}

}
