package com.chi2.peluqueria.seguridad;

import java.util.Collection;
import java.util.List;

import com.chi2.peluqueria.negocio.Negocio;
import com.chi2.peluqueria.usuario.Rol;
import com.chi2.peluqueria.usuario.Usuario;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * El usuario con sesión iniciada. Los controladores lo reciben con {@code @AuthenticationPrincipal}
 * y usan {@link #getNegocioId()} para filtrar todos los datos de la barbería.
 * Las vistas lo reciben como {@code usuario}.
 */
public final class UsuarioAutenticado implements UserDetails, CredentialsContainer {

	private static final long serialVersionUID = 1L;

	private final Long id;
	private final Long negocioId;
	private final String negocioNombre;
	private final String nombre;
	private final String email;
	private final Rol rol;
	private final boolean habilitado;
	private String passwordHash;

	private UsuarioAutenticado(Long id, Long negocioId, String negocioNombre, String nombre, String email,
			String passwordHash, Rol rol, boolean habilitado) {
		this.id = id;
		this.negocioId = negocioId;
		this.negocioNombre = negocioNombre;
		this.nombre = nombre;
		this.email = email;
		this.passwordHash = passwordHash;
		this.rol = rol;
		this.habilitado = habilitado;
	}

	public static UsuarioAutenticado de(Usuario usuario) {
		Negocio negocio = usuario.getNegocio();
		boolean negocioActivo = negocio == null || negocio.isActivo();
		return new UsuarioAutenticado(
				usuario.getId(),
				negocio == null ? null : negocio.getId(),
				negocio == null ? null : negocio.getNombre(),
				usuario.getNombre(),
				usuario.getEmail(),
				usuario.getPasswordHash(),
				usuario.getRol(),
				usuario.isActivo() && negocioActivo);
	}

	public Long getId() {
		return id;
	}

	/** {@code null} para los usuarios de CHI2. */
	public Long getNegocioId() {
		return negocioId;
	}

	public String getNegocioNombre() {
		return negocioNombre;
	}

	public String getNombre() {
		return nombre;
	}

	public String getEmail() {
		return email;
	}

	public Rol getRol() {
		return rol;
	}

	public boolean isAdmin() {
		return rol == Rol.ADMIN;
	}

	public boolean isDueno() {
		return rol == Rol.DUENO;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public void eraseCredentials() {
		this.passwordHash = null;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	/** {@code false} si el usuario fue dado de baja o su barbería está suspendida. */
	@Override
	public boolean isEnabled() {
		return habilitado;
	}

}
