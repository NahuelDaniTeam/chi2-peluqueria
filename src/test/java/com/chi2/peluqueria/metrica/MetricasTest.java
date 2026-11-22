package com.chi2.peluqueria.metrica;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

import com.chi2.peluqueria.DatosDePrueba;
import com.chi2.peluqueria.PruebaIntegracion;
import com.chi2.peluqueria.negocio.Negocio;
import com.chi2.peluqueria.seguridad.UsuarioAutenticado;
import com.chi2.peluqueria.servicio.Servicio;
import com.chi2.peluqueria.turno.MedioPago;
import com.chi2.peluqueria.turno.Turno;
import com.chi2.peluqueria.turno.TurnoRepository;
import com.chi2.peluqueria.usuario.Usuario;
import com.chi2.peluqueria.usuario.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.chi2.peluqueria.DatosDePrueba.instante;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Etapa 5: los números de cada barbero y de la barbería. */
class MetricasTest extends PruebaIntegracion {

	@Autowired
	MetricasService metricas;

	@Autowired
	TurnoRepository turnos;

	@Autowired
	UsuarioRepository usuarios;

	Negocio norte;
	Usuario ana;
	Usuario juan;
	Usuario pedro;
	Servicio corte;
	Servicio barba;
	Servicio corteYBarba;
	/** Un mes que ya pasó entero: el anterior al actual. */
	LocalDate dia10 = LocalDate.now(DatosDePrueba.ZONA).minusMonths(1).withDayOfMonth(10);
	Periodo mes = Periodo.de(Periodo.Tipo.MES, dia10);

	@BeforeEach
	void unMesConDeTodo() {
		norte = datos.negocio("Barbería Norte");
		ana = datos.dueno(norte, "Ana");
		juan = datos.barbero(norte, "Juan");
		pedro = datos.barbero(norte, "Pedro");
		corte = datos.servicio(norte, "Corte", 30, 12000);
		barba = datos.servicio(norte, "Barba", 20, 8000);
		corteYBarba = datos.servicio(norte, "Corte + barba", 45, 15000);

		// Juan: tres atenciones (una sin turno), uno que no vino, una cancelación tardía y una atención anulada
		finalizado(juan, corte, dia10, "10:00", 12000, MedioPago.EFECTIVO);
		finalizado(juan, corte, dia10.plusDays(1), "10:00", 15000, MedioPago.TRANSFERENCIA)
				.corregirCobro(corteYBarba, 15000, MedioPago.TRANSFERENCIA);
		datos.turno(juan, corte, dia10.plusDays(2), "10:00", "Diego").marcarAusente(Instant.now());
		datos.turno(juan, corte, dia10.plusDays(3), "10:00", "Bruno").cancelar(true);
		Turno sinTurno = turnos.save(Turno.sinTurno(norte.getId(), juan, barba, instante(dia10.plusDays(4), "11:00"),
				Duration.ofMinutes(20), null, juan.getId()));
		sinTurno.finalizar(barba, 8000, MedioPago.EFECTIVO, Instant.now());
		turnos.save(Turno.sinTurno(norte.getId(), juan, barba, instante(dia10.plusDays(5), "11:00"),
				Duration.ofMinutes(20), null, juan.getId())).anular();

		// Pedro: una atención
		finalizado(pedro, corte, dia10, "10:00", 18000, MedioPago.EFECTIVO);
		turnos.flush();
	}

	@Test
	void cuentaLoCobradoYComoPorBarbero() {
		MetricasService.Resumen r = metricas.resumen(norte.getId(), juan.getId(), mes);

		assertThat(r.atenciones()).isEqualTo(3);
		assertThat(r.sinTurno()).isEqualTo(1);
		assertThat(r.cobrado()).isEqualTo(35000);
		assertThat(r.efectivo()).isEqualTo(20000);
		assertThat(r.transferencia()).isEqualTo(15000);
		assertThat(r.getPorcentajeEfectivo()).isEqualTo(57);
		assertThat(r.getPromedioTexto()).isEqualTo("$ 11.667");
		assertThat(r.ausentes()).isEqualTo(1);
		assertThat(r.cancelados()).isEqualTo(1);
		assertThat(r.canceladosTarde()).isEqualTo(1);
		// Por servicio cuenta lo que se hizo (corregido), no lo que se reservó
		assertThat(r.servicios()).extracting(MetricasService.PorServicio::nombre)
				.containsExactly("Corte + barba", "Corte", "Barba");
		assertThat(r.dias()).hasSize(mes.dias().size());
		assertThat(r.dias().get(9).cobrado()).isEqualTo(12000);
	}

	@Test
	void laBarberiaCompletaComparaBarberos() {
		MetricasService.Resumen r = metricas.resumen(norte.getId(), null, mes);

		assertThat(r.cobrado()).isEqualTo(53000);
		assertThat(r.barberos()).extracting(MetricasService.PorBarbero::nombre).containsExactly("Juan", "Pedro");
		assertThat(r.barberos().get(0).ausentes()).isEqualTo(1);
		assertThat(r.dias().get(9).cobrado()).isEqualTo(30000);
	}

	@Test
	void unTurnoCuentaEnElDiaDeLaBarberiaAunqueEnUtcSeaOtro() {
		LocalDate ultimo = mes.hasta();
		finalizado(juan, corte, ultimo, "23:30", 12000, MedioPago.EFECTIVO);

		assertThat(metricas.resumen(norte.getId(), juan.getId(), Periodo.de(Periodo.Tipo.DIA, ultimo)).atenciones())
				.isEqualTo(1);
		assertThat(metricas.resumen(norte.getId(), juan.getId(), Periodo.de(Periodo.Tipo.DIA, ultimo.plusDays(1)))
				.atenciones()).isZero();
	}

	@Test
	void avisaLoQueQuedoSinCerrarYLoQueQuedaPorDelante() {
		LocalDate ayer = LocalDate.now(DatosDePrueba.ZONA).minusDays(1);
		datos.turno(juan, corte, ayer, "10:00", "Lucas");
		LocalDate lunes = DatosDePrueba.dentroDeUnasSemanas(java.time.DayOfWeek.MONDAY);
		datos.turno(juan, corte, lunes, "10:00", "Martín");

		assertThat(metricas.resumen(norte.getId(), juan.getId(), Periodo.de(Periodo.Tipo.DIA, ayer)).sinCerrar())
				.isEqualTo(1);
		assertThat(metricas.resumen(norte.getId(), juan.getId(), Periodo.de(Periodo.Tipo.SEMANA, lunes)).porDelante())
				.isEqualTo(1);
	}

	// ---- Pantalla ----

	@Test
	void elDuenoVeTodoYPuedeMirarAUnBarbero() throws Exception {
		mvc.perform(numerosDelMes().with(user(sesion(ana))))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("$ 53.000")))
				.andExpect(content().string(containsString("id=\"por-barbero\"")))
				.andExpect(content().string(containsString("Pedro")));
		mvc.perform(numerosDelMes().param("barbero", juan.getId().toString()).with(user(sesion(ana))))
				.andExpect(content().string(containsString("$ 35.000")))
				.andExpect(content().string(containsString("De Juan")))
				.andExpect(content().string(not(containsString("id=\"por-barbero\""))));
	}

	@Test
	void unBarberoVeSoloLoSuyo() throws Exception {
		mvc.perform(numerosDelMes().param("barbero", pedro.getId().toString()).with(user(sesion(juan))))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("$ 35.000")))
				.andExpect(content().string(containsString("1 sin turno")))
				.andExpect(content().string(containsString("1 con poco aviso")))
				.andExpect(content().string(not(containsString("$ 53.000"))))
				.andExpect(content().string(not(containsString("$ 18.000"))))
				.andExpect(content().string(not(containsString("id=\"por-barbero\""))));
	}

	@Test
	void otraBarberiaNoVeNada() throws Exception {
		Negocio sur = datos.negocio("Barbería Sur");
		Usuario rita = datos.dueno(sur, "Rita");

		mvc.perform(numerosDelMes().param("barbero", juan.getId().toString()).with(user(sesion(rita))))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Todavía no hay nada en este período.")))
				.andExpect(content().string(not(containsString("Juan"))));
		UsuarioAutenticado admin = UsuarioAutenticado.de(usuarios.buscarParaLogin(ADMIN_EMAIL).orElseThrow());
		mvc.perform(get("/numeros").with(user(admin))).andExpect(status().isForbidden());
	}

	@Test
	void sinParametrosMuestraElMesDeHoy() throws Exception {
		mvc.perform(get("/numeros").with(user(sesion(juan))))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Este mes")));
	}

	// ---- Ayudas ----

	private Turno finalizado(Usuario barbero, Servicio servicio, LocalDate dia, String hora, int monto, MedioPago medio) {
		Turno turno = datos.turno(barbero, servicio, dia, hora, "Cliente");
		turno.finalizar(servicio, monto, medio, Instant.now());
		return turno;
	}

	private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder numerosDelMes() {
		return get("/numeros").param("periodo", "mes").param("fecha", dia10.toString());
	}

	private UsuarioAutenticado sesion(Usuario usuario) {
		return datos.sesion(usuario);
	}

}
