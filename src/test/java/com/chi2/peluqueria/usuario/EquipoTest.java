package com.chi2.peluqueria.usuario;

import com.chi2.peluqueria.PruebaIntegracion;
import com.chi2.peluqueria.negocio.Negocio;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EquipoTest extends PruebaIntegracion {

	@Autowired
	UsuarioRepository usuarios;

	@Test
	void elDuenoVeSoloSuEquipo() throws Exception {
		Negocio norte = datos.negocio("Barbería Norte");
		Usuario ana = datos.dueno(norte, "Ana");
		datos.barbero(norte, "Juan");
		datos.barbero(datos.negocio("Barbería Sur"), "Pedro");

		mvc.perform(get("/equipo").with(user(datos.sesion(ana))))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Juan")))
				.andExpect(content().string(not(containsString("Pedro"))));
	}

	@Test
	void unBarberoNoEntraAlEquipo() throws Exception {
		Usuario juan = datos.barbero(datos.negocio("Barbería Norte"), "Juan");

		mvc.perform(get("/equipo").with(user(datos.sesion(juan)))).andExpect(status().isForbidden());
	}

	@Test
	void elDuenoSumaUnBarberoQueDespuesPuedeEntrar() throws Exception {
		Negocio norte = datos.negocio("Barbería Norte");
		Usuario ana = datos.dueno(norte, "Ana");

		mvc.perform(post("/equipo").with(user(datos.sesion(ana))).with(csrf())
						.param("nombre", "Lucas")
						.param("email", "Lucas@Norte.test")
						.param("contrasena", "clave-de-lucas")
						.param("rol", "BARBERO"))
				.andExpect(redirectedUrl("/equipo"));

		Usuario lucas = usuarios.buscarParaLogin("lucas@norte.test").orElseThrow();
		assertThat(lucas.getNegocio().getId()).isEqualTo(norte.getId());
		assertThat(lucas.getRol()).isEqualTo(Rol.BARBERO);
		mvc.perform(formLogin("/login").user("email", "lucas@norte.test").password("contrasena", "clave-de-lucas"))
				.andExpect(authenticated());
	}

	@Test
	void noSePuedeRepetirUnEmail() throws Exception {
		Negocio norte = datos.negocio("Barbería Norte");
		Usuario ana = datos.dueno(norte, "Ana");
		Usuario pedro = datos.barbero(datos.negocio("Barbería Sur"), "Pedro");

		mvc.perform(post("/equipo").with(user(datos.sesion(ana))).with(csrf())
						.param("nombre", "Otro Pedro")
						.param("email", pedro.getEmail())
						.param("contrasena", "clave-de-pedro")
						.param("rol", "BARBERO"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Ya hay una cuenta con ese email.")));
	}

	@Test
	void unDuenoNoPuedeCrearUsuariosDeChi2() throws Exception {
		Usuario ana = datos.dueno(datos.negocio("Barbería Norte"), "Ana");

		mvc.perform(post("/equipo").with(user(datos.sesion(ana))).with(csrf())
						.param("nombre", "Colado")
						.param("email", "colado@norte.test")
						.param("contrasena", "clave-del-colado")
						.param("rol", "ADMIN"))
				.andExpect(status().isOk());

		assertThat(usuarios.existsByEmail("colado@norte.test")).isFalse();
	}

	@Test
	void unDuenoNoPuedeDarDeBajaAAlguienDeOtraBarberia() throws Exception {
		Usuario ana = datos.dueno(datos.negocio("Barbería Norte"), "Ana");
		Usuario pedro = datos.barbero(datos.negocio("Barbería Sur"), "Pedro");

		mvc.perform(post("/equipo/{id}/baja", pedro.getId()).with(user(datos.sesion(ana))).with(csrf()))
				.andExpect(status().isNotFound());

		assertThat(usuarios.findById(pedro.getId()).orElseThrow().isActivo()).isTrue();
	}

	@Test
	void darDeBajaYReactivar() throws Exception {
		Negocio norte = datos.negocio("Barbería Norte");
		Usuario ana = datos.dueno(norte, "Ana");
		Usuario juan = datos.barbero(norte, "Juan");

		mvc.perform(post("/equipo/{id}/baja", juan.getId()).with(user(datos.sesion(ana))).with(csrf()))
				.andExpect(redirectedUrl("/equipo"));
		assertThat(usuarios.findById(juan.getId()).orElseThrow().isActivo()).isFalse();

		mvc.perform(post("/equipo/{id}/alta", juan.getId()).with(user(datos.sesion(ana))).with(csrf()))
				.andExpect(redirectedUrl("/equipo"));
		assertThat(usuarios.findById(juan.getId()).orElseThrow().isActivo()).isTrue();
	}

	@Test
	void nadieSePuedeDarDeBajaASiMismo() throws Exception {
		Usuario ana = datos.dueno(datos.negocio("Barbería Norte"), "Ana");

		mvc.perform(post("/equipo/{id}/baja", ana.getId()).with(user(datos.sesion(ana))).with(csrf()))
				.andExpect(status().is4xxClientError());

		assertThat(usuarios.findById(ana.getId()).orElseThrow().isActivo()).isTrue();
	}

}
