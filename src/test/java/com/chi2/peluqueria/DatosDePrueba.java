package com.chi2.peluqueria;

import java.util.concurrent.atomic.AtomicInteger;

import com.chi2.peluqueria.negocio.Negocio;
import com.chi2.peluqueria.negocio.NegocioRepository;
import com.chi2.peluqueria.negocio.Slugs;
import com.chi2.peluqueria.seguridad.UsuarioAutenticado;
import com.chi2.peluqueria.usuario.Rol;
import com.chi2.peluqueria.usuario.Usuario;
import com.chi2.peluqueria.usuario.UsuarioRepository;

import org.springframework.security.crypto.password.PasswordEncoder;

/** Arma barberías y usuarios para los tests, con emails y links que no se repiten. */
public class DatosDePrueba {

	public static final String CONTRASENA = "clave-de-prueba";

	private static final AtomicInteger SECUENCIA = new AtomicInteger();

	private final NegocioRepository negocios;
	private final UsuarioRepository usuarios;
	private final PasswordEncoder passwordEncoder;
	private String contrasenaCodificada;

	DatosDePrueba(NegocioRepository negocios, UsuarioRepository usuarios, PasswordEncoder passwordEncoder) {
		this.negocios = negocios;
		this.usuarios = usuarios;
		this.passwordEncoder = passwordEncoder;
	}

	public Negocio negocio(String nombre) {
		return negocios.save(new Negocio(nombre, Slugs.desde(nombre) + "-" + SECUENCIA.incrementAndGet()));
	}

	public Usuario dueno(Negocio negocio, String nombre) {
		return miembro(negocio, nombre, Rol.DUENO);
	}

	public Usuario barbero(Negocio negocio, String nombre) {
		return miembro(negocio, nombre, Rol.BARBERO);
	}

	/** Lo que usa {@code with(user(...))} para simular una sesión iniciada. */
	public UsuarioAutenticado sesion(Usuario usuario) {
		return UsuarioAutenticado.de(usuario);
	}

	private Usuario miembro(Negocio negocio, String nombre, Rol rol) {
		String email = Slugs.desde(nombre) + "-" + SECUENCIA.incrementAndGet() + "@prueba.test";
		return usuarios.save(Usuario.miembro(negocio, nombre, email, contrasenaCodificada(), rol));
	}

	// bcrypt es lento a propósito: se codifica una sola vez
	private String contrasenaCodificada() {
		if (contrasenaCodificada == null) {
			contrasenaCodificada = passwordEncoder.encode(CONTRASENA);
		}
		return contrasenaCodificada;
	}

}
