package com.chi2.peluqueria.pagina;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(PaginaController.class)
class PaginaControllerTest {

	@Autowired
	MockMvc mvc;

	@Test
	void inicioRenderizaConElLayoutDelSistema() throws Exception {
		mvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(view().name("inicio"))
				.andExpect(content().string(containsString("CHI2 Turnos")));
	}

}
