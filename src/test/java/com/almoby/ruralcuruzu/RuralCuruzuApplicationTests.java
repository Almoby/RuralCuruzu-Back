package com.almoby.ruralcuruzu;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * jwt.secret y app.mongodb.uri no tienen valor por defecto en application.properties
 * (a propósito: en producción tienen que fallar rápido si falta la variable de entorno
 * real). Para los tests les damos overrides puntuales acá con @TestPropertySource, que
 * se suman por encima de application.properties sin reemplazarlo, así el resto de la
 * configuración (jwt.expiration-minutes, CORS, mail, etc.) se sigue cargando normal.
 *
 * app.mongodb.uri de test es un valor "de relleno": nunca se llega a usar en la
 * práctica porque MongoConfig cede el paso (@ConditionalOnMissingBean) al
 * MongoConnectionDetails que registra TestcontainersConfiguration.
 */
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
		"jwt.secret=test-only-secret-do-not-use-in-production-1234567890",
		"app.mongodb.uri=mongodb://localhost:27017/test"
})
@SpringBootTest
class RuralCuruzuApplicationTests {

	@Test
	void contextLoads() {
	}

}
