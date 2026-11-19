package com.chi2.peluqueria;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base de los tests de punta a punta: app completa, Postgres real (Testcontainers) y MockMvc.
 * Todos los tests usan esta misma configuración, así comparten un solo contexto y un solo contenedor.
 * Cada test corre en una transacción que se deshace al terminar.
 */
@SpringBootTest(properties = {
		"chi2.seguridad.admin-email=" + PruebaIntegracion.ADMIN_EMAIL,
		"chi2.seguridad.admin-contrasena=" + PruebaIntegracion.ADMIN_CONTRASENA })
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, DatosDePrueba.class })
@Transactional
public abstract class PruebaIntegracion {

	public static final String ADMIN_EMAIL = "admin@chi2.test";
	public static final String ADMIN_CONTRASENA = "admin-de-prueba";

	@Autowired
	protected MockMvc mvc;

	@Autowired
	protected DatosDePrueba datos;

}
