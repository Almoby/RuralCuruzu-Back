package com.ruralcuruzu.rural_curuzu;

import org.springframework.boot.SpringApplication;

public class TestRuralCuruzuApplication {

	public static void main(String[] args) {
		SpringApplication.from(RuralCuruzuApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
