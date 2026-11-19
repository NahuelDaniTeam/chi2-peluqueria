package com.chi2.peluqueria.negocio;

import com.chi2.peluqueria.PruebaIntegracion;
import com.chi2.peluqueria.seguridad.UsuarioAutenticado;
import com.chi2.peluqueria.usuario.Rol;
import com.chi2.peluqueria.usuario.Usuario;
import com.chi2.peluqueria.usuario.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminNegocioTest extends PruebaIntegracion {

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	NegocioRepository negocios;

	UsuarioAutenticado admin;

	@BeforeEach
	void sesionDeChi2() {
		admin = UsuarioAutenticado.de(usuarios.buscarParaLogin(ADMIN_EMAIL).orElseThrow());
	}

	@Test
	void creaUnaBarberiaConSuDuenoQueDespuesPuedeEntrar() throws Exception {
		mvc.perform(post("/admin/negocios").with(user(admin)).with(csrf())
						.param("nombre", "Barbería Don Ñoño")
						.param("slug", "")
						.param("duenoNombre", "Ñoño")
						.param("duenoEmail", "nono@donnono.test")
						.param("duenoContrasena", "clave-de-nono"))
				.andExpect(redirectedUrl("/admin/negocios"));

		Usuario dueno = usuarios.buscarParaLogin("nono@donnono.test").orElseThrow();
		assertThat(dueno.getRol()).isEqualTo(Rol.DUENO);
		assertThat(dueno.getNegocio().getSlug()).isEqualTo("barberia-don-nono");
		mvc.perform(formLogin("/login").user("email", "nono@donnono.test").password("contrasena", "clave-de-nono"))
				.andExpect(authenticated());
	}

	@Test
	void elLinkNoSePuedeRepetirNiPisarRutasDelSistema() throws Exception {
		Negocio existente = datos.negocio("Barbería Norte");

		crear("Otra Norte", existente.getSlug(), "otra@norte.test")
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Ya hay una barbería con el link")));
		crear("Login", "", "login@barberia.test")
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("ese no se puede usar")));
	}

	@Test
	void suspenderDejaAfueraALaBarberia() throws Exception {
		Negocio norte = datos.negocio("Barbería Norte");

		mvc.perform(post("/admin/negocios/{id}/suspender", norte.getId()).with(user(admin)).with(csrf()))
				.andExpect(redirectedUrl("/admin/negocios"));

		assertThat(negocios.findById(norte.getId()).orElseThrow().isActivo()).isFalse();
	}

	@Test
	void soloChi2EntraAlPanelDeBarberias() throws Exception {
		Usuario ana = datos.dueno(datos.negocio("Barbería Norte"), "Ana");

		mvc.perform(get("/admin/negocios").with(user(datos.sesion(ana)))).andExpect(status().isForbidden());
		mvc.perform(get("/admin/negocios").with(user(admin))).andExpect(status().isOk());
	}

	private org.springframework.test.web.servlet.ResultActions crear(String nombre, String slug, String email)
			throws Exception {
		return mvc.perform(post("/admin/negocios").with(user(admin)).with(csrf())
				.param("nombre", nombre)
				.param("slug", slug)
				.param("duenoNombre", "Dueño")
				.param("duenoEmail", email)
				.param("duenoContrasena", "clave-del-dueno"));
	}

}
