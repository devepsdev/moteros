package dev.deveps.moteros.dto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La app manda los puntos de la ruta tal como se los devuelve el trazado, con {@code via} a null
 * en los que marca el usuario. Sin la configuracion de Jackson, ese null sobre un campo primitivo
 * tumbaba el guardado de la ruta con un 500.
 */
@JsonTest
@ActiveProfiles("test")
class PuntoRutaRequestDTOJsonTest {

    @Autowired
    private ObjectMapper mapper;

    @Test
    void punto_conViaNula_seLeeComoFalse() {
        String json = """
                {"orden":0,"latitud":41.5407,"longitud":2.2130,"altitudM":null,"nombrePunto":null,"via":null}
                """;

        PuntoRutaRequestDTO punto = mapper.readValue(json, PuntoRutaRequestDTO.class);

        assertThat(punto.isVia()).isFalse();
        assertThat(punto.getOrden()).isZero();
    }

    @Test
    void punto_conViaVerdadera_seRespeta() {
        String json = """
                {"orden":1,"latitud":42.4132,"longitud":1.1287,"via":true}
                """;

        assertThat(mapper.readValue(json, PuntoRutaRequestDTO.class).isVia()).isTrue();
    }
}
