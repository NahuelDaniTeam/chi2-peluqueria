package com.chi2.peluqueria.barbero;

import com.chi2.peluqueria.PruebaIntegracion;
import com.chi2.peluqueria.negocio.Negocio;
import com.chi2.peluqueria.servicio.Servicio;
import com.chi2.peluqueria.usuario.Usuario;
import com.chi2.peluqueria.usuario.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PerfilBarberoTest extends PruebaIntegracion {

	@Autowired
	BarberoServicioRepository barberoServicios;

	@Autowired
	UsuarioRepository usuarios;

	@Test
	void unServicioNuevoLoHacenTodosLosQueAtienden() throws Exception {
		Negocio norte = datos.negocio("Barbería Norte");
		Usuario ana = datos.dueno(norte, "Ana");
		Usuario juan = datos.barbero(norte, "Juan");

		mvc.perform(post("/barberia/servicios").with(user(datos.sesion(ana))).with(csrf())
						.param("nombre", "Corte").param("duracionMinutos", "30").param("precio", "12000"))
				.andExpect(redirectedUrl("/barberia/servicios"));

		assertThat(barberoServicios.findAllByNegocioIdAndUsuarioId(norte.getId(), juan.getId())).hasSize(1);
		// La dueña no atiende (todavía), así que no se le asigna
		assertThat(barberoServicios.findAllByNegocioIdAndUsuarioId(norte.getId(), ana.getId())).isEmpty();
	}

	@Test
	void unBarberoNuevoHaceTodosLosServicios() throws Exception {
		Negocio norte = datos.negocio("Barbería Norte");
		Usuario ana = datos.dueno(norte, "Ana");
		datos.servicio(norte, "Corte", 30, 12000);
		datos.servicio(norte, "Barba", 20, 8000);

		mvc.perform(post("/equipo").with(user(datos.sesion(ana))).with(csrf())
						.param("nombre", "Lucas").param("email", "lucas@norte.test")
						.param("contrasena", "clave-de-lucas").param("rol", "BARBERO"))
				.andExpect(redirectedUrl("/equipo"));

		Usuario lucas = usuarios.buscarParaLogin("lucas@norte.test").orElseThrow();
		assertThat(barberoServicios.findAllByNegocioIdAndUsuarioId(norte.getId(), lucas.getId())).hasSize(2);
	}

	@Test
	void elDuenoEligeQueHaceCadaBarberoYSuPrecioEspecial() throws Exception {
		Negocio norte = datos.negocio("Barbería Norte");
		Usuario ana = datos.dueno(norte, "Ana");
		Servicio corte = datos.servicio(norte, "Corte", 30, 12000);
		Servicio tintura = datos.servicio(norte, "Tintura", 90, 25000);
		Usuario juan = datos.barbero(norte, "Juan");

		mvc.perform(post("/equipo/{id}/servicios", juan.getId()).with(user(datos.sesion(ana))).with(csrf())
						.param("items[0].servicioId", corte.getId().toString())
						.param("items[0].hace", "true")
						.param("items[0].precioEspecial", "15.000")
						.param("items[1].servicioId", tintura.getId().toString())
						.param("items[1].precioEspecial", ""))
				.andExpect(redirectedUrl("/equipo/" + juan.getId()));

		assertThat(barberoServicios.findAllByNegocioIdAndUsuarioId(norte.getId(), juan.getId()))
				.extracting(BarberoServicio::getServicioId, BarberoServicio::getPrecioEspecial)
				.containsExactly(tuple(corte.getId(), 15000));
		mvc.perform(get("/equipo/{id}", juan.getId()).with(user(datos.sesion(ana))))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("15000")));
	}

	@Test
	void unPrecioEspecialInvalidoMuestraElError() throws Exception {
		Negocio norte = datos.negocio("Barbería Norte");
		Usuario ana = datos.dueno(norte, "Ana");
		Servicio corte = datos.servicio(norte, "Corte", 30, 12000);
		Usuario juan = datos.barbero(norte, "Juan");

		mvc.perform(post("/equipo/{id}/servicios", juan.getId()).with(user(datos.sesion(ana))).with(csrf())
						.param("items[0].servicioId", corte.getId().toString())
						.param("items[0].hace", "true")
						.param("items[0].precioEspecial", "quince mil"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("sin centavos")));
	}

	@Test
	void laDuenaPuedeMarcarQueTambienAtiendeYArrancaHaciendoTodo() throws Exception {
		Negocio norte = datos.negocio("Barbería Norte");
		Usuario ana = datos.dueno(norte, "Ana");
		datos.servicio(norte, "Corte", 30, 12000);

		mvc.perform(post("/equipo/{id}/atiende", ana.getId()).with(user(datos.sesion(ana))).with(csrf())
						.param("valor", "true"))
				.andExpect(redirectedUrl("/equipo/" + ana.getId()));

		assertThat(ana.isAtiende()).isTrue();
		assertThat(barberoServicios.findAllByNegocioIdAndUsuarioId(norte.getId(), ana.getId())).hasSize(1);
	}

	@Test
	void serviciosDeOtraBarberiaSeIgnoran() throws Exception {
		Negocio norte = datos.negocio("Barbería Norte");
		Usuario ana = datos.dueno(norte, "Ana");
		Usuario juan = datos.barbero(norte, "Juan");
		Servicio deSur = datos.servicio(datos.negocio("Barbería Sur"), "Corte", 30, 9000);

		mvc.perform(post("/equipo/{id}/servicios", juan.getId()).with(user(datos.sesion(ana))).with(csrf())
						.param("items[0].servicioId", deSur.getId().toString())
						.param("items[0].hace", "true"))
				.andExpect(redirectedUrl("/equipo/" + juan.getId()));

		assertThat(barberoServicios.findAllByNegocioIdAndUsuarioId(norte.getId(), juan.getId())).isEmpty();
	}

	@Test
	void unDuenoNoVeNiTocaElPerfilDeAlguienDeOtraBarberia() throws Exception {
		Usuario ana = datos.dueno(datos.negocio("Barbería Norte"), "Ana");
		Usuario pedro = datos.barbero(datos.negocio("Barbería Sur"), "Pedro");

		mvc.perform(get("/equipo/{id}", pedro.getId()).with(user(datos.sesion(ana))))
				.andExpect(status().isNotFound());
		mvc.perform(post("/equipo/{id}/atiende", pedro.getId()).with(user(datos.sesion(ana))).with(csrf())
						.param("valor", "false"))
				.andExpect(status().isNotFound());

		assertThat(pedro.isAtiende()).isTrue();
	}

}
