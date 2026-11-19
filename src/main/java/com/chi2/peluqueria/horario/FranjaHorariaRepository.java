package com.chi2.peluqueria.horario;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/** Siempre filtrado por negocio (CLAUDE.md §4). Sin usuario = horario del local. */
public interface FranjaHorariaRepository extends JpaRepository<FranjaHoraria, Long> {

	List<FranjaHoraria> findAllByNegocioIdAndUsuarioIdIsNullOrderByDiaSemanaAscDesdeAsc(Long negocioId);

	/** Todas: las del local y las de cada barbero (para calcular la disponibilidad). */
	List<FranjaHoraria> findAllByNegocioId(Long negocioId);

	List<FranjaHoraria> findAllByNegocioIdAndUsuarioIdOrderByDiaSemanaAscDesdeAsc(Long negocioId, Long usuarioId);

	boolean existsByNegocioIdAndUsuarioIdIsNull(Long negocioId);

	boolean existsByNegocioIdAndUsuarioId(Long negocioId, Long usuarioId);

	void deleteAllByNegocioIdAndUsuarioIdIsNull(Long negocioId);

	void deleteAllByNegocioIdAndUsuarioId(Long negocioId, Long usuarioId);

}
