package com.chi2.peluqueria.turno;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

/**
 * Cuentas con tramos de un mismo día, en minutos desde las 00:00 (así no hay líos con la medianoche).
 * Un tramo es [desde, hasta): 9:00 a 9:30 y 9:30 a 10:00 no se pisan.
 */
final class Tramos {

	/** Un tramo del día, en minutos desde las 00:00. */
	record Tramo(int desde, int hasta) {

		Tramo {
			if (desde >= hasta) {
				throw new IllegalArgumentException("Tramo vacío: " + desde + "-" + hasta);
			}
		}

		static Tramo de(LocalTime desde, LocalTime hasta) {
			return new Tramo(minutos(desde), minutos(hasta));
		}

	}

	static final int MINUTOS_DEL_DIA = 24 * 60;

	private Tramos() {
	}

	static int minutos(LocalTime hora) {
		return hora.getHour() * 60 + hora.getMinute();
	}

	static LocalTime hora(int minutos) {
		return LocalTime.of(minutos / 60, minutos % 60);
	}

	/** Lo que está en los dos, ordenado. */
	static List<Tramo> interseccion(List<Tramo> a, List<Tramo> b) {
		List<Tramo> resultado = new ArrayList<>();
		for (Tramo x : a) {
			for (Tramo y : b) {
				int desde = Math.max(x.desde(), y.desde());
				int hasta = Math.min(x.hasta(), y.hasta());
				if (desde < hasta) {
					resultado.add(new Tramo(desde, hasta));
				}
			}
		}
		resultado.sort(Comparator.comparingInt(Tramo::desde));
		return resultado;
	}

	/** Lo que queda de {@code base} después de sacarle {@code quitar}, ordenado. */
	static List<Tramo> restar(List<Tramo> base, List<Tramo> quitar) {
		List<Tramo> resultado = new ArrayList<>(base);
		for (Tramo q : quitar) {
			List<Tramo> siguiente = new ArrayList<>();
			for (Tramo t : resultado) {
				if (q.hasta() <= t.desde() || q.desde() >= t.hasta()) {
					siguiente.add(t);
					continue;
				}
				if (t.desde() < q.desde()) {
					siguiente.add(new Tramo(t.desde(), q.desde()));
				}
				if (q.hasta() < t.hasta()) {
					siguiente.add(new Tramo(q.hasta(), t.hasta()));
				}
			}
			resultado = siguiente;
		}
		resultado.sort(Comparator.comparingInt(Tramo::desde));
		return resultado;
	}

	/**
	 * Horarios en que puede empezar algo de {@code duracion} minutos dentro de los tramos libres: cada
	 * múltiplo de {@code paso} (9:00, 9:15, 9:30…) y además el comienzo de cada tramo libre, para que
	 * después de un turno de 20 minutos se pueda empezar justo cuando termina (9:20).
	 *
	 * @param noAntesDe minuto desde el que se puede empezar (para hoy, la hora actual)
	 */
	static List<Integer> inicios(List<Tramo> libres, int duracion, int paso, int noAntesDe) {
		TreeSet<Integer> inicios = new TreeSet<>();
		for (Tramo libre : libres) {
			int primero = Math.max(libre.desde(), noAntesDe);
			if (primero == libre.desde()) {
				agregarSiEntra(inicios, libre, primero, duracion);
			}
			int enLaGrilla = (primero + paso - 1) / paso * paso;
			for (int m = enLaGrilla; m + duracion <= libre.hasta(); m += paso) {
				inicios.add(m);
			}
		}
		return List.copyOf(inicios);
	}

	/** Si algo de {@code duracion} minutos que empieza en {@code inicio} entra completo en alguno de los tramos. */
	static boolean entra(List<Tramo> libres, int inicio, int duracion) {
		return libres.stream().anyMatch(t -> t.desde() <= inicio && inicio + duracion <= t.hasta());
	}

	private static void agregarSiEntra(TreeSet<Integer> inicios, Tramo libre, int inicio, int duracion) {
		if (inicio + duracion <= libre.hasta()) {
			inicios.add(inicio);
		}
	}

}
