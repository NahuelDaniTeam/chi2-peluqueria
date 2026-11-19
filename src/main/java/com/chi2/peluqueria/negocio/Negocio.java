package com.chi2.peluqueria.negocio;

import java.time.Instant;
import java.time.ZoneId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Una barbería cliente de CHI2. Todo lo demás (usuarios, servicios, turnos) cuelga de un negocio. */
@Entity
@Table(name = "negocio")
public class Negocio {

	public static final String ZONA_HORARIA_POR_DEFECTO = "America/Argentina/Buenos_Aires";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 120)
	private String nombre;

	@Column(nullable = false, unique = true, length = 60)
	private String slug;

	@Column(name = "zona_horaria", nullable = false, length = 60)
	private String zonaHoraria;

	@Column(nullable = false)
	private boolean activo;

	@Column(name = "creado_en", nullable = false, updatable = false)
	private Instant creadoEn;

	protected Negocio() {
	}

	public Negocio(String nombre, String slug) {
		this.nombre = nombre;
		this.slug = slug;
		this.zonaHoraria = ZONA_HORARIA_POR_DEFECTO;
		this.activo = true;
		this.creadoEn = Instant.now();
	}

	public void suspender() {
		this.activo = false;
	}

	public void reactivar() {
		this.activo = true;
	}

	public Long getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

	public String getSlug() {
		return slug;
	}

	public String getZonaHoraria() {
		return zonaHoraria;
	}

	public ZoneId getZona() {
		return ZoneId.of(zonaHoraria);
	}

	public boolean isActivo() {
		return activo;
	}

	public Instant getCreadoEn() {
		return creadoEn;
	}

}
