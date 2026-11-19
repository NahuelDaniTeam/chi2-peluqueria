package com.chi2.peluqueria.usuario;

import com.chi2.peluqueria.comun.NoEncontradoException;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lo que cada usuario puede hacer con su propia cuenta. */
@Service
@Transactional
public class CuentaService {

	private final UsuarioRepository usuarios;
	private final PasswordEncoder passwordEncoder;

	CuentaService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder) {
		this.usuarios = usuarios;
		this.passwordEncoder = passwordEncoder;
	}

	/** @return {@code false} si la contraseña actual no coincide. */
	public boolean cambiarContrasena(Long usuarioId, String actual, String nueva) {
		Usuario usuario = usuarios.findById(usuarioId).orElseThrow(NoEncontradoException::new);
		if (!passwordEncoder.matches(actual, usuario.getPasswordHash())) {
			return false;
		}
		usuario.cambiarPassword(passwordEncoder.encode(nueva));
		return true;
	}

}
