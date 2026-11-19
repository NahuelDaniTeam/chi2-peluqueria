package com.chi2.peluqueria.seguridad;

import com.chi2.peluqueria.usuario.Emails;
import com.chi2.peluqueria.usuario.UsuarioRepository;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class UsuarioDetailsService implements UserDetailsService {

	private final UsuarioRepository usuarios;

	UsuarioDetailsService(UsuarioRepository usuarios) {
		this.usuarios = usuarios;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String email) {
		return usuarios.buscarParaLogin(Emails.normalizar(email))
				.map(UsuarioAutenticado::de)
				.orElseThrow(() -> new UsernameNotFoundException("Usuario inexistente"));
	}

}
