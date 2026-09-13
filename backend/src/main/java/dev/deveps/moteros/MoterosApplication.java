package dev.deveps.moteros;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class MoterosApplication {

	/**
	 * Las fechas se manejan como LocalDateTime (sin zona) y la app las muestra como hora local.
	 * Se fija la zona de Espana para que no dependa de la del servidor (el VPS esta en UTC) y
	 * coincida con serverTimezone de la conexion JDBC.
	 */
	public static final String ZONA_HORARIA = "Europe/Madrid";

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone(ZONA_HORARIA));
		SpringApplication.run(MoterosApplication.class, args);
	}

}
