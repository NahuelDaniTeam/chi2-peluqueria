package com.chi2.peluqueria.turno;

import java.time.LocalTime;
import java.util.List;

import com.chi2.peluqueria.turno.Tramos.Tramo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TramosTest {

	@Test
	void laInterseccionDejaLoQueEstaEnLosDos() {
		List<Tramo> local = List.of(t("09:00", "13:00"), t("16:00", "20:00"));
		List<Tramo> barbero = List.of(t("12:00", "18:00"));

		assertThat(Tramos.interseccion(barbero, local)).containsExactly(t("12:00", "13:00"), t("16:00", "18:00"));
	}

	@Test
	void restarParteUnTramoYSacaLoQueQuedaTapado() {
		List<Tramo> dia = List.of(t("09:00", "13:00"));

		assertThat(Tramos.restar(dia, List.of(t("10:00", "10:30")))).containsExactly(t("09:00", "10:00"), t("10:30", "13:00"));
		assertThat(Tramos.restar(dia, List.of(t("08:00", "09:30"), t("12:30", "14:00")))).containsExactly(t("09:30", "12:30"));
		assertThat(Tramos.restar(dia, List.of(t("09:00", "13:00")))).isEmpty();
		assertThat(Tramos.restar(dia, List.of(t("14:00", "15:00")))).containsExactly(t("09:00", "13:00"));
	}

	@Test
	void ofreceCadaCuartoDeHoraMientrasEntreCompleto() {
		assertThat(horas(Tramos.inicios(List.of(t("09:00", "10:00")), 30, 15, 0)))
				.containsExactly("09:00", "09:15", "09:30");
	}

	@Test
	void tambienOfreceJustoCuandoTerminaOtroTurno() {
		// Libre desde las 9:20 porque antes hubo una barba de 20 minutos
		assertThat(horas(Tramos.inicios(List.of(t("09:20", "10:00")), 30, 15, 0))).containsExactly("09:20", "09:30");
	}

	@Test
	void noOfreceHorasQueYaPasaron() {
		assertThat(horas(Tramos.inicios(List.of(t("09:00", "10:30")), 30, 15, Tramos.minutos(LocalTime.of(9, 31)))))
				.containsExactly("09:45", "10:00");
	}

	@Test
	void sabeSiAlgoEntraEnUnTramo() {
		List<Tramo> libres = List.of(t("09:00", "10:00"));

		assertThat(Tramos.entra(libres, Tramos.minutos(LocalTime.of(9, 30)), 30)).isTrue();
		assertThat(Tramos.entra(libres, Tramos.minutos(LocalTime.of(9, 35)), 30)).isFalse();
		assertThat(Tramos.entra(libres, Tramos.minutos(LocalTime.of(8, 55)), 30)).isFalse();
	}

	private static Tramo t(String desde, String hasta) {
		return Tramo.de(LocalTime.parse(desde), LocalTime.parse(hasta));
	}

	private static List<String> horas(List<Integer> minutos) {
		return minutos.stream().map(m -> Tramos.hora(m).toString()).toList();
	}

}
