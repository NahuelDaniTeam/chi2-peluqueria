package com.chi2.peluqueria.turno;

import java.time.Instant;

import com.chi2.peluqueria.usuario.Usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Una entrada del historial de un turno: qué pasó, quién lo hizo y cuándo. */
@Entity
@Table(name = "turno_cambio")
public class TurnoCambio {

	public enum Accion {

		CREADO("Reservado"),
		REPROGRAMADO("Reprogramado"),
		CANCELADO("Cancelado");

		private final String etiqueta;

		Accion(String etiqueta) {
			this.etiqueta = etiqueta;
		}

		public String getEtiqueta() {
			return etiqueta;
		}

	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "negocio_id", nullable = false, updatable = false)
	private Long negocioId;

	@Column(name = "turno_id", nullable = false, updatable = false)
	private Long turnoId;

	/** {@code null} = el cliente, desde el link. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "usuario_id", updatable = false)
	private Usuario usuario;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30, updatable = false)
	private Accion accion;

	@Column(length = 200, updatable = false)
	private String detalle;

	@Column(name = "creado_en", nullable = false, updatable = false)
	private Instant creadoEn;

	protected TurnoCambio() {
	}

	TurnoCambio(Turno turno, Usuario usuario, Accion accion, String detalle) {
		this.negocioId = turno.getNegocioId();
		this.turnoId = turno.getId();
		this.usuario = usuario;
		this.accion = accion;
		this.detalle = detalle;
		this.creadoEn = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public Long getTurnoId() {
		return turnoId;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public Accion getAccion() {
		return accion;
	}

	public String getDetalle() {
		return detalle;
	}

	public Instant getCreadoEn() {
		return creadoEn;
	}

}
