package com.chi2.peluqueria;

import org.springframework.boot.SpringApplication;

/** Levanta la app en local con un Postgres de Testcontainers (necesita Docker). */
public class TestPeluqueriaApplication {

	public static void main(String[] args) {
		SpringApplication.from(PeluqueriaApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
