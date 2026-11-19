package com.chi2.peluqueria.horario;

import java.time.LocalDate;

import com.chi2.peluqueria.PruebaIntegracion;
import com.chi2.peluqueria.negocio.Negocio;
import com.chi2.peluqueria.usuario.Usuario;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AusenciasTest extends PruebaIntegracion {

	// Lejos en el futuro para no depender del día en que corre el test
	static final LocalDate DESDE = LocalDate.now().plusDays(30);
	static final LocalDate HASTA = DESDE.plusDays(4);

	@Autowired
	AusenciaRepository ausencias;

	@Test
	void elDuenoCargaUnCierreDelLocal() throws Exception {
		Negocio norte = datos.negocio("Barbería Norte");
		Usuario ana = datos.dueno(norte, "Ana");

		mvc.perform(post("/barberia/cierres").with(user(datos.sesion(ana))).with(csrf())
						.param("desde", DESDE.toString()).param("hasta", DESDE.toString()).param("motivo", "Feriado"))
				.andExpect(redirectedUrl("/barberia/cierres"));

		mvc.perform(get("/barberia/cierres").with(user(datos.sesion(ana))))
				.andExpect(content().string(containsString("Feriado")));
		assertThat(ausencias.findAllByNegocioIdAndUsuarioIdIsNullAndHastaGreaterThanEqualOrderByDesdeAsc(
				norte.getId(), LocalDate.now())).hasSize(1);
	}

	@Test
	void validaLasFechas() throws Exception {
		Usuario ana = datos.dueno(datos.negocio("Barbería Norte"), "Ana");

		mvc.perform(post("/barberia/cierres").with(user(datos.sesion(ana))).with(csrf())
						.param("desde", HASTA.toString()).param("hasta", DESDE.toString()))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Tiene que ser el mismo día o después")));
		mvc.perform(post("/barberia/cierres").with(user(datos.sesion(ana))).with(csrf())
						.param("desde", "2020-01-01").param("hasta", "2020-01-02"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Esa fecha ya pasó.")));
	}

	@Test
	void vacacionesDeUnBarberoYBorrarlas() throws Exception {
		Negocio norte = datos.negocio("Barbería Norte");
		Usuario ana = datos.dueno(norte, "Ana");
		Usuario juan = datos.barbero(norte, "Juan");

		mvc.perform(post("/equipo/{id}/ausencias", juan.getId()).with(user(datos.sesion(ana))).with(csrf())
						.param("desde", DESDE.toString()).param("hasta", HASTA.toString()).param("motivo", "Vacaciones"))
				.andExpect(redirectedUrl("/equipo/" + juan.getId() + "/ausencias"));

		Ausencia vacaciones = ausencias.findAllByNegocioIdAndUsuarioIdAndHastaGreaterThanEqualOrderByDesdeAsc(
				norte.getId(), juan.getId(), LocalDate.now()).getFirst();
		assertThat(vacaciones.getDesde()).isEqualTo(DESDE);
		assertThat(vacaciones.getHasta()).isEqualTo(HASTA);

		mvc.perform(post("/equipo/{id}/ausencias/{a}/borrar", juan.getId(), vacaciones.getId())
						.with(user(datos.sesion(ana))).with(csrf()))
				.andExpect(redirectedUrl("/equipo/" + juan.getId() + "/ausencias"));
		assertThat(ausencias.findById(vacaciones.getId())).isEmpty();
	}

	@Test
	void unaAusenciaDeUnBarberoNoSeBorraDesdeLaRutaDelLocal() throws Exception {
		Negocio norte = datos.negocio("Barbería Norte");
		Usuario ana = datos.dueno(norte, "Ana");
		Usuario juan = datos.barbero(norte, "Juan");
		Ausencia deJuan = ausencias.save(new Ausencia(norte.getId(), juan.getId(), DESDE, HASTA, null));

		mvc.perform(post("/barberia/cierres/{a}/borrar", deJuan.getId()).with(user(datos.sesion(ana))).with(csrf()))
				.andExpect(status().isNotFound());
		assertThat(ausencias.findById(deJuan.getId())).isPresent();
	}

	@Test
	void unDuenoNoVeNiBorraAusenciasDeOtraBarberia() throws Exception {
		Usuario ana = datos.dueno(datos.negocio("Barbería Norte"), "Ana");
		Negocio sur = datos.negocio("Barbería Sur");
		Usuario pedro = datos.barbero(sur, "Pedro");
		Ausencia cierreSur = ausencias.save(new Ausencia(sur.getId(), null, DESDE, HASTA, "Cierre del sur"));

		mvc.perform(get("/barberia/cierres").with(user(datos.sesion(ana))))
				.andExpect(content().string(org.hamcrest.Matchers.not(containsString("Cierre del sur"))));
		mvc.perform(post("/barberia/cierres/{a}/borrar", cierreSur.getId()).with(user(datos.sesion(ana))).with(csrf()))
				.andExpect(status().isNotFound());
		mvc.perform(get("/equipo/{id}/ausencias", pedro.getId()).with(user(datos.sesion(ana))))
				.andExpect(status().isNotFound());

		assertThat(ausencias.findById(cierreSur.getId())).isPresent();
	}

}
