package com.chi2.peluqueria.metrica;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

import com.chi2.peluqueria.comun.Formatos;

/**
 * Los días que se miran juntos: un día, una semana (de lunes a domingo) o un mes. Siempre en la fecha de la
 * barbería, con los dos extremos incluidos.
 */
public record Periodo(Tipo tipo, LocalDate desde, LocalDate hasta) {

	public enum Tipo {

		DIA("Día"), SEMANA("Semana"), MES("Mes");

		private final String etiqueta;

		Tipo(String etiqueta) {
			this.etiqueta = etiqueta;
		}

		public String getEtiqueta() {
			return etiqueta;
		}

		/** "dia", "semana" o "mes" (lo que viaja en la URL). */
		public String getClave() {
			return name().toLowerCase();
		}

		/** Lo que viene en la URL; cualquier otra cosa es el mes. */
		public static Tipo desde(String clave) {
			for (Tipo t : values()) {
				if (t.getClave().equals(clave)) {
					return t;
				}
			}
			return MES;
		}

	}

	/** El período de ese tipo que contiene a {@code fecha}. */
	public static Periodo de(Tipo tipo, LocalDate fecha) {
		return switch (tipo) {
			case DIA -> new Periodo(tipo, fecha, fecha);
			case SEMANA -> {
				LocalDate lunes = fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
				yield new Periodo(tipo, lunes, lunes.plusDays(6));
			}
			case MES -> new Periodo(tipo, fecha.withDayOfMonth(1), fecha.with(TemporalAdjusters.lastDayOfMonth()));
		};
	}

	public Periodo anterior() {
		return de(tipo, desde.minusDays(1));
	}

	public Periodo siguiente() {
		return de(tipo, hasta.plusDays(1));
	}

	public boolean contiene(LocalDate fecha) {
		return !fecha.isBefore(desde) && !fecha.isAfter(hasta);
	}

	public List<LocalDate> dias() {
		List<LocalDate> dias = new ArrayList<>();
		for (LocalDate d = desde; !d.isAfter(hasta); d = d.plusDays(1)) {
			dias.add(d);
		}
		return dias;
	}

	/** "noviembre de 2026", "semana del 16 al 22 de noviembre" o "domingo 22 de noviembre". */
	public String getTitulo() {
		return switch (tipo) {
			case DIA -> Formatos.fechaLarga(desde);
			case SEMANA -> desde.getMonth() == hasta.getMonth()
					? "semana del " + desde.getDayOfMonth() + " al " + Formatos.fechaLarga(hasta).replaceFirst("^\\S+ ", "")
					: "semana del " + Formatos.fechaLarga(desde).replaceFirst("^\\S+ ", "") + " al "
							+ Formatos.fechaLarga(hasta).replaceFirst("^\\S+ ", "");
			case MES -> desde.getMonth().getDisplayName(TextStyle.FULL, Formatos.ARGENTINA) + " de " + desde.getYear();
		};
	}

	/** "Hoy", "Esta semana" o "Este mes" si el período incluye a {@code hoy}. */
	public String etiqueta(LocalDate hoy) {
		if (!contiene(hoy)) {
			return "Números";
		}
		return switch (tipo) {
			case DIA -> "Hoy";
			case SEMANA -> "Esta semana";
			case MES -> "Este mes";
		};
	}

}
