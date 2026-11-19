package com.chi2.peluqueria.horario;

import java.time.Instant;
import java.time.LocalDate;

import com.chi2.peluqueria.comun.Formatos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Días completos sin atender: cierre del local (feriado, vacaciones) o ausencia de un barbero. */
@Entity
@Table(name = "ausencia")
public class Ausencia {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "negocio_id", nullable = false, updatable = false)
	private Long negocioId;

	/** {@code null} = cierra la barbería. */
	@Column(name = "usuario_id", updatable = false)
	private Long usuarioId;

	@Column(nullable = false)
	private LocalDate desde;

	@Column(nullable = false)
	private LocalDate hasta;

	@Column(length = 120)
	private String motivo;

	@Column(name = "creado_en", nullable = false, updatable = false)
	private Instant creadoEn;

	protected Ausencia() {
	}

	public Ausencia(Long negocioId, Long usuarioId, LocalDate desde, LocalDate hasta, String motivo) {
		this.negocioId = negocioId;
		this.usuarioId = usuarioId;
		this.desde = desde;
		this.hasta = hasta;
		this.motivo = motivo == null || motivo.isBlank() ? null : motivo.trim();
		this.creadoEn = Instant.now();
	}

	/** "lun 24/11" o "lun 24/11 al vie 28/11". */
	public String getFechasTexto() {
		return desde.equals(hasta)
				? Formatos.fechaCorta(desde)
				: Formatos.fechaCorta(desde) + " al " + Formatos.fechaCorta(hasta);
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

	public LocalDate getDesde() {
		return desde;
	}

	public LocalDate getHasta() {
		return hasta;
	}

	public String getMotivo() {
		return motivo;
	}

}
