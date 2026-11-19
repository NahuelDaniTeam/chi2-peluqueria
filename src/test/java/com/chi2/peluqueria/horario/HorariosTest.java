package com.chi2.peluqueria.horario;

import java.time.LocalTime;
import java.util.Map;

import com.chi2.peluqueria.PruebaIntegracion;
import com.chi2.peluqueria.negocio.Negocio;
import com.chi2.peluqueria.usuario.Usuario;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HorariosTest extends PruebaIntegracion {

	@Autowired
	FranjaHorariaRepository franjas;

	@Test
	void elDuenoCargaElHorarioDelLocalConCorteAlMediodia() throws Exception {
		Negocio norte = datos.negocio("Barbería Norte");
		Usuario ana = datos.dueno(norte, "Ana");

		mvc.perform(semana(post("/barberia/horario"), Map.of(
						1, new String[] { "09:00", "13:00", "16:00", "20:00" },
						6, new String[] { "09:00", "14:00", "", "" }))
						.with(user(datos.sesion(ana))).with(csrf()))
				.andExpect(redirectedUrl("/barberia"));

		assertThat(franjas.findAllByNegocioIdAndUsuarioIdIsNullOrderByDiaSemanaAscDesdeAsc(norte.getId()))
				.extracting(FranjaHoraria::getDiaSemana, FranjaHoraria::getDesde, FranjaHoraria::getHasta)
				.containsExactly(
						tuple(1, LocalTime.of(9, 0), LocalTime.of(13, 0)),
						tuple(1, LocalTime.of(16, 0), LocalTime.of(20, 0)),
						tuple(6, LocalTime.of(9, 0), LocalTime.of(14, 0)));
	}

	@Test
	void validaLasHoras() throws Exception {
		Usuario ana = datos.dueno(datos.negocio("Barbería Norte"), "Ana");

		mvc.perform(semana(post("/barberia/horario"), Map.of(
						1, new String[] { "20:00", "09:00", "", "" },
						2, new String[] { "09:00", "13:00", "12:00", "20:00" },
						3, new String[] { "09:00", "13:00", "16:00", "" }))
						.with(user(datos.sesion(ana))).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("La hora de cierre tiene que ser después de la de apertura.")))
				.andExpect(content().string(containsString("El segundo turno tiene que empezar después")))
				.andExpect(content().string(containsString("Completá las dos horas del segundo turno")));
	}

	@Test
	void pideAlMenosUnDia() throws Exception {
		Usuario ana = datos.dueno(datos.negocio("Barbería Norte"), "Ana");

		mvc.perform(semana(post("/barberia/horario"), Map.of()).with(user(datos.sesion(ana))).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Marcá al menos un día")));
	}

	@Test
	void unBarberoPuedeTenerHorarioPropioYVolverAlDelLocal() throws Exception {
		Negocio norte = datos.negocio("Barbería Norte");
		Usuario ana = datos.dueno(norte, "Ana");
		Usuario juan = datos.barbero(norte, "Juan");

		mvc.perform(semana(post("/equipo/{id}/horario", juan.getId()), Map.of(
						2, new String[] { "14:00", "20:00", "", "" }))
						.with(user(datos.sesion(ana))).with(csrf()))
				.andExpect(redirectedUrl("/equipo/" + juan.getId()));
		assertThat(franjas.findAllByNegocioIdAndUsuarioIdOrderByDiaSemanaAscDesdeAsc(norte.getId(), juan.getId()))
				.extracting(FranjaHoraria::getDiaSemana, FranjaHoraria::getDesde)
				.containsExactly(tuple(2, LocalTime.of(14, 0)));
		mvc.perform(get("/equipo/{id}", juan.getId()).with(user(datos.sesion(ana))))
				.andExpect(content().string(containsString("Tiene un horario propio.")));

		mvc.perform(post("/equipo/{id}/horario/usar-el-de-la-barberia", juan.getId())
						.with(user(datos.sesion(ana))).with(csrf()))
				.andExpect(redirectedUrl("/equipo/" + juan.getId()));
		assertThat(franjas.existsByNegocioIdAndUsuarioId(norte.getId(), juan.getId())).isFalse();
	}

	@Test
	void elFormularioDeUnBarberoArrancaConElHorarioDelLocal() throws Exception {
		Negocio norte = datos.negocio("Barbería Norte");
		Usuario ana = datos.dueno(norte, "Ana");
		Usuario juan = datos.barbero(norte, "Juan");
		franjas.save(new FranjaHoraria(norte.getId(), null, 4, LocalTime.of(11, 15), LocalTime.of(19, 45)));

		mvc.perform(get("/equipo/{id}/horario", juan.getId()).with(user(datos.sesion(ana))))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("11:15")))
				.andExpect(content().string(containsString("Hoy trabaja en el horario de la barbería.")));
	}

	@Test
	void unDuenoNoVeNiTocaElHorarioDeAlguienDeOtraBarberia() throws Exception {
		Usuario ana = datos.dueno(datos.negocio("Barbería Norte"), "Ana");
		Usuario pedro = datos.barbero(datos.negocio("Barbería Sur"), "Pedro");

		mvc.perform(get("/equipo/{id}/horario", pedro.getId()).with(user(datos.sesion(ana))))
				.andExpect(status().isNotFound());
		mvc.perform(semana(post("/equipo/{id}/horario", pedro.getId()), Map.of(1, new String[] { "09:00", "18:00", "", "" }))
						.with(user(datos.sesion(ana))).with(csrf()))
				.andExpect(status().isNotFound());
	}

	@Test
	void unBarberoNoTocaElHorarioDelLocal() throws Exception {
		Usuario juan = datos.barbero(datos.negocio("Barbería Norte"), "Juan");

		mvc.perform(get("/barberia/horario").with(user(datos.sesion(juan)))).andExpect(status().isForbidden());
	}

	/** Arma el POST de la semana: los días del mapa van abiertos con {desde1, hasta1, desde2, hasta2}. */
	private MockHttpServletRequestBuilder semana(MockHttpServletRequestBuilder pedido, Map<Integer, String[]> abiertos) {
		for (int d = 1; d <= 7; d++) {
			int i = d - 1;
			pedido.param("dias[" + i + "].numero", String.valueOf(d));
			String[] horas = abiertos.get(d);
			if (horas != null) {
				pedido.param("dias[" + i + "].abierto", "true")
						.param("dias[" + i + "].desde1", horas[0])
						.param("dias[" + i + "].hasta1", horas[1])
						.param("dias[" + i + "].desde2", horas[2])
						.param("dias[" + i + "].hasta2", horas[3]);
			}
		}
		return pedido;
	}

}
