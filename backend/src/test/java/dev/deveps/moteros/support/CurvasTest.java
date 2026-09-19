package dev.deveps.moteros.support;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class CurvasTest {

    @Test
    void unaRectaNoTieneCurvas() {
        List<double[]> recta = new ArrayList<>();
        for (int i = 0; i <= 100; i++) {
            recta.add(new double[]{41.0 + i * 0.001, 2.0});
        }
        assertThat(Curvas.gradosPorKm(recta)).isCloseTo(0, within(0.5));
    }

    @Test
    void unZigZagTieneMuchasMasCurvasQueUnaRecta() {
        List<double[]> zigzag = new ArrayList<>();
        for (int i = 0; i <= 100; i++) {
            zigzag.add(new double[]{41.0 + i * 0.001, 2.0 + (i % 2 == 0 ? 0 : 0.001)});
        }
        assertThat(Curvas.gradosPorKm(zigzag)).isGreaterThan(300);
    }

    @Test
    void puntoMasAlejado_caeDondeSeSeparanLasCarreteras() {
        // La referencia va recta hacia el norte; la otra se aparta al este a mitad de camino.
        List<double[]> referencia = new ArrayList<>();
        List<double[]> desvio = new ArrayList<>();
        for (int i = 0; i <= 20; i++) {
            referencia.add(new double[]{41.0 + i * 0.01, 2.0});
            double este = i <= 10 ? i * 0.01 : (20 - i) * 0.01;
            desvio.add(new double[]{41.0 + i * 0.01, 2.0 + este});
        }

        double[] punto = Curvas.puntoMasAlejado(desvio, referencia);

        assertThat(punto[0]).isCloseTo(41.10, within(1e-6));
        assertThat(punto[1]).isCloseTo(2.10, within(1e-6));
    }

    @Test
    void separacion_ceroEntreRecorridosIgualesYMayorSiSeApartan() {
        List<double[]> a = List.of(new double[]{41.0, 2.0}, new double[]{41.1, 2.0});
        List<double[]> b = List.of(new double[]{41.0, 2.0}, new double[]{41.05, 2.1}, new double[]{41.1, 2.0});

        assertThat(Curvas.separacionKm(a, a)).isZero();
        assertThat(Curvas.separacionKm(b, a)).isGreaterThan(5);
    }

    @Test
    void candidatosDePaso_empiezanPorElPuntoMasAlejado() {
        List<double[]> referencia = new ArrayList<>();
        List<double[]> desvio = new ArrayList<>();
        for (int i = 0; i <= 20; i++) {
            referencia.add(new double[]{41.0 + i * 0.01, 2.0});
            double este = i <= 10 ? i * 0.01 : (20 - i) * 0.01;
            desvio.add(new double[]{41.0 + i * 0.01, 2.0 + este});
        }

        List<List<double[]>> candidatos = Curvas.candidatosDePaso(desvio, referencia);

        assertThat(candidatos).hasSize(4);
        assertThat(candidatos.getFirst().getFirst()[1]).isCloseTo(2.10, within(1e-6));
        assertThat(candidatos.getLast()).hasSize(2);
    }
}
