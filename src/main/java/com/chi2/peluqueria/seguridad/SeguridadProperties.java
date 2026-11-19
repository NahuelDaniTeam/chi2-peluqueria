package com.chi2.peluqueria.seguridad;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración de acceso ({@code chi2.seguridad.*} en application.properties).
 *
 * @param adminEmail       email del usuario de CHI2 que se crea al arrancar si todavía no existe
 * @param adminContrasena  su contraseña inicial
 * @param claveRecordarme  clave secreta para firmar la cookie de "mantener la sesión iniciada";
 *                         en producción tiene que ser larga y propia (variable CHI2_CLAVE_RECORDARME)
 */
@ConfigurationProperties("chi2.seguridad")
public record SeguridadProperties(String adminEmail, String adminContrasena, String claveRecordarme) {
}
