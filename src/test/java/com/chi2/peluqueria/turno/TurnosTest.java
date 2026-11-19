package com.chi2.peluqueria.turno;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.chi2.peluqueria.DatosDePrueba;
import com.chi2.peluqueria.PruebaIntegracion;
import com.chi2.peluqueria.negocio.Negocio;
import com.chi2.peluqueria.seguridad.UsuarioAutenticado;
import com.chi2.peluqueria.servicio.Servicio;
import com.chi2.peluqueria.usuario.Usuario;
import com.chi2.peluqueria.usuario.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static com.chi2.peluqueria.DatosDePrueba.instante;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TurnosTest extends PruebaIntegracion {

	@Autowired
	TurnoRepository turnos;

	@Autowired
	TurnoCambioRepository cambios;

	@Autowired
	UsuarioRepository usuarios;

	Negocio norte;
	Usuario ana;
	Usuario juan;
	Usuario pedro;
	Servicio corte;
	LocalDate lunes = DatosDePrueba.dentroDeUnasSemanas(DayOfWeek.MONDAY);

	@BeforeEach
	void barberiaQueAbreLosLunes() {
		norte = datos.negocio("Barbería Norte");
		ana = datos.dueno(norte, "Ana");
		juan = datos.barbero(norte, "Juan");
		pedro = datos.barbero(norte, "Pedro");
		corte = datos.servicio(norte, "Corte", 30, 12000);
		datos.hace(juan, corte);
		datos.hace(pedro, corte);
		datos.franja(norte, null, DayOfWeek.MONDAY, "09:00", "13:00");
	}

	// ---- Reservar ----

	@Test
	void elDuenoCargaUnTurnoYQuedaEnLaAgenda() throws Exception {
		mvc.perform(reserva(juan.getId(), "10:00", "Martín").with(user(sesion(ana))).with(csrf()))
				.andExpect(redirectedUrl("/agenda?fecha=" + lunes))
				.andExpect(flash().attribute("aviso", containsString("Turno reservado: Martín")));

		Turno turno = turnos.findAll().stream().filter(t -> t.getNegocioId().equals(norte.getId())).findFirst().orElseThrow();
		// Se guarda en UTC: las 10 de Buenos Aires son las 13 UTC
		assertThat(turno.getInicio()).isEqualTo(instante(lunes, "10:00"));
		assertThat(turno.getFin()).isEqualTo(instante(lunes, "10:30"));
		assertThat(turno.getBarbero().getId()).isEqualTo(juan.getId());
		assertThat(turno.getEstado()).isEqualTo(EstadoTurno.RESERVADO);
		assertThat(cambios.historial(norte.getId(), turno.getId())).extracting(TurnoCambio::getAccion)
				.containsExactly(TurnoCambio.Accion.CREADO);

		mvc.perform(get("/agenda").param("fecha", lunes.toString()).with(user(sesion(ana))))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Martín")))
				.andExpect(content().string(containsString("10:00")));
	}

	@Test
	void conCualquieraSeLoDaAlQueTieneMenosTurnosEseDia() throws Exception {
		datos.turno(juan, corte, lunes, "09:00", "Lucas");

		mvc.perform(reserva(null, "11:00", "Martín").with(user(sesion(ana))).with(csrf()))
				.andExpect(status().is3xxRedirection());

		assertThat(turnoDe("Martín").getBarbero().getId()).isEqualTo(pedro.getId());
	}

	@Test
	void noDejaReservarUnHorarioOcupado() throws Exception {
		datos.turno(juan, corte, lunes, "10:00", "Lucas");

		mvc.perform(reserva(juan.getId(), "10:15", "Martín").with(user(sesion(ana))).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString(AgendaService.HORARIO_OCUPADO)));
		mvc.perform(reserva(juan.getId(), "13:00", "Martín").with(user(sesion(ana))).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString(AgendaService.HORARIO_OCUPADO)));

		assertThat(turnos.findAll()).extracting(Turno::getClienteNombre).doesNotContain("Martín");
	}

	@Test
	void pideElNombreDelClienteYUnHorario() throws Exception {
		mvc.perform(post("/agenda/nuevo").param("servicio", corte.getId().toString()).param("fecha", lunes.toString())
						.param("nombre", " ").with(user(sesion(ana))).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Poné el nombre del cliente.")))
				.andExpect(content().string(containsString("Elegí un horario.")));
	}

	@Test
	void laBaseImpideDosTurnosSuperpuestosDelMismoBarbero() {
		datos.turno(juan, corte, lunes, "10:00", "Lucas");
		turnos.flush();

		assertThatThrownBy(() -> turnos.saveAndFlush(new Turno(norte.getId(), juan, corte, instante(lunes, "10:15"),
				Duration.ofMinutes(30), "Martín", null, null, null)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void laPantallaDeNuevoTurnoMuestraLosHorariosLibres() throws Exception {
		datos.turno(juan, corte, lunes, "10:00", "Lucas");

		mvc.perform(get("/agenda/nuevo").param("servicio", corte.getId().toString()).param("fecha", lunes.toString())
						.with(user(sesion(ana))))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Cualquiera")))
				.andExpect(content().string(containsString("value=\"10:00\"")));
		mvc.perform(get("/agenda/nuevo").param("servicio", corte.getId().toString()).param("fecha", lunes.toString())
						.param("barbero", juan.getId().toString()).with(user(sesion(ana))))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("value=\"09:30\"")))
				.andExpect(content().string(not(containsString("value=\"10:00\""))));
	}

	// ---- Cancelar ----

	@Test
	void cancelarConTiempoLiberaElHorario() throws Exception {
		Turno turno = datos.turno(juan, corte, lunes, "10:00", "Lucas");

		mvc.perform(post("/agenda/turnos/{id}/cancelar", turno.getId()).with(user(sesion(ana))).with(csrf()))
				.andExpect(redirectedUrl("/agenda/turnos/" + turno.getId()))
				.andExpect(flash().attribute("aviso", containsString("El horario quedó libre.")));
		Turno cancelado = turnos.findById(turno.getId()).orElseThrow();
		assertThat(cancelado.getEstado()).isEqualTo(EstadoTurno.CANCELADO);
		assertThat(cancelado.isCancelacionTardia()).isFalse();

		mvc.perform(reserva(juan.getId(), "10:00", "Martín").with(user(sesion(ana))).with(csrf()))
				.andExpect(status().is3xxRedirection());
	}

	@Test
	void cancelarConMenosDe24HorasQuedaComoTardia() throws Exception {
		Turno turno = datos.turno(juan, corte, Instant.now().plus(2, ChronoUnit.HOURS), "Lucas");

		mvc.perform(post("/agenda/turnos/{id}/cancelar", turno.getId()).with(user(sesion(ana))).with(csrf()))
				.andExpect(flash().attribute("aviso", containsString("cancelación tardía")));

		assertThat(turnos.findById(turno.getId()).orElseThrow().isCancelacionTardia()).isTrue();
		mvc.perform(get("/agenda/turnos/{id}", turno.getId()).with(user(sesion(ana))))
				.andExpect(content().string(containsString("Cancelado tarde")))
				.andExpect(content().string(containsString("Con menos de 24 h de aviso.")));
	}

	@Test
	void unTurnoCanceladoNoSeVuelveACancelar() throws Exception {
		Turno turno = datos.turno(juan, corte, lunes, "10:00", "Lucas");
		turno.cancelar(false);

		mvc.perform(post("/agenda/turnos/{id}/cancelar", turno.getId()).with(user(sesion(ana))).with(csrf()))
				.andExpect(status().isBadRequest());
	}

	// ---- Reprogramar ----

	@Test
	void reprogramarMueveElTurnoYQuedaEnElHistorial() throws Exception {
		Turno turno = datos.turno(juan, corte, lunes, "10:00", "Lucas");

		mvc.perform(post("/agenda/turnos/{id}/reprogramar", turno.getId()).param("barbero", pedro.getId().toString())
						.param("fecha", lunes.toString()).param("hora", "11:00").with(user(sesion(ana))).with(csrf()))
				.andExpect(redirectedUrl("/agenda/turnos/" + turno.getId()))
				.andExpect(flash().attribute("aviso", containsString("con Pedro")));

		Turno movido = turnos.findById(turno.getId()).orElseThrow();
		assertThat(movido.getInicio()).isEqualTo(instante(lunes, "11:00"));
		assertThat(movido.getFin()).isEqualTo(instante(lunes, "11:30"));
		assertThat(movido.getBarbero().getId()).isEqualTo(pedro.getId());
		mvc.perform(get("/agenda/turnos/{id}", turno.getId()).with(user(sesion(ana))))
				.andExpect(content().string(containsString("Reprogramado")))
				.andExpect(content().string(containsString("10:00 con Juan.")));
	}

	@Test
	void reprogramarUnPocoMasTardeNoChocaConsigoMismo() throws Exception {
		Turno turno = datos.turno(juan, corte, lunes, "10:00", "Lucas");

		mvc.perform(post("/agenda/turnos/{id}/reprogramar", turno.getId()).param("fecha", lunes.toString())
						.param("hora", "10:15").with(user(sesion(ana))).with(csrf()))
				.andExpect(redirectedUrl("/agenda/turnos/" + turno.getId()));

		assertThat(turnos.findById(turno.getId()).orElseThrow().getInicio()).isEqualTo(instante(lunes, "10:15"));
	}

	// ---- Qué ve cada uno ----

	@Test
	void unBarberoVeYTocaSoloSusTurnos() throws Exception {
		datos.turno(juan, corte, lunes, "10:00", "Lucas");
		Turno dePedro = datos.turno(pedro, corte, lunes, "10:00", "Bruno");

		mvc.perform(get("/agenda").param("fecha", lunes.toString()).with(user(sesion(juan))))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Lucas")))
				.andExpect(content().string(not(containsString("Bruno"))));
		mvc.perform(get("/agenda/turnos/{id}", dePedro.getId()).with(user(sesion(juan))))
				.andExpect(status().isNotFound());
		mvc.perform(post("/agenda/turnos/{id}/cancelar", dePedro.getId()).with(user(sesion(juan))).with(csrf()))
				.andExpect(status().isNotFound());
		mvc.perform(reserva(pedro.getId(), "11:00", "Martín").with(user(sesion(juan))).with(csrf()))
				.andExpect(status().isBadRequest());
	}

	@Test
	void unBarberoSeCargaTurnosASiMismo() throws Exception {
		mvc.perform(reserva(null, "11:00", "Martín").with(user(sesion(pedro))).with(csrf()))
				.andExpect(status().is3xxRedirection());

		assertThat(turnoDe("Martín").getBarbero().getId()).isEqualTo(pedro.getId());
	}

	@Test
	void unDuenoNoVeNiTocaTurnosDeOtraBarberia() throws Exception {
		Turno turno = datos.turno(juan, corte, lunes, "10:00", "Lucas");
		Negocio sur = datos.negocio("Barbería Sur");
		Usuario rita = datos.dueno(sur, "Rita");

		mvc.perform(get("/agenda/turnos/{id}", turno.getId()).with(user(sesion(rita)))).andExpect(status().isNotFound());
		mvc.perform(post("/agenda/turnos/{id}/cancelar", turno.getId()).with(user(sesion(rita))).with(csrf()))
				.andExpect(status().isNotFound());
		mvc.perform(post("/agenda/turnos/{id}/reprogramar", turno.getId()).param("fecha", lunes.toString())
						.param("hora", "11:00").with(user(sesion(rita))).with(csrf()))
				.andExpect(status().isNotFound());
		mvc.perform(get("/agenda").param("fecha", lunes.toString()).with(user(sesion(rita))))
				.andExpect(content().string(not(containsString("Lucas"))));
		// Ni reservar con un servicio de otra barbería
		mvc.perform(reserva(null, "11:00", "Martín").with(user(sesion(rita))).with(csrf()))
				.andExpect(status().isNotFound());
		assertThat(turnos.findById(turno.getId()).orElseThrow().getEstado()).isEqualTo(EstadoTurno.RESERVADO);
	}

	@Test
	void chi2NoEntraALaAgenda() throws Exception {
		UsuarioAutenticado admin = UsuarioAutenticado.de(usuarios.buscarParaLogin(ADMIN_EMAIL).orElseThrow());

		mvc.perform(get("/agenda").with(user(admin))).andExpect(status().isForbidden());
	}

	// ---- Lo que la agenda frena o avisa ----

	@Test
	void nadieSaleDeLaAgendaConTurnosReservados() throws Exception {
		datos.turno(juan, corte, lunes, "10:00", "Lucas");

		mvc.perform(post("/equipo/{id}/baja", juan.getId()).with(user(sesion(ana))).with(csrf()))
				.andExpect(redirectedUrl("/equipo"))
				.andExpect(flash().attribute("avisoError", containsString("Juan tiene 1 turno reservado")));
		mvc.perform(post("/equipo/{id}/atiende", juan.getId()).param("valor", "false").with(user(sesion(ana))).with(csrf()))
				.andExpect(flash().attribute("avisoError", containsString("Reprogramalos o cancelalos antes.")));

		Usuario igual = usuarios.findById(juan.getId()).orElseThrow();
		assertThat(igual.isActivo()).isTrue();
		assertThat(igual.isAtiende()).isTrue();
		// Pedro no tiene turnos: puede dejar de atender
		mvc.perform(post("/equipo/{id}/atiende", pedro.getId()).param("valor", "false").with(user(sesion(ana))).with(csrf()))
				.andExpect(flash().attribute("aviso", containsString("ya no atiende")));
	}

	@Test
	void cerrarUnDiaConTurnosDadosAvisa() throws Exception {
		datos.turno(juan, corte, lunes, "10:00", "Lucas");

		mvc.perform(post("/barberia/cierres").param("desde", lunes.toString()).param("hasta", lunes.toString())
						.with(user(sesion(ana))).with(csrf()))
				.andExpect(redirectedUrl("/barberia/cierres"))
				.andExpect(flash().attribute("avisoError", containsString("hay 1 turno reservado")));
	}

	// ---- Ayudas ----

	private MockHttpServletRequestBuilder reserva(Long barberoId, String hora, String cliente) {
		MockHttpServletRequestBuilder pedido = post("/agenda/nuevo").param("servicio", corte.getId().toString())
				.param("fecha", lunes.toString()).param("hora", hora).param("nombre", cliente);
		return barberoId == null ? pedido : pedido.param("barbero", barberoId.toString());
	}

	private Turno turnoDe(String cliente) {
		return turnos.findAll().stream().filter(t -> t.getClienteNombre().equals(cliente)).findFirst().orElseThrow();
	}

	private UsuarioAutenticado sesion(Usuario usuario) {
		return datos.sesion(usuario);
	}

}
