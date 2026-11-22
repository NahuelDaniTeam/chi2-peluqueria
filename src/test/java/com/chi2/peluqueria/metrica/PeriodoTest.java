package com.chi2.peluqueria.metrica;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PeriodoTest {

	int anio = LocalDate.now().getYear();

	@Test
	void cadaTipoAgarraSusDias() {
		LocalDate jueves = LocalDate.of(2026, 11, 19);

		assertThat(Periodo.de(Periodo.Tipo.DIA, jueves)).isEqualTo(new Periodo(Periodo.Tipo.DIA, jueves, jueves));
		assertThat(Periodo.de(Periodo.Tipo.SEMANA, jueves))
				.isEqualTo(new Periodo(Periodo.Tipo.SEMANA, LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 22)));
		assertThat(Periodo.de(Periodo.Tipo.MES, jueves))
				.isEqualTo(new Periodo(Periodo.Tipo.MES, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)));
		assertThat(Periodo.de(Periodo.Tipo.MES, jueves).anterior().desde()).isEqualTo(LocalDate.of(2026, 10, 1));
		assertThat(Periodo.de(Periodo.Tipo.SEMANA, jueves).siguiente().desde()).isEqualTo(LocalDate.of(2026, 11, 23));
		assertThat(Periodo.Tipo.desde("cualquiera")).isEqualTo(Periodo.Tipo.MES);
	}

	@Test
	void losTitulosSeLeenComoSeDicen() {
		assertThat(Periodo.de(Periodo.Tipo.MES, LocalDate.of(2026, 11, 19)).getTitulo()).isEqualTo("noviembre de 2026");
		assertThat(new Periodo(Periodo.Tipo.SEMANA, LocalDate.of(anio, 11, 16), LocalDate.of(anio, 11, 22)).getTitulo())
				.isEqualTo("semana del 16 al 22 de noviembre");
		assertThat(new Periodo(Periodo.Tipo.SEMANA, LocalDate.of(anio, 6, 29), LocalDate.of(anio, 7, 5)).getTitulo())
				.isEqualTo("semana del 29 de junio al 5 de julio");
		assertThat(Periodo.de(Periodo.Tipo.MES, LocalDate.of(anio, 11, 19)).etiqueta(LocalDate.of(anio, 11, 30)))
				.isEqualTo("Este mes");
		assertThat(Periodo.de(Periodo.Tipo.MES, LocalDate.of(anio, 11, 19)).etiqueta(LocalDate.of(anio, 12, 1)))
				.isEqualTo("Números");
	}

}
