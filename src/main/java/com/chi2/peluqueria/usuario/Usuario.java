package com.chi2.peluqueria.usuario;

import java.time.Instant;

import com.chi2.peluqueria.negocio.Negocio;

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

/** Una persona que entra al sistema: alguien de CHI2, un dueño o un barbero. */
@Entity
@Table(name = "usuario")
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** {@code null} solo para los usuarios de CHI2. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "negocio_id")
	private Negocio negocio;

	@Column(nullable = false, length = 120)
	private String nombre;

	@Column(nullable = false, length = 254)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Rol rol;

	@Column(nullable = false)
	private boolean activo;

	/** Si atiende clientes: aparece en la agenda y se le pueden dar turnos. */
	@Column(nullable = false)
	private boolean atiende;

	@Column(name = "creado_en", nullable = false, updatable = false)
	private Instant creadoEn;

	protected Usuario() {
	}

	private Usuario(Negocio negocio, String nombre, String email, String passwordHash, Rol rol) {
		this.negocio = negocio;
		this.nombre = nombre.trim();
		this.email = Emails.normalizar(email);
		this.passwordHash = passwordHash;
		this.rol = rol;
		this.activo = true;
		// Los barberos atienden; el dueño lo marca en su perfil si también corta
		this.atiende = rol == Rol.BARBERO;
		this.creadoEn = Instant.now();
	}

	public static Usuario admin(String nombre, String email, String passwordHash) {
		return new Usuario(null, nombre, email, passwordHash, Rol.ADMIN);
	}

	public static Usuario miembro(Negocio negocio, String nombre, String email, String passwordHash, Rol rol) {
		if (negocio == null || !rol.esDeNegocio()) {
			throw new IllegalArgumentException("Un miembro necesita negocio y un rol de negocio");
		}
		return new Usuario(negocio, nombre, email, passwordHash, rol);
	}

	public void cambiarPassword(String nuevoHash) {
		this.passwordHash = nuevoHash;
	}

	public void darDeBaja() {
		this.activo = false;
	}

	public void reactivar() {
		this.activo = true;
	}

	public void cambiarAtiende(boolean atiende) {
		if (atiende && !rol.esDeNegocio()) {
			throw new IllegalStateException("Un usuario de CHI2 no atiende clientes");
		}
		this.atiende = atiende;
	}

	public boolean isAtiende() {
		return atiende;
	}

	public Long getId() {
		return id;
	}

	public Negocio getNegocio() {
		return negocio;
	}

	public String getNombre() {
		return nombre;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public Rol getRol() {
		return rol;
	}

	public boolean isActivo() {
		return activo;
	}

	public Instant getCreadoEn() {
		return creadoEn;
	}

}
