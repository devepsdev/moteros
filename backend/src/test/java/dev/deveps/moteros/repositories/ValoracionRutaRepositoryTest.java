package dev.deveps.moteros.repositories;

import dev.deveps.moteros.entities.Ruta;
import dev.deveps.moteros.entities.Usuario;
import dev.deveps.moteros.entities.ValoracionRuta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ValoracionRutaRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ValoracionRutaRepository valoracionRepository;

    private Ruta ruta;
    private Usuario u1;
    private Usuario u2;

    @BeforeEach
    void setUp() {
        Usuario creador = persistirUsuario("creador");
        u1 = persistirUsuario("u1");
        u2 = persistirUsuario("u2");
        ruta = em.persistAndFlush(Ruta.builder()
                .creador(creador).nombre("R").puntoInicio("A").puntoFin("B").build());

        em.persistAndFlush(ValoracionRuta.builder().ruta(ruta).usuario(u1)
                .puntuacion((byte) 4).comentario("bien").build());
        em.persistAndFlush(ValoracionRuta.builder().ruta(ruta).usuario(u2)
                .puntuacion((byte) 2).build());
    }

    @Test
    void mediaPuntuacion_yConteo() {
        assertThat(valoracionRepository.mediaPuntuacion(ruta.getId())).isEqualTo(3.0);
        assertThat(valoracionRepository.countByRutaId(ruta.getId())).isEqualTo(2);
    }

    @Test
    void findByRutaIdAndUsuarioId_yExists() {
        assertThat(valoracionRepository.findByRutaIdAndUsuarioId(ruta.getId(), u1.getId())).isPresent();
        assertThat(valoracionRepository.existsByRutaIdAndUsuarioId(ruta.getId(), u2.getId())).isTrue();
    }

    @Test
    void mediaPuntuacion_esNullSinValoraciones() {
        Ruta otra = em.persistAndFlush(Ruta.builder()
                .creador(u1).nombre("Otra").puntoInicio("A").puntoFin("B").build());
        assertThat(valoracionRepository.mediaPuntuacion(otra.getId())).isNull();
    }

    private Usuario persistirUsuario(String n) {
        return em.persistAndFlush(Usuario.builder()
                .nombreUsuario(n).nombreCompleto(n).email(n + "@test.com")
                .passwordHash("hash").activo(true).build());
    }
}
