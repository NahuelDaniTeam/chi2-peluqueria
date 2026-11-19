package com.chi2.peluqueria.turno;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;

/**
 * Un turno nuevo cargado por la barbería. Sirve también para los links de la pantalla
 * ({@code /agenda/nuevo?servicio=1&barbero=2&fecha=2026-11-20}): lo elegido hasta ahora viaja en la URL.
 */
public class ReservaForm {

	private Long servicio;

	/** {@code null} = cualquiera. */
	private Long barbero;

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate fecha;

	@DateTimeFormat(pattern = "HH:mm")
	private LocalTime hora;

	@NotBlank(message = "Poné el nombre del cliente.")
	@Size(max = 80, message = "Hasta 80 letras.")
	private String nombre;

	@Size(max = 30, message = "Hasta 30 caracteres.")
	@Pattern(regexp = "^$|^[0-9+()\\- ]{6,30}$", message = "Solo números (puede llevar +, espacios o guiones).")
	private String telefono;

	@Size(max = 200, message = "Hasta 200 letras.")
	private String nota;

	public Long getServicio() {
		return servicio;
	}

	public void setServicio(Long servicio) {
		this.servicio = servicio;
	}

	public Long getBarbero() {
		return barbero;
	}

	public void setBarbero(Long barbero) {
		this.barbero = barbero;
	}

	public LocalDate getFecha() {
		return fecha;
	}

	public void setFecha(LocalDate fecha) {
		this.fecha = fecha;
	}

	public LocalTime getHora() {
		return hora;
	}

	public void setHora(LocalTime hora) {
		this.hora = hora;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getTelefono() {
		return telefono;
	}

	public void setTelefono(String telefono) {
		this.telefono = telefono;
	}

	public String getNota() {
		return nota;
	}

	public void setNota(String nota) {
		this.nota = nota;
	}

}
