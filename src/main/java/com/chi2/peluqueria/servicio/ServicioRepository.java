package com.chi2.peluqueria.servicio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Siempre filtrado por negocio (CLAUDE.md §4). */
public interface ServicioRepository extends JpaRepository<Servicio, Long> {

	List<Servicio> findAllByNegocioIdAndActivoOrderByNombreAsc(Long negocioId, boolean activo);

	long countByNegocioIdAndActivoTrue(Long negocioId);

	Optional<Servicio> findByIdAndNegocioId(Long id, Long negocioId);

	boolean existsByNegocioIdAndActivoTrueAndNombreIgnoreCase(Long negocioId, String nombre);

	boolean existsByNegocioIdAndActivoTrueAndNombreIgnoreCaseAndIdNot(Long negocioId, String nombre, Long id);

}
