package com.chi2.peluqueria.horario;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;

import com.chi2.peluqueria.comun.Formatos;

import org.springframework.format.annotation.DateTimeFormat;

/** Horario de una semana: un renglón por día, con hasta dos turnos (ej. 9 a 13 y 16 a 20). */
public class HorarioSemanalForm {

	private List<Dia> dias = new ArrayList<>();

	/** Horario sugerido para arrancar: lunes a sábado de 10 a 20. */
	public static HorarioSemanalForm sugerido() {
		HorarioSemanalForm form = new HorarioSemanalForm();
		for (int d = 1; d <= 7; d++) {
			Dia dia = new Dia(d);
			if (d <= 6) {
				dia.setAbierto(true);
				dia.setDesde1(LocalTime.of(10, 0));
				dia.setHasta1(LocalTime.of(20, 0));
			}
			form.dias.add(dia);
		}
		return form;
	}

	public static HorarioSemanalForm de(List<FranjaHoraria> franjas) {
		HorarioSemanalForm form = new HorarioSemanalForm();
		for (int d = 1; d <= 7; d++) {
			Dia dia = new Dia(d);
			int numero = d;
			List<FranjaHoraria> delDia = franjas.stream().filter(f -> f.getDiaSemana() == numero).toList();
			if (!delDia.isEmpty()) {
				dia.setAbierto(true);
				dia.setDesde1(delDia.get(0).getDesde());
				dia.setHasta1(delDia.get(0).getHasta());
			}
			if (delDia.size() > 1) {
				dia.setDesde2(delDia.get(1).getDesde());
				dia.setHasta2(delDia.get(1).getHasta());
			}
			form.dias.add(dia);
		}
		return form;
	}

	public List<Dia> getDias() {
		return dias;
	}

	public void setDias(List<Dia> dias) {
		this.dias = dias;
	}

	public static class Dia {

		/** 1 = lunes … 7 = domingo. */
		private int numero;
		private boolean abierto;
		@DateTimeFormat(pattern = "HH:mm")
		private LocalTime desde1;
		@DateTimeFormat(pattern = "HH:mm")
		private LocalTime hasta1;
		/** Segundo turno, opcional. */
		@DateTimeFormat(pattern = "HH:mm")
		private LocalTime desde2;
		@DateTimeFormat(pattern = "HH:mm")
		private LocalTime hasta2;

		public Dia() {
		}

		Dia(int numero) {
			this.numero = numero;
		}

		public String getNombre() {
			String nombre = DayOfWeek.of(numero).getDisplayName(TextStyle.FULL, Formatos.ARGENTINA);
			return Character.toUpperCase(nombre.charAt(0)) + nombre.substring(1);
		}

		public int getNumero() {
			return numero;
		}

		public void setNumero(int numero) {
			this.numero = numero;
		}

		public boolean isAbierto() {
			return abierto;
		}

		public void setAbierto(boolean abierto) {
			this.abierto = abierto;
		}

		public LocalTime getDesde1() {
			return desde1;
		}

		public void setDesde1(LocalTime desde1) {
			this.desde1 = desde1;
		}

		public LocalTime getHasta1() {
			return hasta1;
		}

		public void setHasta1(LocalTime hasta1) {
			this.hasta1 = hasta1;
		}

		public LocalTime getDesde2() {
			return desde2;
		}

		public void setDesde2(LocalTime desde2) {
			this.desde2 = desde2;
		}

		public LocalTime getHasta2() {
			return hasta2;
		}

		public void setHasta2(LocalTime hasta2) {
			this.hasta2 = hasta2;
		}

	}

}
