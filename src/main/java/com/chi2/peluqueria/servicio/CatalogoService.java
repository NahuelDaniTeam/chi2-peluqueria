package com.chi2.peluqueria.servicio;

import java.util.List;

import com.chi2.peluqueria.comun.DatoNoDisponibleException;
import com.chi2.peluqueria.comun.NoEncontradoException;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** El catálogo de servicios de una barbería. Siempre recibe el negocio del usuario con sesión iniciada. */
@Service
@Transactional
public class CatalogoService {

	/** Se publica al crear un servicio, para asignárselo a los barberos (ver barbero.PerfilBarberoService). */
	public record ServicioCreado(Long negocioId, Long servicioId) {
	}

	private final ServicioRepository servicios;
	private final ApplicationEventPublisher eventos;

	CatalogoService(ServicioRepository servicios, ApplicationEventPublisher eventos) {
		this.servicios = servicios;
		this.eventos = eventos;
	}

	@Transactional(readOnly = true)
	public List<Servicio> activos(Long negocioId) {
		return servicios.findAllByNegocioIdAndActivoOrderByNombreAsc(negocioId, true);
	}

	@Transactional(readOnly = true)
	public List<Servicio> quitados(Long negocioId) {
		return servicios.findAllByNegocioIdAndActivoOrderByNombreAsc(negocioId, false);
	}

	@Transactional(readOnly = true)
	public Servicio buscar(Long negocioId, Long servicioId) {
		return servicios.findByIdAndNegocioId(servicioId, negocioId).orElseThrow(NoEncontradoException::new);
	}

	/** El precio ya viene leído y validado por el controlador. */
	public Servicio crear(Long negocioId, String nombre, int duracionMinutos, int precio) {
		if (servicios.existsByNegocioIdAndActivoTrueAndNombreIgnoreCase(negocioId, nombre.trim())) {
			throw new DatoNoDisponibleException("nombre", "Ya hay un servicio con ese nombre.");
		}
		Servicio servicio = servicios.save(new Servicio(negocioId, nombre, duracionMinutos, precio));
		eventos.publishEvent(new ServicioCreado(negocioId, servicio.getId()));
		return servicio;
	}

	public Servicio actualizar(Long negocioId, Long servicioId, String nombre, int duracionMinutos, int precio) {
		Servicio servicio = buscar(negocioId, servicioId);
		if (servicios.existsByNegocioIdAndActivoTrueAndNombreIgnoreCaseAndIdNot(negocioId, nombre.trim(), servicioId)) {
			throw new DatoNoDisponibleException("nombre", "Ya hay un servicio con ese nombre.");
		}
		servicio.actualizar(nombre, duracionMinutos, precio);
		return servicio;
	}

	public Servicio quitar(Long negocioId, Long servicioId) {
		Servicio servicio = buscar(negocioId, servicioId);
		servicio.quitar();
		return servicio;
	}

	public Servicio reactivar(Long negocioId, Long servicioId) {
		Servicio servicio = buscar(negocioId, servicioId);
		if (servicios.existsByNegocioIdAndActivoTrueAndNombreIgnoreCaseAndIdNot(negocioId, servicio.getNombre(), servicioId)) {
			throw new DatoNoDisponibleException("nombre",
					"Ya hay otro servicio llamado " + servicio.getNombre() + ". Cambiale el nombre a uno de los dos.");
		}
		servicio.reactivar();
		return servicio;
	}

}
