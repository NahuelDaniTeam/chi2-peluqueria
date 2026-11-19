package com.chi2.peluqueria.servicio;

import com.chi2.peluqueria.PruebaIntegracion;
import com.chi2.peluqueria.negocio.Negocio;
import com.chi2.peluqueria.usuario.Usuario;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ServiciosTest extends PruebaIntegracion {

	@Autowired
	ServicioRepository servicios;

	@Test
	void elDuenoCargaUnServicioConDuracionYPrecio() throws Exception {
		Negocio norte = datos.negocio("Barbería Norte");
		Usuario ana = datos.dueno(norte, "Ana");

		mvc.perform(post("/barberia/servicios").with(user(datos.sesion(ana))).with(csrf())
						.param("nombre", "Corte + tintura")
						.param("duracionMinutos", "120")
						.param("precio", "$ 25.000"))
				.andExpect(redirectedUrl("/barberia/servicios"));

		assertThat(servicios.findAllByNegocioIdAndActivoOrderByNombreAsc(norte.getId(), true))
				.singleElement()
				.satisfies(s -> {
					assertThat(s.getNombre()).isEqualTo("Corte + tintura");
					assertThat(s.getDuracionMinutos()).isEqualTo(120);
					assertThat(s.getPrecio()).isEqualTo(25000);
				});
		mvc.perform(get("/barberia/servicios").with(user(datos.sesion(ana))))
				.andExpect(content().string(containsString("2 h")))
				.andExpect(content().string(containsString("$ 25.000")));
	}

	@Test
	void validaPrecioYDuracion() throws Exception {
		Usuario ana = datos.dueno(datos.negocio("Barbería Norte"), "Ana");

		mvc.perform(post("/barberia/servicios").with(user(datos.sesion(ana))).with(csrf())
						.param("nombre", "Corte")
						.param("duracionMinutos", "33")
						.param("precio", "12000,50"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Elegí una duración de la lista.")))
				.andExpect(content().string(containsString("sin centavos")));
	}

	@Test
	void noSeRepiteElNombreDentroDeLaBarberia() throws Exception {
		Negocio norte = datos.negocio("Barbería Norte");
		Usuario ana = datos.dueno(norte, "Ana");
		datos.servicio(norte, "Corte", 30, 12000);

		mvc.perform(post("/barberia/servicios").with(user(datos.sesion(ana))).with(csrf())
						.param("nombre", "  corte ")
						.param("duracionMinutos", "30")
						.param("precio", "13000"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Ya hay un servicio con ese nombre.")));
	}

	@Test
	void otraBarberiaPuedeTenerUnServicioConElMismoNombre() throws Exception {
		datos.servicio(datos.negocio("Barbería Sur"), "Corte", 30, 9000);
		Usuario ana = datos.dueno(datos.negocio("Barbería Norte"), "Ana");

		mvc.perform(post("/barberia/servicios").with(user(datos.sesion(ana))).with(csrf())
						.param("nombre", "Corte")
						.param("duracionMinutos", "30")
						.param("precio", "12000"))
				.andExpect(redirectedUrl("/barberia/servicios"));
	}

	@Test
	void editarQuitarYVolverAOfrecer() throws Exception {
		Negocio norte = datos.negocio("Barbería Norte");
		Usuario ana = datos.dueno(norte, "Ana");
		Servicio corte = datos.servicio(norte, "Corte", 30, 12000);

		mvc.perform(post("/barberia/servicios/{id}", corte.getId()).with(user(datos.sesion(ana))).with(csrf())
						.param("nombre", "Corte clásico")
						.param("duracionMinutos", "40")
						.param("precio", "14.000"))
				.andExpect(redirectedUrl("/barberia/servicios"));
		assertThat(corte.getNombre()).isEqualTo("Corte clásico");
		assertThat(corte.getDuracionMinutos()).isEqualTo(40);
		assertThat(corte.getPrecio()).isEqualTo(14000);

		mvc.perform(post("/barberia/servicios/{id}/quitar", corte.getId()).with(user(datos.sesion(ana))).with(csrf()))
				.andExpect(redirectedUrl("/barberia/servicios"));
		assertThat(corte.isActivo()).isFalse();

		mvc.perform(post("/barberia/servicios/{id}/reactivar", corte.getId()).with(user(datos.sesion(ana))).with(csrf()))
				.andExpect(redirectedUrl("/barberia/servicios"));
		assertThat(corte.isActivo()).isTrue();
	}

	@Test
	void unDuenoNoVeNiTocaServiciosDeOtraBarberia() throws Exception {
		Usuario ana = datos.dueno(datos.negocio("Barbería Norte"), "Ana");
		Servicio deSur = datos.servicio(datos.negocio("Barbería Sur"), "Corte navaja", 30, 9000);

		mvc.perform(get("/barberia/servicios").with(user(datos.sesion(ana))))
				.andExpect(content().string(not(containsString("Corte navaja"))));
		mvc.perform(get("/barberia/servicios/{id}", deSur.getId()).with(user(datos.sesion(ana))))
				.andExpect(status().isNotFound());
		mvc.perform(post("/barberia/servicios/{id}/quitar", deSur.getId()).with(user(datos.sesion(ana))).with(csrf()))
				.andExpect(status().isNotFound());

		assertThat(deSur.isActivo()).isTrue();
	}

	@Test
	void unBarberoNoEntraAlCatalogo() throws Exception {
		Usuario juan = datos.barbero(datos.negocio("Barbería Norte"), "Juan");

		mvc.perform(get("/barberia/servicios").with(user(datos.sesion(juan)))).andExpect(status().isForbidden());
		mvc.perform(get("/barberia").with(user(datos.sesion(juan)))).andExpect(status().isForbidden());
	}

}
