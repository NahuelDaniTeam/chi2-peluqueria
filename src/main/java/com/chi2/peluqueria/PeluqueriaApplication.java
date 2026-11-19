package com.chi2.peluqueria;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PeluqueriaApplication {

	public static void main(String[] args) {
		// Todo en UTC: los turnos se guardan en UTC y se convierten a la hora de cada barbería solo al mostrarlos.
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		SpringApplication.run(PeluqueriaApplication.class, args);
	}

}
