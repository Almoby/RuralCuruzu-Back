package com.almoby.ruralcuruzu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableScheduling habilita la limpieza periódica de RateLimiterService.
@SpringBootApplication
@EnableScheduling
public class RuralCuruzuApplication {

	public static void main(String[] args) {
		SpringApplication.run(RuralCuruzuApplication.class, args);
	}

}
