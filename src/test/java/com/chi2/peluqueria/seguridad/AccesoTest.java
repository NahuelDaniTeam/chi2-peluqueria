package com.chi2.peluqueria.seguridad;

import com.chi2.peluqueria.DatosDePrueba;
import com.chi2.peluqueria.PruebaIntegracion;
import com.chi2.peluqueria.negocio.Negocio;
import com.chi2.peluqueria.usuario.Rol;
import com.chi2.peluqueria.usuario.Usuario;
import com.chi2.peluqueria.usuario.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccesoTest extends PruebaIntegracion {

	@Autowired
	UsuarioRepository usuarios;

	@Test
	void sinSesionTodoLlevaAlLogin() throws Exception {
		mvc.perform(get("/")).andExpect(redirectedUrl("/login"));
		mvc.perform(get("/equipo")).andExpect(redirectedUrl("/login"));
		mvc.perform(get("/admin/negocios")).andExpect(redirectedUrl("/login"));
	}

	@Test
	void elLoginEsPublico() throws Exception {
		mvc.perform(get("/login"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Entrá a tu barbería")));
	}

	@Test
	void entraConEmailYContrasena() throws Exception {
		Usuario barbero = datos.barbero(datos.negocio("Barbería Norte"), "Juan");

		mvc.perform(formLogin("/login").user("email", barbero.getEmail()).password("contrasena", DatosDePrueba.CONTRASENA))
				.andExpect(authenticated().withUsername(barbero.getEmail()))
				.andExpect(redirectedUrl("/"));
	}

	@Test
	void elEmailNoDistingueMayusculas() throws Exception {
		Usuario barbero = datos.barbero(datos.negocio("Barbería Norte"), "Juan");

		mvc.perform(formLogin("/login").user("email", "  " + barbero.getEmail().toUpperCase() + " ")
						.password("contrasena", DatosDePrueba.CONTRASENA))
				.andExpect(authenticated());
	}

	@Test
	void contrasenaIncorrectaNoEntra() throws Exception {
		Usuario barbero = datos.barbero(datos.negocio("Barbería Norte"), "Juan");

		mvc.perform(formLogin("/login").user("email", barbero.getEmail()).password("contrasena", "otra-cualquiera"))
				.andExpect(unauthenticated())
				.andExpect(redirectedUrl("/login?error"));
	}

	@Test
	void unUsuarioDadoDeBajaNoEntra() throws Exception {
		Usuario barbero = datos.barbero(datos.negocio("Barbería Norte"), "Juan");
		barbero.darDeBaja();
		usuarios.flush();

		mvc.perform(formLogin("/login").user("email", barbero.getEmail()).password("contrasena", DatosDePrueba.CONTRASENA))
				.andExpect(unauthenticated());
	}

	@Test
	void nadieDeUnaBarberiaSuspendidaEntra() throws Exception {
		Negocio negocio = datos.negocio("Barbería Norte");
		Usuario dueno = datos.dueno(negocio, "Ana");
		negocio.suspender();
		usuarios.flush();

		mvc.perform(formLogin("/login").user("email", dueno.getEmail()).password("contrasena", DatosDePrueba.CONTRASENA))
				.andExpect(unauthenticated());
	}

	@Test
	void alArrancarSeCreaElUsuarioDeChi2() throws Exception {
		assertThat(usuarios.buscarParaLogin(ADMIN_EMAIL))
				.hasValueSatisfying(admin -> assertThat(admin.getRol()).isEqualTo(Rol.ADMIN));

		mvc.perform(formLogin("/login").user("email", ADMIN_EMAIL).password("contrasena", ADMIN_CONTRASENA))
				.andExpect(authenticated());
	}

	@Test
	void cadaRolEmpiezaEnSuPantalla() throws Exception {
		Usuario barbero = datos.barbero(datos.negocio("Barbería Norte"), "Juan");
		UsuarioAutenticado admin = UsuarioAutenticado.de(usuarios.buscarParaLogin(ADMIN_EMAIL).orElseThrow());

		mvc.perform(get("/").with(user(datos.sesion(barbero))))
				.andExpect(redirectedUrl("/agenda"));
		mvc.perform(get("/agenda").with(user(datos.sesion(barbero))))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Barbería Norte")));
		mvc.perform(get("/").with(user(admin)))
				.andExpect(redirectedUrl("/admin/negocios"));
	}

	@Test
	void conSesionIniciadaElLoginLlevaAlInicio() throws Exception {
		Usuario barbero = datos.barbero(datos.negocio("Barbería Norte"), "Juan");

		mvc.perform(get("/login").with(user(datos.sesion(barbero))))
				.andExpect(redirectedUrl("/"));
	}

}
