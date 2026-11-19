package com.chi2.peluqueria.comun;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class FormatosTest {

	@ParameterizedTest
	@CsvSource({ "5, 5 min", "30, 30 min", "60, 1 h", "90, 1 h 30 min", "120, 2 h" })
	void duracion(int minutos, String esperado) {
		assertThat(Formatos.duracion(minutos)).isEqualTo(esperado);
	}

	@Test
	void precioConPuntoDeMiles() {
		assertThat(Formatos.precio(12000)).isEqualTo("$ 12.000");
		assertThat(Formatos.precio(500)).isEqualTo("$ 500");
	}

	@ParameterizedTest
	@CsvSource({ "12000, 12000", "'12.000', 12000", "'$ 1.250.000', 1250000", "' 800 ', 800" })
	void leePreciosComoLosEscribeLaGente(String texto, int esperado) {
		assertThat(Formatos.leerPrecio(texto)).contains(esperado);
	}

	@ParameterizedTest
	@CsvSource({ "''", "'12000,50'", "'abc'", "'-5'" })
	void rechazaPreciosInvalidos(String texto) {
		assertThat(Formatos.leerPrecio(texto)).isEmpty();
	}

}
