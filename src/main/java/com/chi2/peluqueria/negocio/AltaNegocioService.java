package com.chi2.peluqueria.negocio;

import java.util.List;

import com.chi2.peluqueria.comun.DatoNoDisponibleException;
import com.chi2.peluqueria.comun.NoEncontradoException;
import com.chi2.peluqueria.usuario.Emails;
import com.chi2.peluqueria.usuario.Rol;
import com.chi2.peluqueria.usuario.Usuario;
import com.chi2.peluqueria.usuario.UsuarioRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Lo que hace CHI2 con las barberías: darlas de alta, suspenderlas y reactivarlas. */
@Service
@Transactional
public class AltaNegocioService {

	private final NegocioRepository negocios;
	private final UsuarioRepository usuarios;
	private final PasswordEncoder passwordEncoder;

	AltaNegocioService(NegocioRepository negocios, UsuarioRepository usuarios, PasswordEncoder passwordEncoder) {
		this.negocios = negocios;
		this.usuarios = usuarios;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional(readOnly = true)
	public List<Negocio> listar() {
		return negocios.findAllByOrderByNombreAsc();
	}

	/** Crea la barbería y su primer dueño, todo junto o nada. */
	public Negocio crear(NuevoNegocioForm form) {
		String slug = Slugs.desde(StringUtils.hasText(form.getSlug()) ? form.getSlug() : form.getNombre());
		if (slug.isEmpty() || Slugs.esReservado(slug)) {
			throw new DatoNoDisponibleException("slug", "Elegí otro link: ese no se puede usar.");
		}
		if (negocios.existsBySlug(slug)) {
			throw new DatoNoDisponibleException("slug", "Ya hay una barbería con el link " + slug + ".");
		}
		String email = Emails.normalizar(form.getDuenoEmail());
		if (usuarios.existsByEmail(email)) {
			throw new DatoNoDisponibleException("duenoEmail", "Ya hay una cuenta con ese email.");
		}
		Negocio negocio = negocios.save(new Negocio(form.getNombre().trim(), slug));
		usuarios.save(Usuario.miembro(negocio, form.getDuenoNombre(), email,
				passwordEncoder.encode(form.getDuenoContrasena()), Rol.DUENO));
		return negocio;
	}

	/** Nadie de la barbería puede entrar mientras esté suspendida (ej. falta de pago). */
	public Negocio suspender(Long negocioId) {
		Negocio negocio = negocios.findById(negocioId).orElseThrow(NoEncontradoException::new);
		negocio.suspender();
		return negocio;
	}

	public Negocio reactivar(Long negocioId) {
		Negocio negocio = negocios.findById(negocioId).orElseThrow(NoEncontradoException::new);
		negocio.reactivar();
		return negocio;
	}

}
