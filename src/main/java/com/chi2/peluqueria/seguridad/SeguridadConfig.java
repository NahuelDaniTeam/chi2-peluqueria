package com.chi2.peluqueria.seguridad;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Quién puede entrar a qué. Las rutas nuevas quedan protegidas por defecto (requieren sesión);
 * si una ruta es solo para dueños o para CHI2, se agrega acá.
 */
@Configuration
class SeguridadConfig {

	private static final Duration DURACION_RECORDARME = Duration.ofDays(30);

	@Bean
	SecurityFilterChain cadenaDeSeguridad(HttpSecurity http, UserDetailsService usuarios,
			SeguridadProperties propiedades) throws Exception {
		http
				.authorizeHttpRequests(rutas -> rutas
						.requestMatchers("/login", "/error", "/css/**", "/favicon.svg", "/apple-touch-icon.png",
								"/actuator/health", "/actuator/health/**").permitAll()
						.requestMatchers("/admin/**").hasRole("ADMIN")
						.requestMatchers("/equipo/**", "/barberia/**").hasRole("DUENO")
						.anyRequest().authenticated())
				.formLogin(login -> login
						.loginPage("/login")
						.usernameParameter("email")
						.passwordParameter("contrasena")
						.defaultSuccessUrl("/", true)
						.permitAll())
				// En el celular nadie quiere volver a loguearse todos los días
				.rememberMe(recordarme -> recordarme
						.key(propiedades.claveRecordarme())
						.userDetailsService(usuarios)
						.rememberMeParameter("recordarme")
						.tokenValiditySeconds((int) DURACION_RECORDARME.toSeconds()))
				.logout(salir -> salir
						.logoutSuccessUrl("/login?salir")
						.permitAll());
		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

}
