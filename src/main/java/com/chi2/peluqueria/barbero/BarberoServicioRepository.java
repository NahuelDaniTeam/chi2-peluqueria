package com.chi2.peluqueria.barbero;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Siempre filtrado por negocio (CLAUDE.md §4). */
public interface BarberoServicioRepository extends JpaRepository<BarberoServicio, Long> {

	List<BarberoServicio> findAllByNegocioIdAndUsuarioId(Long negocioId, Long usuarioId);

	boolean existsByNegocioIdAndUsuarioId(Long negocioId, Long usuarioId);

	boolean existsByUsuarioIdAndServicioId(Long usuarioId, Long servicioId);

	List<BarberoServicio> findAllByNegocioId(Long negocioId);

	/** Si ese barbero hace ese servicio, con su precio especial si tiene. */
	Optional<BarberoServicio> findByNegocioIdAndUsuarioIdAndServicioId(Long negocioId, Long usuarioId, Long servicioId);

	/** Quiénes hacen ese servicio. */
	List<BarberoServicio> findAllByNegocioIdAndServicioId(Long negocioId, Long servicioId);

}
