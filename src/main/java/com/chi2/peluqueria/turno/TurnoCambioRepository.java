package com.chi2.peluqueria.turno;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Siempre filtrado por negocio (CLAUDE.md §4). */
public interface TurnoCambioRepository extends JpaRepository<TurnoCambio, Long> {

	@Query("""
			select c from TurnoCambio c left join fetch c.usuario
			where c.negocioId = :negocioId and c.turnoId = :turnoId
			order by c.creadoEn, c.id""")
	List<TurnoCambio> historial(@Param("negocioId") Long negocioId, @Param("turnoId") Long turnoId);

}
