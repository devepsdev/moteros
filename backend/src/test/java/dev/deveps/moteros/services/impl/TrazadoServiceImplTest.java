package dev.deveps.moteros.services.impl;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TrazadoServiceImplTest {

    @Test
    void sinClave_noEstaDisponibleNiCalcula() {
        TrazadoServiceImpl service = new TrazadoServiceImpl("", "http://localhost:1");

        assertThat(service.disponible()).isFalse();
        assertThat(service.calcular(List.of(new double[]{41.0, 2.0}, new double[]{41.1, 2.1}))).isEmpty();
    }

    @Test
    void leerTramo_pasaDeLongitudLatitudALatitudLongitud() {
        String geojson = """
                {"type":"FeatureCollection","features":[{"type":"Feature",
                  "properties":{"summary":{"distance":12345.6,"duration":900.0}},
                  "geometry":{"type":"LineString","coordinates":[[2.1,41.3],[2.2,41.35],[2.3,41.4]]}}]}
                """;

        TrazadoServiceImpl.Tramo tramo = TrazadoServiceImpl.leerTramo(geojson);

        assertThat(tramo.coordenadas()).hasSize(3);
        assertThat(tramo.coordenadas().getFirst()).containsExactly(41.3, 2.1);
        assertThat(tramo.metros()).isEqualTo(12345.6);
        assertThat(tramo.segundos()).isEqualTo(900.0);
    }

    @Test
    void leerTramos_leeTodasLasAlternativas() {
        String geojson = """
                {"features":[
                  {"properties":{"summary":{"distance":1000,"duration":60}},
                   "geometry":{"coordinates":[[2.1,41.3],[2.2,41.4]]}},
                  {"properties":{"summary":{"distance":1500,"duration":120}},
                   "geometry":{"coordinates":[[2.1,41.3],[2.3,41.35],[2.2,41.4]]}}]}
                """;

        assertThat(TrazadoServiceImpl.leerTramos(geojson)).extracting(TrazadoServiceImpl.Tramo::metros)
                .containsExactly(1000.0, 1500.0);
    }

    @Test
    void leerTramo_sinGeometria_devuelveNull() {
        assertThat(TrazadoServiceImpl.leerTramo("{\"features\":[]}")).isNull();
    }
}
