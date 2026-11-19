package com.chi2.peluqueria.negocio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NegocioRepository extends JpaRepository<Negocio, Long> {

	boolean existsBySlug(String slug);

	List<Negocio> findAllByOrderByNombreAsc();

}
