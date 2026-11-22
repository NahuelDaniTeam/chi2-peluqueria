package com.chi2.peluqueria.turno;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import com.chi2.peluqueria.comun.Formatos;
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
 * La base impide que un barbero tenga dos turnos superpuestos (ver V6__turnos.sql y V7__atencion.sql).
 * <p>
 * Se atiende como un viaje de Uber: Reservado → En curso → Finalizado (con el cobro marcado), o bien
 * Cancelado / Ausente. La atención sin turno ({@link #sinTurno}) arranca directamente en curso.
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

	/** Puede faltar solo en la atención sin turno. */
	@Column(name = "cliente_nombre", length = 80)
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

	/** Alguien que entró sin reservar. */
	@Column(name = "sin_turno", nullable = false, updatable = false)
	private boolean sinTurno;

	@Column(name = "empezado_en")
	private Instant empezadoEn;

	@Column(name = "finalizado_en")
	private Instant finalizadoEn;

	/** Lo cobrado, en pesos sin centavos. Solo en los finalizados. */
	private Integer monto;

	@Enumerated(EnumType.STRING)
	@Column(name = "medio_pago", length = 20)
	private MedioPago medioPago;

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

	/**
	 * Alguien que entró sin reservar: queda en curso desde {@code inicio}, ocupando lo que dura el servicio.
	 * El nombre es opcional.
	 */
	public static Turno sinTurno(Long negocioId, Usuario barbero, Servicio servicio, Instant inicio, Duration duracion,
			String clienteNombre, Long creadoPor) {
		Turno turno = new Turno();
		turno.negocioId = negocioId;
		turno.barbero = barbero;
		turno.servicio = servicio;
		turno.inicio = inicio;
		turno.fin = inicio.plus(duracion);
		turno.clienteNombre = vacioANulo(clienteNombre);
		turno.estado = EstadoTurno.EN_CURSO;
		turno.sinTurno = true;
		turno.creadoPor = creadoPor;
		turno.creadoEn = Instant.now();
		turno.empezadoEn = inicio;
		return turno;
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

	// ---- Atención ----

	/** Se puede empezar el mismo día del turno (el cliente puede llegar temprano o tarde). */
	public boolean puedeEmpezar(Instant ahora, ZoneId zona) {
		return isReservado() && LocalDate.ofInstant(inicio, zona).equals(LocalDate.ofInstant(ahora, zona));
	}

	/** "No vino" recién cuando llegó la hora. */
	public boolean puedeMarcarAusente(Instant ahora) {
		return isReservado() && !inicio.isAfter(ahora);
	}

	/** En curso, o reservado cuando ya llegó su hora (el barbero se olvidó de tocar "Empezar"). */
	public boolean puedeFinalizar(Instant ahora) {
		return estado == EstadoTurno.EN_CURSO || puedeMarcarAusente(ahora);
	}

	/** Deshace un "Empezar" o un "No vino" tocados sin querer (no en la atención sin turno, que se anula). */
	public boolean puedeVolverAReservado() {
		return !sinTurno && (estado == EstadoTurno.EN_CURSO || estado == EstadoTurno.AUSENTE);
	}

	/** Una atención sin turno cargada por error se anula (o el cliente se fue sin atenderse). */
	public boolean puedeAnularse() {
		return sinTurno && estado == EstadoTurno.EN_CURSO;
	}

	/** Reservado y ya pasó su horario: falta cerrarlo (finalizado, ausente o cancelado). */
	public boolean isSinCerrar(Instant ahora) {
		return isReservado() && !fin.isAfter(ahora);
	}

	public void empezar(Instant ahora) {
		exigirReservado();
		this.estado = EstadoTurno.EN_CURSO;
		this.empezadoEn = ahora;
	}

	/**
	 * Lo cierra con lo que se hizo y se cobró. El servicio puede ser otro que el reservado (pasó de corte a
	 * corte + barba). El horario de un turno reservado no cambia; el de la atención sin turno se acorta hasta
	 * ahora si terminó antes, para que ese rato vuelva a quedar libre.
	 */
	public void finalizar(Servicio servicio, int monto, MedioPago medioPago, Instant ahora) {
		if (!puedeFinalizar(ahora)) {
			throw new IllegalStateException("El turno no se puede finalizar: " + estado);
		}
		cobrar(servicio, monto, medioPago);
		this.estado = EstadoTurno.FINALIZADO;
		this.finalizadoEn = ahora;
		if (sinTurno) {
			Instant minimo = inicio.plus(Duration.ofMinutes(1));
			Instant hasta = ahora.isAfter(minimo) ? ahora : minimo;
			if (hasta.isBefore(fin)) {
				this.fin = hasta;
			}
		}
	}

	/** Corrige un turno ya finalizado: otro servicio, otro monto u otro medio de pago. */
	public void corregirCobro(Servicio servicio, int monto, MedioPago medioPago) {
		if (estado != EstadoTurno.FINALIZADO) {
			throw new IllegalStateException("Solo se corrige un turno finalizado: " + estado);
		}
		cobrar(servicio, monto, medioPago);
	}

	public void marcarAusente(Instant ahora) {
		if (!puedeMarcarAusente(ahora)) {
			throw new IllegalStateException("Todavía no es la hora del turno o ya no está reservado: " + estado);
		}
		this.estado = EstadoTurno.AUSENTE;
	}

	public void volverAReservado() {
		if (!puedeVolverAReservado()) {
			throw new IllegalStateException("No se puede volver a reservado: " + estado);
		}
		this.estado = EstadoTurno.RESERVADO;
		this.empezadoEn = null;
	}

	public void anular() {
		if (!puedeAnularse()) {
			throw new IllegalStateException("Solo se anula una atención sin turno en curso: " + estado);
		}
		this.estado = EstadoTurno.CANCELADO;
	}

	private void cobrar(Servicio servicio, int monto, MedioPago medioPago) {
		if (monto < 0 || medioPago == null) {
			throw new IllegalArgumentException("Falta el monto o el medio de pago");
		}
		this.servicio = servicio;
		this.monto = monto;
		this.medioPago = medioPago;
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

	public boolean isEnCurso() {
		return estado == EstadoTurno.EN_CURSO;
	}

	public boolean isFinalizado() {
		return estado == EstadoTurno.FINALIZADO;
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

	/** El nombre para mostrar: a quien entró sin turno y no dio el nombre se lo muestra así. */
	public String getClienteTexto() {
		return clienteNombre != null ? clienteNombre : "Cliente sin turno";
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

	public boolean isSinTurno() {
		return sinTurno;
	}

	public Instant getEmpezadoEn() {
		return empezadoEn;
	}

	public Instant getFinalizadoEn() {
		return finalizadoEn;
	}

	public Integer getMonto() {
		return monto;
	}

	public MedioPago getMedioPago() {
		return medioPago;
	}

	/** "$ 12.000" (solo en los finalizados). */
	public String getMontoTexto() {
		return monto == null ? null : Formatos.precio(monto);
	}

	/** "$ 12.000 · Efectivo" (solo en los finalizados). */
	public String getCobroTexto() {
		return monto == null ? null : Formatos.precio(monto) + " · " + medioPago.getEtiqueta();
	}

}
