package dev.deveps.moteros.support;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class PolilineaTest {

    /** Ejemplo de la documentacion del algoritmo de Google. */
    private static final String EJEMPLO = "_p~iF~ps|U_ulLnnqC_mqNvxq`@";

    @Test
    void codifica_elEjemploDeReferencia() {
        List<double[]> puntos = List.of(
                new double[]{38.5, -120.2},
                new double[]{40.7, -120.95},
                new double[]{43.252, -126.453});
        assertThat(Polilinea.codificar(puntos)).isEqualTo(EJEMPLO);
    }

    @Test
    void decodifica_loQueCodifica() {
        List<double[]> puntos = List.of(
                new double[]{41.38791, 2.16992},
                new double[]{42.26712, 2.96103},
                new double[]{42.48612, 2.74891});
        List<double[]> vuelta = Polilinea.decodificar(Polilinea.codificar(puntos));

        assertThat(vuelta).hasSize(3);
        for (int i = 0; i < puntos.size(); i++) {
            assertThat(vuelta.get(i)[0]).isCloseTo(puntos.get(i)[0], within(1e-5));
            assertThat(vuelta.get(i)[1]).isCloseTo(puntos.get(i)[1], within(1e-5));
        }
    }
}
