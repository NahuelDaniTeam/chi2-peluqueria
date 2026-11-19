package com.chi2.peluqueria.seguridad;

import com.chi2.peluqueria.usuario.Emails;
import com.chi2.peluqueria.usuario.Rol;
import com.chi2.peluqueria.usuario.Usuario;
import com.chi2.peluqueria.usuario.UsuarioRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Al arrancar, crea el usuario de CHI2 configurado en {@code chi2.seguridad.admin-*} si todavía no existe.
 * Es la única forma de crear el primer usuario: con él se dan de alta las barberías.
 */
@Component
class AdminInicial implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminInicial.class);

	private final UsuarioRepository usuarios;
	private final PasswordEncoder passwordEncoder;
	private final SeguridadProperties propiedades;

	AdminInicial(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, SeguridadProperties propiedades) {
		this.usuarios = usuarios;
		this.passwordEncoder = passwordEncoder;
		this.propiedades = propiedades;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		String email = Emails.normalizar(propiedades.adminEmail());
		if (!StringUtils.hasText(email) || !StringUtils.hasText(propiedades.adminContrasena())) {
			if (!usuarios.existsByRol(Rol.ADMIN)) {
				log.warn("No hay usuario de CHI2. Configurá CHI2_ADMIN_EMAIL y CHI2_ADMIN_CONTRASENA para crearlo.");
			}
			return;
		}
		if (!usuarios.existsByEmail(email)) {
			usuarios.save(Usuario.admin("CHI2", email, passwordEncoder.encode(propiedades.adminContrasena())));
			log.info("Usuario de CHI2 creado: {}", email);
		}
	}

}
