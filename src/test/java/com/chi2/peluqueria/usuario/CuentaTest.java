package com.chi2.peluqueria.usuario;

import com.chi2.peluqueria.DatosDePrueba;
import com.chi2.peluqueria.PruebaIntegracion;

import org.junit.jupiter.api.Test;

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

class CuentaTest extends PruebaIntegracion {

	@Test
	void muestraLosDatosPropios() throws Exception {
		Usuario juan = datos.barbero(datos.negocio("Barbería Norte"), "Juan");

		mvc.perform(get("/cuenta").with(user(datos.sesion(juan))))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString(juan.getEmail())))
				.andExpect(content().string(containsString("Barbería Norte")));
	}

	@Test
	void cambiaLaContrasena() throws Exception {
		Usuario juan = datos.barbero(datos.negocio("Barbería Norte"), "Juan");

		mvc.perform(post("/cuenta/contrasena").with(user(datos.sesion(juan))).with(csrf())
						.param("actual", DatosDePrueba.CONTRASENA)
						.param("nueva", "mi-clave-nueva")
						.param("repetida", "mi-clave-nueva"))
				.andExpect(redirectedUrl("/cuenta"));

		mvc.perform(formLogin("/login").user("email", juan.getEmail()).password("contrasena", "mi-clave-nueva"))
				.andExpect(authenticated());
	}

	@Test
	void pideLaContrasenaActualCorrecta() throws Exception {
		Usuario juan = datos.barbero(datos.negocio("Barbería Norte"), "Juan");

		mvc.perform(post("/cuenta/contrasena").with(user(datos.sesion(juan))).with(csrf())
						.param("actual", "no-es-esta")
						.param("nueva", "mi-clave-nueva")
						.param("repetida", "mi-clave-nueva"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("La contraseña actual no es correcta.")));
	}

	@Test
	void lasDosContrasenasNuevasTienenQueCoincidir() throws Exception {
		Usuario juan = datos.barbero(datos.negocio("Barbería Norte"), "Juan");

		mvc.perform(post("/cuenta/contrasena").with(user(datos.sesion(juan))).with(csrf())
						.param("actual", DatosDePrueba.CONTRASENA)
						.param("nueva", "mi-clave-nueva")
						.param("repetida", "otra-distinta"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("no coinciden")));
	}

}
