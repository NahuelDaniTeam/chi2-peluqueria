package com.chi2.peluqueria.turno;

import java.util.Optional;

import com.chi2.peluqueria.comun.Formatos;

import org.springframework.validation.BindingResult;

/**
 * Lo que se hizo y se cobró al finalizar un turno (o al corregirlo). El monto llega como lo escribe la
 * gente ("12000", "12.000", "$ 12.000"), sin centavos.
 */
public class CobroForm {

	private Long servicio;

	private String monto;

	private MedioPago medioPago;

	/** Valida y arma el cobro; si falta algo, deja el error al lado del campo y devuelve vacío. */
	Optional<AtencionService.Cobro> leer(BindingResult errores) {
		Optional<Integer> pesos = Formatos.leerPrecio(monto);
		if (pesos.isEmpty() && !errores.hasFieldErrors("monto")) {
			errores.rejectValue("monto", "invalido", "Poné cuánto se cobró, en pesos y sin centavos.");
		}
		if (medioPago == null && !errores.hasFieldErrors("medioPago")) {
			errores.rejectValue("medioPago", "falta", "Elegí cómo pagó.");
		}
		if (errores.hasErrors()) {
			return Optional.empty();
		}
		return Optional.of(new AtencionService.Cobro(servicio, pesos.get(), medioPago));
	}

	public Long getServicio() {
		return servicio;
	}

	public void setServicio(Long servicio) {
		this.servicio = servicio;
	}

	public String getMonto() {
		return monto;
	}

	public void setMonto(String monto) {
		this.monto = monto;
	}

	public MedioPago getMedioPago() {
		return medioPago;
	}

	public void setMedioPago(MedioPago medioPago) {
		this.medioPago = medioPago;
	}

}
