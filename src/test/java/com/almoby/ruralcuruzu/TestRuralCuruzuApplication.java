package com.almoby.ruralcuruzu;

import org.springframework.boot.SpringApplication;

public class TestRuralCuruzuApplication {

	public static void main(String[] args) {
		SpringApplication.from(RuralCuruzuApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
