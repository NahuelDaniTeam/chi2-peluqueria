package com.chi2.peluqueria.turno;

/** Cómo pagó el cliente. Solo se marca para llevar el control: el sistema no cobra ni maneja dinero. */
public enum MedioPago {

	EFECTIVO("Efectivo"),
	TRANSFERENCIA("Transferencia");

	private final String etiqueta;

	MedioPago(String etiqueta) {
		this.etiqueta = etiqueta;
	}

	public String getEtiqueta() {
		return etiqueta;
	}

}
