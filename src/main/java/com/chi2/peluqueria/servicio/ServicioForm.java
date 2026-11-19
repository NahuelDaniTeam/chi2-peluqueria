package com.chi2.peluqueria.servicio;

import java.util.Optional;

import com.chi2.peluqueria.comun.Formatos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ServicioForm {

	@NotBlank(message = "Completá el nombre.")
	@Size(max = 80, message = "Máximo 80 caracteres.")
	private String nombre;

	@NotNull(message = "Elegí cuánto dura.")
	@Min(value = 5, message = "Elegí cuánto dura.")
	@Max(value = 480, message = "Como máximo 8 horas.")
	private Integer duracionMinutos = 30;

	/** Texto tal como lo escribe el dueño ("12.000"); se lee con {@link Formatos#leerPrecio}. */
	@NotBlank(message = "Completá el precio.")
	private String precio;

	public static ServicioForm de(Servicio servicio) {
		ServicioForm form = new ServicioForm();
		form.setNombre(servicio.getNombre());
		form.setDuracionMinutos(servicio.getDuracionMinutos());
		form.setPrecio(String.valueOf(servicio.getPrecio()));
		return form;
	}

	public Optional<Integer> precioEnPesos() {
		return Formatos.leerPrecio(precio);
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public Integer getDuracionMinutos() {
		return duracionMinutos;
	}

	public void setDuracionMinutos(Integer duracionMinutos) {
		this.duracionMinutos = duracionMinutos;
	}

	public String getPrecio() {
		return precio;
	}

	public void setPrecio(String precio) {
		this.precio = precio;
	}

}
