package com.chi2.peluqueria.negocio;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

/**
 * Arma el identificador del link público de una barbería: "Barbería Juan" → "barberia-juan".
 * Como el link va a vivir en la raíz del sitio (turnos.chi2.com/barberia-juan, etapa 6), algunos
 * nombres quedan reservados para no pisar rutas del sistema.
 */
public final class Slugs {

	public static final int LARGO_MAXIMO = 60;

	private static final Set<String> RESERVADOS = Set.of(
			"login", "logout", "admin", "equipo", "cuenta", "api", "actuator", "error",
			"css", "js", "img", "static", "favicon.svg", "apple-touch-icon.png",
			"turnos", "reservar", "agenda", "metricas", "chi2");

	private Slugs() {
	}

	public static String desde(String texto) {
		String sinTildes = Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "");
		String slug = sinTildes.toLowerCase(Locale.ROOT)
				.replaceAll("[^a-z0-9]+", "-")
				.replaceAll("^-+|-+$", "");
		if (slug.length() > LARGO_MAXIMO) {
			slug = slug.substring(0, LARGO_MAXIMO).replaceAll("-+$", "");
		}
		return slug;
	}

	public static boolean esReservado(String slug) {
		return RESERVADOS.contains(slug);
	}

}
