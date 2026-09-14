package dev.deveps.moteros.repositories;

import dev.deveps.moteros.entities.Ruta;
import dev.deveps.moteros.entities.SugerenciaRuta;
import dev.deveps.moteros.entities.Usuario;
import dev.deveps.moteros.entities.enums.EstadoSugerencia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class SugerenciaRutaRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private SugerenciaRutaRepository sugerenciaRepository;
    @Autowired private RutaRepository rutaRepository;

    private Usuario bot;

    @BeforeEach
    void setUp() {
        bot = em.persistAndFlush(Usuario.builder().nombreUsuario("bot").nombreCompleto("Bot")
                .email("bot@test.com").passwordHash("h").activo(true).build());
        em.persistAndFlush(Ruta.builder().creador(bot).nombre("Curvas del Montseny")
                .puntoInicio("Sant Celoni").puntoFin("Viladrau").build());
        em.persistAndFlush(SugerenciaRuta.builder().usuario(bot).urlFuente("https://x")
                .nombre("Costa Brava").puntoInicio("Blanes").puntoFin("Roses")
                .estado(EstadoSugerencia.rechazada).build());
        em.persistAndFlush(SugerenciaRuta.builder().usuario(bot).urlFuente("https://y")
                .nombre("Pirineo").puntoInicio("Berga").puntoFin("Andorra")
                .estado(EstadoSugerencia.pendiente).build());
    }

    @Test
    void existeEnCatalogo_ignoraMayusculas() {
        assertThat(rutaRepository.existeEnCatalogo("curvas del montseny", "SANT CELONI")).isTrue();
        assertThat(rutaRepository.existeEnCatalogo("Curvas del Montseny", "Viladrau")).isFalse();
    }

    @Test
    void existeSugerida_cuentaTambienLasRechazadas() {
        assertThat(sugerenciaRepository.existeSugerida("costa brava", "blanes")).isTrue();
        assertThat(sugerenciaRepository.existeSugerida("Costa Brava", "Girona")).isFalse();
    }

    @Test
    void filtroPorEstadoYContador() {
        assertThat(sugerenciaRepository.countByEstado(EstadoSugerencia.pendiente)).isEqualTo(1);
        assertThat(sugerenciaRepository.findByEstadoOrderByFechaCreacionDesc(EstadoSugerencia.pendiente, PageRequest.of(0, 10))
                .getContent()).extracting(SugerenciaRuta::getNombre).containsExactly("Pirineo");
    }
}
