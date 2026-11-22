package com.chi2.peluqueria.comun;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Optional;

/** Cómo se muestran y se leen precios, duraciones, fechas y horas, a la manera argentina. */
public final class Formatos {

	public static final Locale ARGENTINA = Locale.forLanguageTag("es-AR");

	private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

	private Formatos() {
	}

	/** 12000 → "$ 12.000" */
	public static String precio(long pesos) {
		return "$ " + miles(pesos);
	}

	/** 12000 → "12.000" (para dejar un monto escrito en un campo, que ya muestra el "$") */
	public static String miles(long pesos) {
		return String.format(ARGENTINA, "%,d", pesos);
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

	/** "lun 24/11"; si no es de este año, "lun 24/11/2027". */
	public static String fechaCorta(LocalDate fecha) {
		String dia = fecha.getDayOfWeek().getDisplayName(TextStyle.SHORT, ARGENTINA).replace(".", "");
		String texto = dia + " " + fecha.getDayOfMonth() + "/" + fecha.getMonthValue();
		return fecha.getYear() == LocalDate.now().getYear() ? texto : texto + "/" + fecha.getYear();
	}

	/** "jueves 20 de noviembre"; si no es de este año, "jueves 20 de noviembre de 2027". */
	public static String fechaLarga(LocalDate fecha) {
		String texto = fecha.getDayOfWeek().getDisplayName(TextStyle.FULL, ARGENTINA) + " " + fecha.getDayOfMonth()
				+ " de " + fecha.getMonth().getDisplayName(TextStyle.FULL, ARGENTINA);
		return fecha.getYear() == LocalDate.now().getYear() ? texto : texto + " de " + fecha.getYear();
	}

	/** 9:05 → "09:05" */
	public static String hora(LocalTime hora) {
		return HORA.format(hora);
	}

	/** "jue 20/11 10:00" */
	public static String fechaHora(LocalDateTime momento) {
		return fechaCorta(momento.toLocalDate()) + " " + hora(momento.toLocalTime());
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
