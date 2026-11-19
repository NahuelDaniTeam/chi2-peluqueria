package com.chi2.peluqueria.servicio;

import java.time.Instant;

import com.chi2.peluqueria.comun.Formatos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Un servicio del catálogo de una barbería (corte, barba, corte + tintura…).
 * Las combinaciones son servicios propios, con su duración y su precio.
 */
@Entity
@Table(name = "servicio")
public class Servicio {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "negocio_id", nullable = false, updatable = false)
	private Long negocioId;

	@Column(nullable = false, length = 80)
	private String nombre;

	@Column(name = "duracion_minutos", nullable = false)
	private int duracionMinutos;

	/** En pesos, sin centavos. */
	@Column(nullable = false)
	private int precio;

	@Column(nullable = false)
	private boolean activo;

	@Column(name = "creado_en", nullable = false, updatable = false)
	private Instant creadoEn;

	protected Servicio() {
	}

	public Servicio(Long negocioId, String nombre, int duracionMinutos, int precio) {
		this.negocioId = negocioId;
		this.activo = true;
		this.creadoEn = Instant.now();
		actualizar(nombre, duracionMinutos, precio);
	}

	public void actualizar(String nombre, int duracionMinutos, int precio) {
		this.nombre = nombre.trim();
		this.duracionMinutos = duracionMinutos;
		this.precio = precio;
	}

	/** Deja de ofrecerse. No se borra porque los turnos viejos lo siguen usando. */
	public void quitar() {
		this.activo = false;
	}

	public void reactivar() {
		this.activo = true;
	}

	public Long getId() {
		return id;
	}

	public Long getNegocioId() {
		return negocioId;
	}

	public String getNombre() {
		return nombre;
	}

	public int getDuracionMinutos() {
		return duracionMinutos;
	}

	public int getPrecio() {
		return precio;
	}

	public boolean isActivo() {
		return activo;
	}

	public String getDuracionTexto() {
		return Formatos.duracion(duracionMinutos);
	}

	public String getPrecioTexto() {
		return Formatos.precio(precio);
	}

}
