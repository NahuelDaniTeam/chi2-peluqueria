package com.chi2.peluqueria.turno;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

import com.chi2.peluqueria.DatosDePrueba;
import com.chi2.peluqueria.PruebaIntegracion;
import com.chi2.peluqueria.negocio.Negocio;
import com.chi2.peluqueria.servicio.Servicio;
import com.chi2.peluqueria.usuario.Usuario;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class DisponibilidadTest extends PruebaIntegracion {

	@Autowired
	Disponibilidad disponibilidad;

	Negocio norte;
	Usuario juan;
	Usuario pedro;
	Servicio corte;
	LocalDate lunes = DatosDePrueba.dentroDeUnasSemanas(DayOfWeek.MONDAY);

	@BeforeEach
	void barberiaQueAbreLosLunesConCorteAlMediodia() {
		norte = datos.negocio("Barbería Norte");
		juan = datos.barbero(norte, "Juan");
		pedro = datos.barbero(norte, "Pedro");
		corte = datos.servicio(norte, "Corte", 30, 12000);
		datos.hace(juan, corte);
		datos.hace(pedro, corte);
		datos.franja(norte, null, DayOfWeek.MONDAY, "09:00", "13:00");
		datos.franja(norte, null, DayOfWeek.MONDAY, "16:00", "20:00");
	}

	@Test
	void ofreceCadaCuartoDeHoraDentroDelHorarioDelLocal() {
		Disponibilidad.DiaLibre dia = disponibilidad.libres(pedido(corte, null), lunes);

		assertThat(dia.abierto()).isTrue();
		assertThat(dia.horas()).startsWith(h("09:00"), h("09:15")).contains(h("12:30"), h("16:00")).endsWith(h("19:30"))
				.doesNotContain(h("12:45"), h("13:00"), h("15:45"));
	}

	@Test
	void unTurnoTomadoSacaEsosHorariosSoloAEseBarbero() {
		datos.turno(juan, corte, lunes, "10:00", "Martín");

		assertThat(disponibilidad.libres(pedido(corte, juan.getId()), lunes).horas())
				.contains(h("09:30"), h("10:30")).doesNotContain(h("09:45"), h("10:00"), h("10:15"));
		assertThat(disponibilidad.libres(pedido(corte, null), lunes).horas()).contains(h("10:00"));
		assertThat(disponibilidad.barberosLibres(pedido(corte, null), lunes, h("10:00"))).containsExactly(pedro);
	}

	@Test
	void despuesDeUnTurnoCortoSePuedeEmpezarJustoCuandoTermina() {
		Servicio barba = datos.servicio(norte, "Barba", 20, 8000);
		datos.turno(juan, barba, lunes, "09:00", "Martín");

		assertThat(disponibilidad.libres(pedido(corte, juan.getId()), lunes).horas()).startsWith(h("09:20"), h("09:30"));
	}

	@Test
	void elHorarioPropioDelBarberoSeRespetaDentroDelDelLocal() {
		datos.franja(norte, juan, DayOfWeek.MONDAY, "12:00", "18:00");
		datos.franja(norte, pedro, DayOfWeek.TUESDAY, "09:00", "18:00");

		assertThat(disponibilidad.libres(pedido(corte, juan.getId()), lunes).horas())
				.containsExactly(h("12:00"), h("12:15"), h("12:30"), h("16:00"), h("16:15"), h("16:30"), h("16:45"),
						h("17:00"), h("17:15"), h("17:30"));
		// Pedro tiene horario propio y los lunes no trabaja
		assertThat(disponibilidad.libres(pedido(corte, pedro.getId()), lunes).horas()).isEmpty();
	}

	@Test
	void conElLocalCerradoNoHayNadaYConUnBarberoAusenteQuedanLosDemas() {
		LocalDate otroLunes = lunes.plusWeeks(1);
		datos.ausencia(norte, null, lunes);
		datos.ausencia(norte, juan, otroLunes);

		Disponibilidad.DiaLibre cerrado = disponibilidad.libres(pedido(corte, null), lunes);
		assertThat(cerrado.abierto()).isFalse();
		assertThat(cerrado.horas()).isEmpty();

		assertThat(disponibilidad.libres(pedido(corte, juan.getId()), otroLunes).horas()).isEmpty();
		assertThat(disponibilidad.barberosLibres(pedido(corte, null), otroLunes, h("10:00"))).containsExactly(pedro);
	}

	@Test
	void soloCuentanLosQueHacenElServicioYAtienden() {
		Servicio tintura = datos.servicio(norte, "Corte + tintura", 120, 30000);
		datos.hace(juan, tintura);
		Usuario ana = datos.dueno(norte, "Ana");
		datos.hace(ana, tintura); // la dueña no atiende, así que no cuenta

		assertThat(disponibilidad.barberos(norte.getId(), tintura.getId())).containsExactly(juan);
		// Dos horas: a la mañana se puede empezar hasta las 11 y a la tarde hasta las 18
		assertThat(disponibilidad.libres(pedido(tintura, null), lunes).horas()).contains(h("11:00"), h("18:00"))
				.doesNotContain(h("11:15"), h("18:15"));
	}

	@Test
	void losDiasQueYaPasaronNoTienenHorarios() {
		LocalDate lunesPasado = LocalDate.now(DatosDePrueba.ZONA).minusWeeks(1)
				.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

		assertThat(disponibilidad.libres(pedido(corte, null), lunesPasado).horas()).isEmpty();
	}

	private Disponibilidad.Pedido pedido(Servicio servicio, Long barberoId) {
		return new Disponibilidad.Pedido(norte.getId(), servicio.getId(), servicio.getDuracionMinutos(), barberoId, null);
	}

	private static LocalTime h(String hora) {
		return LocalTime.parse(hora);
	}

}
