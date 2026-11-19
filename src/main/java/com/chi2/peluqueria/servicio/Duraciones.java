package com.chi2.peluqueria.servicio;

import java.util.ArrayList;
import java.util.List;

import com.chi2.peluqueria.comun.Formatos;

/** Las duraciones que se ofrecen en el selector: de 5 en 5 hasta 2 h, después más espaciadas, hasta 8 h. */
public final class Duraciones {

	private static final List<Opcion> OPCIONES = armar();

	public record Opcion(int minutos, String texto) {
	}

	private Duraciones() {
	}

	public static List<Opcion> opciones() {
		return OPCIONES;
	}

	public static boolean esValida(Integer minutos) {
		return minutos != null && minutos >= 5 && minutos <= 480 && minutos % 5 == 0;
	}

	private static List<Opcion> armar() {
		List<Opcion> opciones = new ArrayList<>();
		for (int m = 5; m <= 120; m += 5) {
			opciones.add(new Opcion(m, Formatos.duracion(m)));
		}
		for (int m = 135; m <= 240; m += 15) {
			opciones.add(new Opcion(m, Formatos.duracion(m)));
		}
		for (int m = 270; m <= 480; m += 30) {
			opciones.add(new Opcion(m, Formatos.duracion(m)));
		}
		return List.copyOf(opciones);
	}

}
