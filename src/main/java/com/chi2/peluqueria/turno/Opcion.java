package com.chi2.peluqueria.turno;

/**
 * Algo que se elige tocando (un servicio, un barbero, un día): un link que recarga la pantalla con esa
 * opción elegida, sin JavaScript.
 *
 * @param detalle segunda línea, chica (ej. "30 min · $ 12.000" o "Cerrado"); puede ser {@code null}
 * @param apagada se ve pero no hay lugar (ej. un día cerrado)
 */
public record Opcion(String texto, String detalle, String enlace, boolean elegida, boolean apagada) {
}
