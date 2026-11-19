package com.chi2.peluqueria.negocio;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Formulario de CHI2 para dar de alta una barbería con su dueño. */
public class NuevoNegocioForm {

	@NotBlank(message = "Completá el nombre de la barbería.")
	@Size(max = 120, message = "Máximo 120 caracteres.")
	private String nombre;

	/** Opcional: si queda vacío, se arma con el nombre. */
	@Size(max = Slugs.LARGO_MAXIMO, message = "Máximo 60 caracteres.")
	private String slug;

	@NotBlank(message = "Completá el nombre del dueño.")
	@Size(max = 120, message = "Máximo 120 caracteres.")
	private String duenoNombre;

	@NotBlank(message = "Completá el email del dueño.")
	@Email(message = "Ingresá un email válido.")
	@Size(max = 254, message = "Máximo 254 caracteres.")
	private String duenoEmail;

	@NotBlank(message = "Elegí una contraseña inicial.")
	@Size(min = 8, max = 72, message = "Tiene que tener entre 8 y 72 caracteres.")
	private String duenoContrasena;

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getSlug() {
		return slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}

	public String getDuenoNombre() {
		return duenoNombre;
	}

	public void setDuenoNombre(String duenoNombre) {
		this.duenoNombre = duenoNombre;
	}

	public String getDuenoEmail() {
		return duenoEmail;
	}

	public void setDuenoEmail(String duenoEmail) {
		this.duenoEmail = duenoEmail;
	}

	public String getDuenoContrasena() {
		return duenoContrasena;
	}

	public void setDuenoContrasena(String duenoContrasena) {
		this.duenoContrasena = duenoContrasena;
	}

}
