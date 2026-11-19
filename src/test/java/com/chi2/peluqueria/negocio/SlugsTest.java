package com.chi2.peluqueria.negocio;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class SlugsTest {

	@ParameterizedTest
	@CsvSource({
			"'Barbería Juan', barberia-juan",
			"'  Peñarol & Co.  ', penarol-co",
			"'El Corte   Fino', el-corte-fino",
			"'---', ''"
	})
	void armaElLinkDesdeElNombre(String nombre, String esperado) {
		assertThat(Slugs.desde(nombre)).isEqualTo(esperado);
	}

	@ParameterizedTest
	@CsvSource({ "login", "admin", "equipo", "api" })
	void lasRutasDelSistemaEstanReservadas(String slug) {
		assertThat(Slugs.esReservado(slug)).isTrue();
	}

}
