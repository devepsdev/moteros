package dev.deveps.moteros;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MoterosApplication {

	public static void main(String[] args) {
		SpringApplication.run(MoterosApplication.class, args);
	}

}
