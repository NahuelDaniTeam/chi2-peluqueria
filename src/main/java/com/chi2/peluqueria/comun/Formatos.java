package com.chi2.peluqueria.comun;

import java.util.Locale;
import java.util.Optional;

/** Cómo se muestran y se leen precios y duraciones, a la manera argentina. */
public final class Formatos {

	public static final Locale ARGENTINA = Locale.forLanguageTag("es-AR");

	private Formatos() {
	}

	/** 12000 → "$ 12.000" */
	public static String precio(int pesos) {
		return "$ " + String.format(ARGENTINA, "%,d", pesos);
	}

	/**
	 * Lee un precio en pesos tal como lo escribe la gente: "12000", "12.000", "$ 12.000".
	 * Vacío o con centavos ("12000,50") no es válido.
	 */
	public static Optional<Integer> leerPrecio(String texto) {
		if (texto == null) {
			return Optional.empty();
		}
		String limpio = texto.replace("$", "").replace(" ", "").replace(".", "").trim();
		if (!limpio.matches("\\d{1,9}")) {
			return Optional.empty();
		}
		return Optional.of(Integer.parseInt(limpio));
	}

	/** 30 → "30 min", 60 → "1 h", 90 → "1 h 30 min" */
	public static String duracion(int minutos) {
		int horas = minutos / 60;
		int resto = minutos % 60;
		if (horas == 0) {
			return resto + " min";
		}
		return resto == 0 ? horas + " h" : horas + " h " + resto + " min";
	}

}
