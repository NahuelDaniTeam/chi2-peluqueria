package com.chi2.peluqueria.horario;

import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Una franja semanal de atención (ej. lunes de 9 a 13), del local o de un barbero. */
@Entity
@Table(name = "franja_horaria")
public class FranjaHoraria {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "negocio_id", nullable = false, updatable = false)
	private Long negocioId;

	/** {@code null} = horario del local. */
	@Column(name = "usuario_id", updatable = false)
	private Long usuarioId;

	/** 1 = lunes … 7 = domingo. */
	@Column(name = "dia_semana", nullable = false)
	private int diaSemana;

	@Column(nullable = false)
	private LocalTime desde;

	@Column(nullable = false)
	private LocalTime hasta;

	protected FranjaHoraria() {
	}

	public FranjaHoraria(Long negocioId, Long usuarioId, int diaSemana, LocalTime desde, LocalTime hasta) {
		this.negocioId = negocioId;
		this.usuarioId = usuarioId;
		this.diaSemana = diaSemana;
		this.desde = desde;
		this.hasta = hasta;
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

	public int getDiaSemana() {
		return diaSemana;
	}

	public LocalTime getDesde() {
		return desde;
	}

	public LocalTime getHasta() {
		return hasta;
	}

}
