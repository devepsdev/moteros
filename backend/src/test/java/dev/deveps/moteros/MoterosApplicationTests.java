package dev.deveps.moteros;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Verifica que el contexto completo de Spring (JPA, security, mapper, servicios, controladores) carga sin errores. */
@SpringBootTest
@ActiveProfiles("test")
class MoterosApplicationTests {

    @Test
    void contextLoads() {
    }
}
