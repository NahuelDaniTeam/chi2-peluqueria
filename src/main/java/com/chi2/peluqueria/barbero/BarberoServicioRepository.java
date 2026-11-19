package com.chi2.peluqueria.barbero;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/** Siempre filtrado por negocio (CLAUDE.md §4). */
public interface BarberoServicioRepository extends JpaRepository<BarberoServicio, Long> {

	List<BarberoServicio> findAllByNegocioIdAndUsuarioId(Long negocioId, Long usuarioId);

	boolean existsByNegocioIdAndUsuarioId(Long negocioId, Long usuarioId);

	boolean existsByUsuarioIdAndServicioId(Long usuarioId, Long servicioId);

}
