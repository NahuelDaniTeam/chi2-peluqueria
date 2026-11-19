package com.chi2.peluqueria.turno;

import java.time.Duration;
import java.time.Instant;

import com.chi2.peluqueria.servicio.Servicio;
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

/**
 * Un turno de la agenda: un cliente, con un barbero, para un servicio, de tal hora a tal hora.
 * La base impide que un barbero tenga dos turnos superpuestos (ver V6__turnos.sql).
 */
@Entity
@Table(name = "turno")
public class Turno {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "negocio_id", nullable = false, updatable = false)
	private Long negocioId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "barbero_id", nullable = false)
	private Usuario barbero;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "servicio_id", nullable = false)
	private Servicio servicio;

	@Column(nullable = false)
	private Instant inicio;

	@Column(nullable = false)
	private Instant fin;

	@Column(name = "cliente_nombre", nullable = false, length = 80)
	private String clienteNombre;

	@Column(name = "cliente_telefono", length = 30)
	private String clienteTelefono;

	@Column(length = 200)
	private String nota;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EstadoTurno estado;

	@Column(name = "cancelacion_tardia", nullable = false)
	private boolean cancelacionTardia;

	/** {@code null} cuando lo reservó el cliente desde el link. */
	@Column(name = "creado_por", updatable = false)
	private Long creadoPor;

	@Column(name = "creado_en", nullable = false, updatable = false)
	private Instant creadoEn;

	protected Turno() {
	}

	public Turno(Long negocioId, Usuario barbero, Servicio servicio, Instant inicio, Duration duracion,
			String clienteNombre, String clienteTelefono, String nota, Long creadoPor) {
		this.negocioId = negocioId;
		this.barbero = barbero;
		this.servicio = servicio;
		this.inicio = inicio;
		this.fin = inicio.plus(duracion);
		this.clienteNombre = clienteNombre.trim();
		this.clienteTelefono = vacioANulo(clienteTelefono);
		this.nota = vacioANulo(nota);
		this.estado = EstadoTurno.RESERVADO;
		this.creadoPor = creadoPor;
		this.creadoEn = Instant.now();
	}

	/** Lo mueve a otro horario (y quizás a otro barbero), con el mismo largo. */
	public void reprogramar(Usuario barbero, Instant inicio) {
		exigirReservado();
		Duration duracion = getDuracion();
		this.barbero = barbero;
		this.inicio = inicio;
		this.fin = inicio.plus(duracion);
	}

	/** @param tardia si se avisó con menos de 24 horas */
	public void cancelar(boolean tardia) {
		exigirReservado();
		this.estado = EstadoTurno.CANCELADO;
		this.cancelacionTardia = tardia;
	}

	private void exigirReservado() {
		if (estado != EstadoTurno.RESERVADO) {
			throw new IllegalStateException("El turno ya no está reservado: " + estado);
		}
	}

	private static String vacioANulo(String texto) {
		return texto == null || texto.isBlank() ? null : texto.trim();
	}

	public Duration getDuracion() {
		return Duration.between(inicio, fin);
	}

	public boolean isReservado() {
		return estado == EstadoTurno.RESERVADO;
	}

	public Long getId() {
		return id;
	}

	public Long getNegocioId() {
		return negocioId;
	}

	public Usuario getBarbero() {
		return barbero;
	}

	public Servicio getServicio() {
		return servicio;
	}

	public Instant getInicio() {
		return inicio;
	}

	public Instant getFin() {
		return fin;
	}

	public String getClienteNombre() {
		return clienteNombre;
	}

	public String getClienteTelefono() {
		return clienteTelefono;
	}

	public String getNota() {
		return nota;
	}

	public EstadoTurno getEstado() {
		return estado;
	}

	public boolean isCancelacionTardia() {
		return cancelacionTardia;
	}

	public Long getCreadoPor() {
		return creadoPor;
	}

	public Instant getCreadoEn() {
		return creadoEn;
	}

}
