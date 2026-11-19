package com.chi2.peluqueria;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.concurrent.atomic.AtomicInteger;

import com.chi2.peluqueria.barbero.BarberoServicio;
import com.chi2.peluqueria.barbero.BarberoServicioRepository;
import com.chi2.peluqueria.horario.Ausencia;
import com.chi2.peluqueria.horario.AusenciaRepository;
import com.chi2.peluqueria.horario.FranjaHoraria;
import com.chi2.peluqueria.horario.FranjaHorariaRepository;
import com.chi2.peluqueria.negocio.Negocio;
import com.chi2.peluqueria.negocio.NegocioRepository;
import com.chi2.peluqueria.negocio.Slugs;
import com.chi2.peluqueria.seguridad.UsuarioAutenticado;
import com.chi2.peluqueria.servicio.Servicio;
import com.chi2.peluqueria.servicio.ServicioRepository;
import com.chi2.peluqueria.turno.Turno;
import com.chi2.peluqueria.turno.TurnoRepository;
import com.chi2.peluqueria.usuario.Rol;
import com.chi2.peluqueria.usuario.Usuario;
import com.chi2.peluqueria.usuario.UsuarioRepository;

import org.springframework.security.crypto.password.PasswordEncoder;

/** Arma barberías, usuarios, horarios y turnos para los tests, con emails y links que no se repiten. */
public class DatosDePrueba {

	public static final String CONTRASENA = "clave-de-prueba";

	/** La zona de las barberías de prueba (la que tiene un negocio nuevo). */
	public static final ZoneId ZONA = ZoneId.of(Negocio.ZONA_HORARIA_POR_DEFECTO);

	private static final AtomicInteger SECUENCIA = new AtomicInteger();

	private final NegocioRepository negocios;
	private final UsuarioRepository usuarios;
	private final ServicioRepository servicios;
	private final BarberoServicioRepository barberoServicios;
	private final FranjaHorariaRepository franjas;
	private final AusenciaRepository ausencias;
	private final TurnoRepository turnos;
	private final PasswordEncoder passwordEncoder;
	private String contrasenaCodificada;

	DatosDePrueba(NegocioRepository negocios, UsuarioRepository usuarios, ServicioRepository servicios,
			BarberoServicioRepository barberoServicios, FranjaHorariaRepository franjas, AusenciaRepository ausencias,
			TurnoRepository turnos, PasswordEncoder passwordEncoder) {
		this.negocios = negocios;
		this.usuarios = usuarios;
		this.servicios = servicios;
		this.barberoServicios = barberoServicios;
		this.franjas = franjas;
		this.ausencias = ausencias;
		this.turnos = turnos;
		this.passwordEncoder = passwordEncoder;
	}

	public Negocio negocio(String nombre) {
		return negocios.save(new Negocio(nombre, Slugs.desde(nombre) + "-" + SECUENCIA.incrementAndGet()));
	}

	public Usuario dueno(Negocio negocio, String nombre) {
		return miembro(negocio, nombre, Rol.DUENO);
	}

	public Usuario barbero(Negocio negocio, String nombre) {
		return miembro(negocio, nombre, Rol.BARBERO);
	}

	public Servicio servicio(Negocio negocio, String nombre, int duracionMinutos, int precio) {
		return servicios.save(new Servicio(negocio.getId(), nombre, duracionMinutos, precio));
	}

	/** Que el barbero haga esos servicios (los datos de prueba no pasan por los eventos que lo hacen solo). */
	public void hace(Usuario barbero, Servicio... queHace) {
		for (Servicio s : queHace) {
			barberoServicios.save(new BarberoServicio(s.getNegocioId(), barbero.getId(), s.getId()));
		}
	}

	/** Una franja de atención; {@code usuario} nulo = del local. Horas como "09:00". */
	public void franja(Negocio negocio, Usuario usuario, DayOfWeek dia, String desde, String hasta) {
		franjas.save(new FranjaHoraria(negocio.getId(), usuario == null ? null : usuario.getId(), dia.getValue(),
				LocalTime.parse(desde), LocalTime.parse(hasta)));
	}

	/** Un día sin atender; {@code usuario} nulo = cierra el local. */
	public void ausencia(Negocio negocio, Usuario usuario, LocalDate dia) {
		ausencias.save(new Ausencia(negocio.getId(), usuario == null ? null : usuario.getId(), dia, dia, null));
	}

	/** Un turno reservado, guardado directo (sin mirar disponibilidad). */
	public Turno turno(Usuario barbero, Servicio servicio, Instant inicio, String cliente) {
		return turnos.save(new Turno(servicio.getNegocioId(), barbero, servicio, inicio,
				Duration.ofMinutes(servicio.getDuracionMinutos()), cliente, null, null, null));
	}

	public Turno turno(Usuario barbero, Servicio servicio, LocalDate dia, String hora, String cliente) {
		return turno(barbero, servicio, instante(dia, hora), cliente);
	}

	/** Ese día a esa hora ("10:00") en la zona de las barberías de prueba. */
	public static Instant instante(LocalDate dia, String hora) {
		return dia.atTime(LocalTime.parse(hora)).atZone(ZONA).toInstant();
	}

	/** Un día de la semana que viene o la otra: siempre a más de 7 días y dentro de lo que se puede reservar. */
	public static LocalDate dentroDeUnasSemanas(DayOfWeek dia) {
		return LocalDate.now(ZONA).with(TemporalAdjusters.next(dia)).plusWeeks(1);
	}

	/** Lo que usa {@code with(user(...))} para simular una sesión iniciada. */
	public UsuarioAutenticado sesion(Usuario usuario) {
		return UsuarioAutenticado.de(usuario);
	}

	private Usuario miembro(Negocio negocio, String nombre, Rol rol) {
		String email = Slugs.desde(nombre) + "-" + SECUENCIA.incrementAndGet() + "@prueba.test";
		return usuarios.save(Usuario.miembro(negocio, nombre, email, contrasenaCodificada(), rol));
	}

	// bcrypt es lento a propósito: se codifica una sola vez
	private String contrasenaCodificada() {
		if (contrasenaCodificada == null) {
			contrasenaCodificada = passwordEncoder.encode(CONTRASENA);
		}
		return contrasenaCodificada;
	}

}
