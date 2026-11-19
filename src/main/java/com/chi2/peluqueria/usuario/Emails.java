package com.chi2.peluqueria.usuario;

import java.util.Locale;

/** Los emails se guardan y se buscan siempre normalizados: sin espacios y en minúsculas. */
public final class Emails {

	private Emails() {
	}

	public static String normalizar(String email) {
		return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
	}

}
