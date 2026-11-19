package com.chi2.peluqueria.barbero;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Un servicio que hace un barbero, con su precio especial si cobra distinto que el local. */
@Entity
@Table(name = "barbero_servicio")
public class BarberoServicio {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "negocio_id", nullable = false, updatable = false)
	private Long negocioId;

	@Column(name = "usuario_id", nullable = false, updatable = false)
	private Long usuarioId;

	@Column(name = "servicio_id", nullable = false, updatable = false)
	private Long servicioId;

	/** {@code null} = cobra el precio del local. */
	@Column(name = "precio_especial")
	private Integer precioEspecial;

	protected BarberoServicio() {
	}

	public BarberoServicio(Long negocioId, Long usuarioId, Long servicioId) {
		this.negocioId = negocioId;
		this.usuarioId = usuarioId;
		this.servicioId = servicioId;
	}

	public void cambiarPrecioEspecial(Integer precioEspecial) {
		this.precioEspecial = precioEspecial;
	}

	public Long getId() {
		return id;
	}

	public Long getNegocioId() {
		return negocioId;
	}

	public Long getUsuarioId() {
		return usuarioId;
	}

	public Long getServicioId() {
		return servicioId;
	}

	public Integer getPrecioEspecial() {
		return precioEspecial;
	}

}
