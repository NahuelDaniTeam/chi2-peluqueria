package com.chi2.peluqueria.turno;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.chi2.peluqueria.DatosDePrueba;
import com.chi2.peluqueria.PruebaIntegracion;
import com.chi2.peluqueria.barbero.BarberoServicioRepository;
import com.chi2.peluqueria.negocio.Negocio;
import com.chi2.peluqueria.seguridad.UsuarioAutenticado;
import com.chi2.peluqueria.servicio.Servicio;
import com.chi2.peluqueria.usuario.Usuario;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Etapa 4: empezar, finalizar marcando el cobro, "no vino", deshacer, corregir y la atención sin turno. */
class AtencionTest extends PruebaIntegracion {

	@Autowired
	TurnoRepository turnos;

	@Autowired
	TurnoCambioRepository cambios;

	@Autowired
	BarberoServicioRepository barberoServicios;

	Negocio norte;
	Usuario ana;
	Usuario juan;
	Usuario pedro;
	Servicio corte;
	Servicio barba;
	Servicio corteYBarba;
	/** Ahora, al minuto: un turno que empieza acá es de hoy y todavía no terminó. */
	Instant ahora = Instant.now().truncatedTo(ChronoUnit.MINUTES);

	@BeforeEach
	void barberiaConDosBarberos() {
		norte = datos.negocio("Barbería Norte");
		ana = datos.dueno(norte, "Ana");
		juan = datos.barbero(norte, "Juan");
		pedro = datos.barbero(norte, "Pedro");
		corte = datos.servicio(norte, "Corte", 30, 12000);
		barba = datos.servicio(norte, "Barba", 20, 8000);
		corteYBarba = datos.servicio(norte, "Corte + barba", 45, 15000);
		datos.hace(juan, corte, barba, corteYBarba);
		datos.hace(pedro, corte, barba, corteYBarba);
	}

	// ---- Empezar y finalizar ----

	@Test
	void seEmpiezaYSeFinalizaEnUnToqueConElPrecioDeSiempre() throws Exception {
		Turno turno = datos.turno(juan, corte, ahora, "Martín");

		mvc.perform(get("/agenda/turnos/{id}", turno.getId()).with(user(sesion(juan))))
				.andExpect(content().string(containsString("Empezar")))
				.andExpect(content().string(not(containsString("Transferencia"))));
		mvc.perform(post("/agenda/turnos/{id}/empezar", turno.getId()).with(user(sesion(juan))).with(csrf()))
				.andExpect(redirectedUrl("/agenda/turnos/" + turno.getId()));
		assertThat(turnos.findById(turno.getId()).orElseThrow().getEstado()).isEqualTo(EstadoTurno.EN_CURSO);

		mvc.perform(get("/agenda/turnos/{id}", turno.getId()).with(user(sesion(juan))))
				.andExpect(content().string(containsString("¿Terminaste? Marcá cómo pagó")))
				.andExpect(content().string(containsString("Corte · $ 12.000")))
				.andExpect(content().string(containsString("value=\"TRANSFERENCIA\"")));
		mvc.perform(finalizar(turno, corte, "12000", "EFECTIVO").with(user(sesion(juan))))
				.andExpect(redirectedUrl("/agenda"))
				.andExpect(flash().attribute("aviso", "Listo: Martín · Corte · $ 12.000 · Efectivo."));

		Turno finalizado = turnos.findById(turno.getId()).orElseThrow();
		assertThat(finalizado.getEstado()).isEqualTo(EstadoTurno.FINALIZADO);
		assertThat(finalizado.getMonto()).isEqualTo(12000);
		assertThat(finalizado.getMedioPago()).isEqualTo(MedioPago.EFECTIVO);
		assertThat(finalizado.getEmpezadoEn()).isNotNull();
		assertThat(finalizado.getFinalizadoEn()).isNotNull();
		// El horario reservado no cambia
		assertThat(finalizado.getFin()).isEqualTo(ahora.plus(Duration.ofMinutes(30)));
		assertThat(cambios.historial(norte.getId(), turno.getId())).extracting(TurnoCambio::getAccion)
				.containsExactly(TurnoCambio.Accion.EMPEZADO, TurnoCambio.Accion.FINALIZADO);
	}

	@Test
	void alFinalizarSePuedeCambiarElServicioYElMonto() throws Exception {
		Turno turno = datos.turno(juan, corte, ahora, "Martín");
		turno.empezar(Instant.now());

		// Elegir otro servicio trae su precio
		mvc.perform(get("/agenda/turnos/{id}/cobro", turno.getId()).param("servicio", corteYBarba.getId().toString())
						.with(user(sesion(juan))))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("value=\"15.000\"")));
		mvc.perform(finalizar(turno, corteYBarba, "$ 15.500", "TRANSFERENCIA").with(user(sesion(juan))))
				.andExpect(redirectedUrl("/agenda"));

		Turno finalizado = turnos.findById(turno.getId()).orElseThrow();
		assertThat(finalizado.getServicio().getId()).isEqualTo(corteYBarba.getId());
		assertThat(finalizado.getMonto()).isEqualTo(15500);
		assertThat(finalizado.getMedioPago()).isEqualTo(MedioPago.TRANSFERENCIA);
		assertThat(ultimoCambio(turno).getDetalle()).isEqualTo("Corte + barba · $ 15.500 · Transferencia (reservado: Corte).");
	}

	@Test
	void ofreceElPrecioEspecialDelBarbero() throws Exception {
		barberoServicios.findByNegocioIdAndUsuarioIdAndServicioId(norte.getId(), juan.getId(), corte.getId())
				.orElseThrow().cambiarPrecioEspecial(14000);
		Turno turno = datos.turno(juan, corte, ahora, "Martín");
		turno.empezar(Instant.now());

		mvc.perform(get("/agenda/turnos/{id}", turno.getId()).with(user(sesion(ana))))
				.andExpect(content().string(containsString("Corte · $ 14.000")))
				.andExpect(content().string(containsString("value=\"14000\"")));
	}

	@Test
	void pideElMontoSinCentavosYElMedioDePago() throws Exception {
		Turno turno = datos.turno(juan, corte, ahora, "Martín");
		turno.empezar(Instant.now());

		mvc.perform(post("/agenda/turnos/{id}/finalizar", turno.getId()).param("servicio", corte.getId().toString())
						.param("monto", "12000,50").with(user(sesion(juan))).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Poné cuánto se cobró, en pesos y sin centavos.")))
				.andExpect(content().string(containsString("Elegí cómo pagó.")));
		assertThat(turnos.findById(turno.getId()).orElseThrow().getEstado()).isEqualTo(EstadoTurno.EN_CURSO);
	}

	@Test
	void unTurnoQueYaPasoSeFinalizaSinEmpezarPeroUnoFuturoNo() throws Exception {
		Turno pasado = datos.turno(juan, corte, ahora.minus(2, ChronoUnit.HOURS), "Lucas");
		Turno futuro = datos.turno(juan, corte, DatosDePrueba.dentroDeUnasSemanas(DayOfWeek.MONDAY), "10:00", "Bruno");

		mvc.perform(get("/agenda/turnos/{id}", pasado.getId()).with(user(sesion(juan))))
				.andExpect(content().string(containsString("Sin cerrar")))
				.andExpect(content().string(containsString("¿Lo atendiste? Marcá cómo pagó")));
		mvc.perform(finalizar(pasado, corte, "12000", "EFECTIVO").with(user(sesion(juan))))
				.andExpect(redirectedUrl("/agenda"));
		Turno finalizado = turnos.findById(pasado.getId()).orElseThrow();
		assertThat(finalizado.getEstado()).isEqualTo(EstadoTurno.FINALIZADO);
		assertThat(finalizado.getEmpezadoEn()).isNull();

		mvc.perform(finalizar(futuro, corte, "12000", "EFECTIVO").with(user(sesion(juan))))
				.andExpect(status().isBadRequest());
		mvc.perform(post("/agenda/turnos/{id}/empezar", futuro.getId()).with(user(sesion(juan))).with(csrf()))
				.andExpect(status().isBadRequest());
		mvc.perform(post("/agenda/turnos/{id}/ausente", futuro.getId()).with(user(sesion(juan))).with(csrf()))
				.andExpect(status().isBadRequest());
	}

	// ---- No vino y deshacer ----

	@Test
	void noVinoLiberaElHorarioYSePuedeDeshacer() throws Exception {
		Turno turno = datos.turno(juan, corte, ahora.minus(10, ChronoUnit.MINUTES), "Lucas");

		mvc.perform(post("/agenda/turnos/{id}/ausente", turno.getId()).with(user(sesion(juan))).with(csrf()))
				.andExpect(redirectedUrl("/agenda/turnos/" + turno.getId()))
				.andExpect(flash().attribute("aviso", containsString("El horario quedó libre.")));
		assertThat(turnos.findById(turno.getId()).orElseThrow().getEstado()).isEqualTo(EstadoTurno.AUSENTE);

		mvc.perform(post("/agenda/turnos/{id}/reservado", turno.getId()).with(user(sesion(juan))).with(csrf()))
				.andExpect(flash().attribute("aviso", containsString("volvió a quedar reservado")));
		assertThat(turnos.findById(turno.getId()).orElseThrow().getEstado()).isEqualTo(EstadoTurno.RESERVADO);
		assertThat(cambios.historial(norte.getId(), turno.getId())).extracting(TurnoCambio::getAccion)
				.containsExactly(TurnoCambio.Accion.AUSENTE, TurnoCambio.Accion.VUELTO_A_RESERVADO);
	}

	@Test
	void noSeDeshaceUnNoVinoSiOtroYaTomoElHorario() throws Exception {
		Turno turno = datos.turno(juan, corte, ahora.minus(10, ChronoUnit.MINUTES), "Lucas");
		turno.marcarAusente(Instant.now());
		turnos.flush();
		datos.turno(juan, barba, ahora, "Tomás");

		mvc.perform(post("/agenda/turnos/{id}/reservado", turno.getId()).with(user(sesion(juan))).with(csrf()))
				.andExpect(flash().attribute("avisoError", AtencionService.HORARIO_TOMADO));
		assertThat(turnos.findById(turno.getId()).orElseThrow().getEstado()).isEqualTo(EstadoTurno.AUSENTE);
	}

	@Test
	void unEmpezarTocadoSinQuererVuelveAReservado() throws Exception {
		Turno turno = datos.turno(juan, corte, ahora, "Martín");
		turno.empezar(Instant.now());

		mvc.perform(get("/agenda/turnos/{id}", turno.getId()).with(user(sesion(juan))))
				.andExpect(content().string(containsString("¿Lo empezaste sin querer? Volver a reservado")));
		mvc.perform(post("/agenda/turnos/{id}/reservado", turno.getId()).with(user(sesion(juan))).with(csrf()))
				.andExpect(status().is3xxRedirection());

		Turno reservado = turnos.findById(turno.getId()).orElseThrow();
		assertThat(reservado.getEstado()).isEqualTo(EstadoTurno.RESERVADO);
		assertThat(reservado.getEmpezadoEn()).isNull();
	}

	// ---- Corregir ----

	@Test
	void corregirUnFinalizadoQuedaEnElHistorial() throws Exception {
		Turno turno = datos.turno(juan, corte, ahora.minus(1, ChronoUnit.HOURS), "Lucas");
		turno.finalizar(corte, 12000, MedioPago.EFECTIVO, Instant.now());

		mvc.perform(get("/agenda/turnos/{id}", turno.getId()).with(user(sesion(juan))))
				.andExpect(content().string(containsString("$ 12.000 · Efectivo")))
				.andExpect(content().string(containsString("Corregir servicio o cobro")));
		mvc.perform(get("/agenda/turnos/{id}/cobro", turno.getId()).with(user(sesion(juan))))
				.andExpect(content().string(containsString("Corregir cobro")))
				.andExpect(content().string(containsString("value=\"12.000\"")));
		mvc.perform(corregir(turno, corteYBarba, "15000", "TRANSFERENCIA").with(user(sesion(juan))))
				.andExpect(redirectedUrl("/agenda/turnos/" + turno.getId()))
				.andExpect(flash().attribute("aviso", "Cobro corregido."));

		Turno corregido = turnos.findById(turno.getId()).orElseThrow();
		assertThat(corregido.getServicio().getId()).isEqualTo(corteYBarba.getId());
		assertThat(corregido.getCobroTexto()).isEqualTo("$ 15.000 · Transferencia");
		assertThat(ultimoCambio(turno).getAccion()).isEqualTo(TurnoCambio.Accion.COBRO_CORREGIDO);
		assertThat(ultimoCambio(turno).getDetalle()).isEqualTo("Antes: Corte · $ 12.000 · Efectivo.");

		// Guardar lo mismo no suma nada al historial; y un finalizado no se vuelve a finalizar
		mvc.perform(corregir(turno, corteYBarba, "15.000", "TRANSFERENCIA").with(user(sesion(juan))));
		assertThat(cambios.historial(norte.getId(), turno.getId())).hasSize(1);
		mvc.perform(finalizar(turno, corte, "12000", "EFECTIVO").with(user(sesion(juan))))
				.andExpect(status().isBadRequest());
	}

	// ---- Sin turno ----

	@Test
	void sinTurnoEmpiezaAhoraAunquePiseUnTurnoYSeFinalizaComoCualquiera() throws Exception {
		datos.turno(juan, corte, ahora.plus(10, ChronoUnit.MINUTES), "Lucas");

		mvc.perform(get("/agenda/sin-turno").with(user(sesion(juan))))
				.andExpect(content().string(containsString("Corte + barba")));
		mvc.perform(post("/agenda/sin-turno").param("servicio", corte.getId().toString()).with(user(sesion(juan)))
						.with(csrf()))
				.andExpect(redirectedUrlPattern("/agenda/turnos/*"));

		Turno atencion = sinTurnoDe(juan);
		assertThat(atencion.getEstado()).isEqualTo(EstadoTurno.EN_CURSO);
		assertThat(atencion.getClienteNombre()).isNull();
		assertThat(atencion.getInicio()).isBetween(ahora, ahora.plus(1, ChronoUnit.MINUTES));
		mvc.perform(get("/agenda").with(user(sesion(juan))))
				.andExpect(content().string(containsString("Cliente sin turno")))
				.andExpect(content().string(containsString("En curso")));

		mvc.perform(finalizar(atencion, corte, "12000", "TRANSFERENCIA").with(user(sesion(juan))))
				.andExpect(redirectedUrl("/agenda"));
		Turno finalizada = turnos.findById(atencion.getId()).orElseThrow();
		assertThat(finalizada.getEstado()).isEqualTo(EstadoTurno.FINALIZADO);
		// Terminó antes de los 30 minutos: el resto vuelve a quedar libre
		assertThat(finalizada.getFin()).isBefore(finalizada.getInicio().plus(Duration.ofMinutes(30)));
	}

	@Test
	void sinTurnoYaAtendidoSeCargaConElCobro() throws Exception {
		mvc.perform(post("/agenda/sin-turno").param("barbero", pedro.getId().toString())
						.param("servicio", barba.getId().toString()).param("nombre", "Tomás").param("monto", "8000")
						.param("medioPago", "EFECTIVO").with(user(sesion(ana))).with(csrf()))
				.andExpect(redirectedUrl("/agenda"))
				.andExpect(flash().attribute("aviso", "Listo: Barba con Pedro · $ 8.000 · Efectivo."));

		Turno atencion = sinTurnoDe(pedro);
		assertThat(atencion.getEstado()).isEqualTo(EstadoTurno.FINALIZADO);
		assertThat(atencion.getClienteNombre()).isEqualTo("Tomás");
		assertThat(atencion.getDuracion()).isEqualTo(Duration.ofMinutes(20));
		assertThat(atencion.getFin()).isBetween(ahora, ahora.plus(1, ChronoUnit.MINUTES));
		assertThat(cambios.historial(norte.getId(), atencion.getId())).extracting(TurnoCambio::getAccion)
				.containsExactly(TurnoCambio.Accion.SIN_TURNO, TurnoCambio.Accion.FINALIZADO);
	}

	@Test
	void sinTurnoPideQuienLoAtiende() throws Exception {
		// Ana no atiende y hay dos barberos: tiene que elegir
		mvc.perform(post("/agenda/sin-turno").param("servicio", corte.getId().toString()).with(user(sesion(ana)))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Elegí quién lo atiende.")));
		assertThat(turnos.findAll()).noneMatch(t -> t.getNegocioId().equals(norte.getId()));
	}

	@Test
	void unaAtencionSinTurnoCargadaPorErrorSeAnula() throws Exception {
		mvc.perform(post("/agenda/sin-turno").param("servicio", corte.getId().toString()).with(user(sesion(juan)))
				.with(csrf()));
		Turno atencion = sinTurnoDe(juan);

		mvc.perform(post("/agenda/turnos/{id}/anular", atencion.getId()).with(user(sesion(juan))).with(csrf()))
				.andExpect(redirectedUrl("/agenda"));
		assertThat(turnos.findById(atencion.getId()).orElseThrow().getEstado()).isEqualTo(EstadoTurno.CANCELADO);
		// Uno reservado no se anula (se cancela)
		Turno reservado = datos.turno(juan, corte, ahora.plus(1, ChronoUnit.HOURS), "Lucas");
		mvc.perform(post("/agenda/turnos/{id}/anular", reservado.getId()).with(user(sesion(juan))).with(csrf()))
				.andExpect(status().isBadRequest());
	}

	// ---- Sin cerrar ----

	@Test
	void laAgendaDeHoyMuestraLoQueQuedoSinCerrarDeOtrosDias() throws Exception {
		Turno olvidado = datos.turno(juan, corte, ahora.minus(3, ChronoUnit.DAYS), "Lucas");

		mvc.perform(get("/agenda").with(user(sesion(ana))))
				.andExpect(content().string(containsString("Quedó 1 turno sin cerrar")))
				.andExpect(content().string(containsString("Lucas")));
		mvc.perform(get("/agenda").with(user(sesion(pedro))))
				.andExpect(content().string(not(containsString("sin cerrar"))));

		olvidado.finalizar(corte, 12000, MedioPago.EFECTIVO, Instant.now());
		mvc.perform(get("/agenda").with(user(sesion(juan))))
				.andExpect(content().string(not(containsString("sin cerrar"))));
	}

	// ---- Qué toca cada uno ----

	@Test
	void unBarberoNoAtiendeLosTurnosDeOtro() throws Exception {
		Turno dePedro = datos.turno(pedro, corte, ahora, "Bruno");

		mvc.perform(post("/agenda/turnos/{id}/empezar", dePedro.getId()).with(user(sesion(juan))).with(csrf()))
				.andExpect(status().isNotFound());
		mvc.perform(finalizar(dePedro, corte, "12000", "EFECTIVO").with(user(sesion(juan))))
				.andExpect(status().isNotFound());
		mvc.perform(get("/agenda/turnos/{id}/cobro", dePedro.getId()).with(user(sesion(juan))))
				.andExpect(status().isNotFound());
		mvc.perform(post("/agenda/sin-turno").param("barbero", pedro.getId().toString())
						.param("servicio", corte.getId().toString()).with(user(sesion(juan))).with(csrf()))
				.andExpect(status().isBadRequest());
		// El dueño sí
		mvc.perform(post("/agenda/turnos/{id}/empezar", dePedro.getId()).with(user(sesion(ana))).with(csrf()))
				.andExpect(status().is3xxRedirection());
	}

	@Test
	void otraBarberiaNoTocaNada() throws Exception {
		Turno turno = datos.turno(juan, corte, ahora, "Martín");
		Negocio sur = datos.negocio("Barbería Sur");
		Usuario rita = datos.dueno(sur, "Rita");

		mvc.perform(post("/agenda/turnos/{id}/empezar", turno.getId()).with(user(sesion(rita))).with(csrf()))
				.andExpect(status().isNotFound());
		mvc.perform(finalizar(turno, corte, "12000", "EFECTIVO").with(user(sesion(rita))))
				.andExpect(status().isNotFound());
		mvc.perform(post("/agenda/turnos/{id}/ausente", turno.getId()).with(user(sesion(rita))).with(csrf()))
				.andExpect(status().isNotFound());
		mvc.perform(corregir(turno, corte, "12000", "EFECTIVO").with(user(sesion(rita))))
				.andExpect(status().isNotFound());
		mvc.perform(get("/agenda/turnos/{id}/cobro", turno.getId()).with(user(sesion(rita))))
				.andExpect(status().isNotFound());
		// Ni cargarle una atención sin turno a un barbero de otra barbería
		mvc.perform(post("/agenda/sin-turno").param("barbero", juan.getId().toString())
						.param("servicio", corte.getId().toString()).with(user(sesion(rita))).with(csrf()))
				.andExpect(status().isNotFound());
		assertThat(turnos.findById(turno.getId()).orElseThrow().getEstado()).isEqualTo(EstadoTurno.RESERVADO);
	}

	// ---- Ayudas ----

	private MockHttpServletRequestBuilder finalizar(Turno turno, Servicio servicio, String monto, String medio) {
		return post("/agenda/turnos/{id}/finalizar", turno.getId()).param("servicio", servicio.getId().toString())
				.param("monto", monto).param("medioPago", medio).with(csrf());
	}

	private MockHttpServletRequestBuilder corregir(Turno turno, Servicio servicio, String monto, String medio) {
		return post("/agenda/turnos/{id}/cobro", turno.getId()).param("servicio", servicio.getId().toString())
				.param("monto", monto).param("medioPago", medio).with(csrf());
	}

	private Turno sinTurnoDe(Usuario barbero) {
		return turnos.findAll().stream().filter(t -> t.isSinTurno() && t.getBarbero().getId().equals(barbero.getId()))
				.findFirst().orElseThrow();
	}

	private TurnoCambio ultimoCambio(Turno turno) {
		return cambios.historial(norte.getId(), turno.getId()).getLast();
	}

	private UsuarioAutenticado sesion(Usuario usuario) {
		return datos.sesion(usuario);
	}

}
