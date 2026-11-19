package com.chi2.peluqueria.barbero;

import java.util.ArrayList;
import java.util.List;

/** Los servicios que hace un barbero: una fila por servicio del catálogo. */
public class ServiciosBarberoForm {

	private List<Item> items = new ArrayList<>();

	public static ServiciosBarberoForm de(List<PerfilBarberoService.ServicioDelBarbero> servicios) {
		ServiciosBarberoForm form = new ServiciosBarberoForm();
		for (PerfilBarberoService.ServicioDelBarbero s : servicios) {
			Item item = new Item();
			item.setServicioId(s.servicio().getId());
			item.setHace(s.hace());
			item.setPrecioEspecial(s.precioEspecial() == null ? "" : String.valueOf(s.precioEspecial()));
			form.items.add(item);
		}
		return form;
	}

	public List<Item> getItems() {
		return items;
	}

	public void setItems(List<Item> items) {
		this.items = items;
	}

	public static class Item {

		private Long servicioId;
		private boolean hace;
		/** Vacío = cobra el precio del local. */
		private String precioEspecial;

		public Long getServicioId() {
			return servicioId;
		}

		public void setServicioId(Long servicioId) {
			this.servicioId = servicioId;
		}

		public boolean isHace() {
			return hace;
		}

		public void setHace(boolean hace) {
			this.hace = hace;
		}

		public String getPrecioEspecial() {
			return precioEspecial;
		}

		public void setPrecioEspecial(String precioEspecial) {
			this.precioEspecial = precioEspecial;
		}

	}

}
