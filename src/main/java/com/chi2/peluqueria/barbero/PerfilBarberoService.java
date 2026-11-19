package com.chi2.peluqueria.barbero;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.chi2.peluqueria.comun.Formatos;
import com.chi2.peluqueria.servicio.CatalogoService;
import com.chi2.peluqueria.servicio.Servicio;
import com.chi2.peluqueria.servicio.ServicioRepository;
import com.chi2.peluqueria.usuario.EquipoService;
import com.chi2.peluqueria.usuario.Usuario;
import com.chi2.peluqueria.usuario.UsuarioRepository;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El perfil de cada miembro que atiende: si atiende y qué servicios hace (con precio especial).
 * Todo filtrado por el negocio del usuario con sesión iniciada.
 */
@Service
@Transactional
public class PerfilBarberoService {

	/** Un servicio del catálogo visto desde un barbero. */
	public record ServicioDelBarbero(Servicio servicio, boolean hace, Integer precioEspecial) {

		/** Lo que cobra este barbero: su precio especial o el del local. */
		public int precio() {
			return precioEspecial != null ? precioEspecial : servicio.getPrecio();
		}

		public String getPrecioTexto() {
			return Formatos.precio(precio());
		}

	}

	private final EquipoService equipo;
	private final UsuarioRepository usuarios;
	private final ServicioRepository servicios;
	private final BarberoServicioRepository barberoServicios;

	PerfilBarberoService(EquipoService equipo, UsuarioRepository usuarios, ServicioRepository servicios,
			BarberoServicioRepository barberoServicios) {
		this.equipo = equipo;
		this.usuarios = usuarios;
		this.servicios = servicios;
		this.barberoServicios = barberoServicios;
	}

	/** Todos los servicios activos del local, marcando cuáles hace este barbero. */
	@Transactional(readOnly = true)
	public List<ServicioDelBarbero> servicios(Long negocioId, Long usuarioId) {
		equipo.buscar(negocioId, usuarioId);
		Map<Long, BarberoServicio> queHace = barberoServicios.findAllByNegocioIdAndUsuarioId(negocioId, usuarioId)
				.stream().collect(Collectors.toMap(BarberoServicio::getServicioId, Function.identity()));
		return servicios.findAllByNegocioIdAndActivoOrderByNombreAsc(negocioId, true).stream()
				.map(s -> {
					BarberoServicio bs = queHace.get(s.getId());
					return new ServicioDelBarbero(s, bs != null, bs == null ? null : bs.getPrecioEspecial());
				})
				.toList();
	}

	public Usuario cambiarAtiende(Long negocioId, Long usuarioId, boolean atiende) {
		Usuario usuario = equipo.buscar(negocioId, usuarioId);
		usuario.cambiarAtiende(atiende);
		// Si empieza a atender y no tiene servicios, arranca haciendo todos (después se ajusta)
		if (atiende && !barberoServicios.existsByNegocioIdAndUsuarioId(negocioId, usuarioId)) {
			asignarTodos(negocioId, usuarioId);
		}
		return usuario;
	}

	/**
	 * Guarda qué servicios hace y a qué precio.
	 *
	 * @param elegidos servicioId → precio especial ({@code null} = el del local). Los servicios que no
	 *                 están en el mapa no los hace. Ids de otra barbería se ignoran.
	 */
	public void guardarServicios(Long negocioId, Long usuarioId, Map<Long, Integer> elegidos) {
		equipo.buscar(negocioId, usuarioId);
		Map<Long, BarberoServicio> actuales = barberoServicios.findAllByNegocioIdAndUsuarioId(negocioId, usuarioId)
				.stream().collect(Collectors.toMap(BarberoServicio::getServicioId, Function.identity()));
		for (Servicio servicio : servicios.findAllByNegocioIdAndActivoOrderByNombreAsc(negocioId, true)) {
			BarberoServicio actual = actuales.get(servicio.getId());
			if (!elegidos.containsKey(servicio.getId())) {
				if (actual != null) {
					barberoServicios.delete(actual);
				}
				continue;
			}
			Integer precio = elegidos.get(servicio.getId());
			// Igual al del local = no es especial
			Integer especial = precio != null && precio != servicio.getPrecio() ? precio : null;
			BarberoServicio fila = actual != null ? actual
					: barberoServicios.save(new BarberoServicio(negocioId, usuarioId, servicio.getId()));
			fila.cambiarPrecioEspecial(especial);
		}
	}

	/** Un servicio nuevo lo hacen, de entrada, todos los que atienden. */
	@EventListener
	void alCrearServicio(CatalogoService.ServicioCreado evento) {
		for (Usuario barbero : usuarios.findAllByNegocioIdAndActivoTrueAndAtiendeTrueOrderByNombreAsc(evento.negocioId())) {
			if (!barberoServicios.existsByUsuarioIdAndServicioId(barbero.getId(), evento.servicioId())) {
				barberoServicios.save(new BarberoServicio(evento.negocioId(), barbero.getId(), evento.servicioId()));
			}
		}
	}

	/** Alguien nuevo que atiende hace, de entrada, todos los servicios. */
	@EventListener
	void alAgregarMiembro(EquipoService.MiembroAgregado evento) {
		Usuario nuevo = equipo.buscar(evento.negocioId(), evento.usuarioId());
		if (nuevo.isAtiende()) {
			asignarTodos(evento.negocioId(), evento.usuarioId());
		}
	}

	private void asignarTodos(Long negocioId, Long usuarioId) {
		for (Servicio servicio : servicios.findAllByNegocioIdAndActivoOrderByNombreAsc(negocioId, true)) {
			if (!barberoServicios.existsByUsuarioIdAndServicioId(usuarioId, servicio.getId())) {
				barberoServicios.save(new BarberoServicio(negocioId, usuarioId, servicio.getId()));
			}
		}
	}

}
