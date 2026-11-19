package com.chi2.peluqueria.usuario;

import java.util.Comparator;
import java.util.List;

import com.chi2.peluqueria.comun.DatoNoDisponibleException;
import com.chi2.peluqueria.comun.NoEncontradoException;
import com.chi2.peluqueria.comun.OperacionNoPermitidaException;
import com.chi2.peluqueria.negocio.NegocioRepository;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El equipo de una barbería (dueños y barberos). Todos los métodos reciben el negocio del usuario
 * con sesión iniciada y nunca tocan usuarios de otro negocio.
 */
@Service
@Transactional
public class EquipoService {

	private static final Comparator<Usuario> POR_ROL_Y_NOMBRE = Comparator
			.comparing((Usuario u) -> u.getRol() == Rol.DUENO ? 0 : 1)
			.thenComparing(Usuario::getNombre, String.CASE_INSENSITIVE_ORDER);

	/** Se publica al sumar a alguien, para asignarle los servicios si atiende (ver barbero.PerfilBarberoService). */
	public record MiembroAgregado(Long negocioId, Long usuarioId) {
	}

	private final UsuarioRepository usuarios;
	private final NegocioRepository negocios;
	private final PasswordEncoder passwordEncoder;
	private final ApplicationEventPublisher eventos;

	EquipoService(UsuarioRepository usuarios, NegocioRepository negocios, PasswordEncoder passwordEncoder,
			ApplicationEventPublisher eventos) {
		this.usuarios = usuarios;
		this.negocios = negocios;
		this.passwordEncoder = passwordEncoder;
		this.eventos = eventos;
	}

	/** Un miembro de la barbería (404 si no existe o es de otra barbería). */
	@Transactional(readOnly = true)
	public Usuario buscar(Long negocioId, Long usuarioId) {
		return usuarios.findByIdAndNegocioId(usuarioId, negocioId).orElseThrow(NoEncontradoException::new);
	}

	@Transactional(readOnly = true)
	public List<Usuario> listar(Long negocioId) {
		return usuarios.findAllByNegocioId(negocioId).stream().sorted(POR_ROL_Y_NOMBRE).toList();
	}

	public Usuario agregar(Long negocioId, NuevoMiembroForm form) {
		if (form.getRol() == null || !form.getRol().esDeNegocio()) {
			throw new OperacionNoPermitidaException("Rol no permitido en una barbería: " + form.getRol());
		}
		String email = Emails.normalizar(form.getEmail());
		if (usuarios.existsByEmail(email)) {
			throw new DatoNoDisponibleException("email", "Ya hay una cuenta con ese email.");
		}
		Usuario nuevo = usuarios.save(Usuario.miembro(negocios.getReferenceById(negocioId), form.getNombre(), email,
				passwordEncoder.encode(form.getContrasena()), form.getRol()));
		eventos.publishEvent(new MiembroAgregado(negocioId, nuevo.getId()));
		return nuevo;
	}

	/** Da de baja a alguien del equipo: no puede volver a entrar hasta que lo reactiven. */
	public Usuario darDeBaja(Long negocioId, Long usuarioId, Long quienLoPide) {
		if (usuarioId.equals(quienLoPide)) {
			throw new OperacionNoPermitidaException("Nadie puede darse de baja a sí mismo");
		}
		Usuario usuario = buscar(negocioId, usuarioId);
		usuario.darDeBaja();
		return usuario;
	}

	public Usuario reactivar(Long negocioId, Long usuarioId) {
		Usuario usuario = buscar(negocioId, usuarioId);
		usuario.reactivar();
		return usuario;
	}

}
