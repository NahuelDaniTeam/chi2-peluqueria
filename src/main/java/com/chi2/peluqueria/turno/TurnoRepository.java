package com.chi2.peluqueria.turno;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Siempre filtrado por negocio (CLAUDE.md §4). Trae barbero y servicio, que la agenda siempre muestra. */
public interface TurnoRepository extends JpaRepository<Turno, Long> {

	@Query("""
			select t from Turno t join fetch t.barbero join fetch t.servicio
			where t.id = :id and t.negocioId = :negocioId""")
	Optional<Turno> buscar(@Param("negocioId") Long negocioId, @Param("id") Long id);

	/** Los que empiezan en [desde, hasta), de todos los barberos o de uno ({@code barberoId} nulo = todos). */
	@Query("""
			select t from Turno t join fetch t.barbero join fetch t.servicio
			where t.negocioId = :negocioId and (:barberoId is null or t.barbero.id = :barberoId)
			  and t.inicio >= :desde and t.inicio < :hasta
			order by t.inicio, t.barbero.nombre""")
	List<Turno> queEmpiezanEntre(@Param("negocioId") Long negocioId, @Param("barberoId") Long barberoId,
			@Param("desde") Instant desde, @Param("hasta") Instant hasta);

	/** Los de esos estados que empiezan en [desde, hasta), del más nuevo al más viejo ({@code barberoId} nulo = todos). */
	@Query("""
			select t from Turno t join fetch t.barbero join fetch t.servicio
			where t.negocioId = :negocioId and (:barberoId is null or t.barbero.id = :barberoId)
			  and t.estado in :estados and t.inicio >= :desde and t.inicio < :hasta
			order by t.inicio desc, t.barbero.nombre""")
	List<Turno> conEstadoEntre(@Param("negocioId") Long negocioId, @Param("barberoId") Long barberoId,
			@Param("estados") Collection<EstadoTurno> estados, @Param("desde") Instant desde,
			@Param("hasta") Instant hasta);

	/** Los que ocupan lugar y se superponen con [desde, hasta), de esos barberos. */
	@Query("""
			select t from Turno t
			where t.negocioId = :negocioId and t.barbero.id in :barberos and t.estado in :estados
			  and t.inicio < :hasta and t.fin > :desde""")
	List<Turno> ocupados(@Param("negocioId") Long negocioId, @Param("barberos") Collection<Long> barberos,
			@Param("estados") Collection<EstadoTurno> estados, @Param("desde") Instant desde,
			@Param("hasta") Instant hasta);

	/** Para saber si a alguien le quedan turnos reservados (de {@code ahora} en adelante). */
	long countByNegocioIdAndBarbero_IdAndEstadoAndFinAfter(Long negocioId, Long barberoId, EstadoTurno estado,
			Instant ahora);

	/** Reservados de un barbero (o de todos, con {@code barberoId} nulo) que se superponen con [desde, hasta). */
	@Query("""
			select count(t) from Turno t
			where t.negocioId = :negocioId and (:barberoId is null or t.barbero.id = :barberoId)
			  and t.estado = com.chi2.peluqueria.turno.EstadoTurno.RESERVADO
			  and t.inicio < :hasta and t.fin > :desde""")
	long contarReservados(@Param("negocioId") Long negocioId, @Param("barberoId") Long barberoId,
			@Param("desde") Instant desde, @Param("hasta") Instant hasta);

}
