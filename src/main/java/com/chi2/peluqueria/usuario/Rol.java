package com.chi2.peluqueria.usuario;

import java.util.List;

public enum Rol {

	/** Nosotros: da de alta y suspende barberías. No pertenece a ningún negocio. */
	ADMIN("CHI2"),
	/** Ve y configura todo su negocio. */
	DUENO("Dueño o encargado"),
	/** Ve solo lo suyo. */
	BARBERO("Barbero");

	private final String etiqueta;

	Rol(String etiqueta) {
		this.etiqueta = etiqueta;
	}

	public String getEtiqueta() {
		return etiqueta;
	}

	/** Roles que un dueño puede asignar dentro de su barbería (nunca ADMIN). */
	public static List<Rol> deNegocio() {
		return List.of(BARBERO, DUENO);
	}

	public boolean esDeNegocio() {
		return this != ADMIN;
	}

}
