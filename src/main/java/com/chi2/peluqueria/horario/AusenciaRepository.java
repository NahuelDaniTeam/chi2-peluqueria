package com.chi2.peluqueria.horario;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Siempre filtrado por negocio (CLAUDE.md §4). Sin usuario = cierre del local. */
public interface AusenciaRepository extends JpaRepository<Ausencia, Long> {

	List<Ausencia> findAllByNegocioIdAndUsuarioIdIsNullAndHastaGreaterThanEqualOrderByDesdeAsc(Long negocioId, LocalDate hoy);

	List<Ausencia> findAllByNegocioIdAndUsuarioIdAndHastaGreaterThanEqualOrderByDesdeAsc(Long negocioId, Long usuarioId,
			LocalDate hoy);

	Optional<Ausencia> findByIdAndNegocioId(Long id, Long negocioId);

}
