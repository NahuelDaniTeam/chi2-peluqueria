package com.chi2.peluqueria.usuario;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	/** Para el login: trae también la barbería, para saber si está suspendida. */
	@Query("select u from Usuario u left join fetch u.negocio where u.email = :email")
	Optional<Usuario> buscarParaLogin(@Param("email") String emailNormalizado);

	boolean existsByEmail(String emailNormalizado);

	boolean existsByRol(Rol rol);

	// Datos de una barbería: siempre filtrados por negocio (CLAUDE.md §4)

	List<Usuario> findAllByNegocioId(Long negocioId);

	Optional<Usuario> findByIdAndNegocioId(Long id, Long negocioId);

	/** Los que aparecen en la agenda: activos y que atienden. */
	List<Usuario> findAllByNegocioIdAndActivoTrueAndAtiendeTrueOrderByNombreAsc(Long negocioId);

}
