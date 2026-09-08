package dev.deveps.moteros.repositories;

import dev.deveps.moteros.entities.Amistad;
import dev.deveps.moteros.entities.Publicacion;
import dev.deveps.moteros.entities.Usuario;
import dev.deveps.moteros.entities.enums.EstadoAmistad;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class PublicacionRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private PublicacionRepository publicacionRepository;

    private Usuario yo;
    private Usuario amigo;
    private Usuario extrano;

    @BeforeEach
    void setUp() {
        yo = usuario("yo");
        amigo = usuario("amigo");
        extrano = usuario("extrano");
        em.persistAndFlush(Amistad.builder().usuario(yo).amigo(amigo).estado(EstadoAmistad.aceptada).build());

        publicacion(yo, "mi publicacion sobre curvas");
        publicacion(amigo, "publicacion de un amigo");
        publicacion(extrano, "publicacion de un desconocido");
    }

    @Test
    void feed_incluyePropiasYDeAmigosAceptados_excluyeExtranos() {
        Page<Publicacion> feed = publicacionRepository.feed(yo.getId(), PageRequest.of(0, 10));
        assertThat(feed.getContent()).extracting(p -> p.getUsuario().getNombreUsuario())
                .containsExactlyInAnyOrder("yo", "amigo");
    }

    @Test
    void buscarPorTexto_coincidenciaEnContenido() {
        Page<Publicacion> res = publicacionRepository.buscarPorTexto("curvas", PageRequest.of(0, 10));
        assertThat(res.getContent()).hasSize(1);
        assertThat(res.getContent().getFirst().getUsuario().getNombreUsuario()).isEqualTo("yo");
    }

    @Test
    void countByUsuario() {
        assertThat(publicacionRepository.countByUsuarioId(yo.getId())).isEqualTo(1);
    }

    private Usuario usuario(String n) {
        return em.persistAndFlush(Usuario.builder()
                .nombreUsuario(n).nombreCompleto(n).email(n + "@test.com")
                .passwordHash("hash").activo(true).build());
    }

    private void publicacion(Usuario autor, String contenido) {
        em.persistAndFlush(Publicacion.builder().usuario(autor).contenido(contenido).build());
    }
}
