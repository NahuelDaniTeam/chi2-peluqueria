package com.chi2.peluqueria.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Formulario del dueño para sumar a alguien a su equipo. */
public class NuevoMiembroForm {

	@NotBlank(message = "Completá el nombre.")
	@Size(max = 120, message = "Máximo 120 caracteres.")
	private String nombre;

	@NotBlank(message = "Completá el email.")
	@Email(message = "Ingresá un email válido.")
	@Size(max = 254, message = "Máximo 254 caracteres.")
	private String email;

	@NotBlank(message = "Elegí una contraseña inicial.")
	@Size(min = 8, max = 72, message = "Tiene que tener entre 8 y 72 caracteres.")
	private String contrasena;

	@NotNull(message = "Elegí un rol.")
	private Rol rol = Rol.BARBERO;

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getContrasena() {
		return contrasena;
	}

	public void setContrasena(String contrasena) {
		this.contrasena = contrasena;
	}

	public Rol getRol() {
		return rol;
	}

	public void setRol(Rol rol) {
		this.rol = rol;
	}

}
