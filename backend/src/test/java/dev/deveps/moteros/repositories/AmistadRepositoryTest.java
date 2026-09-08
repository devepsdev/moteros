package dev.deveps.moteros.repositories;

import dev.deveps.moteros.entities.Amistad;
import dev.deveps.moteros.entities.Usuario;
import dev.deveps.moteros.entities.enums.EstadoAmistad;
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
class AmistadRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private AmistadRepository amistadRepository;

    private Usuario a;
    private Usuario b;
    private Usuario c;

    @BeforeEach
    void setUp() {
        a = usuario("a");
        b = usuario("b");
        c = usuario("c");
        // a <-> b aceptada (a solicita)
        em.persistAndFlush(Amistad.builder().usuario(a).amigo(b).estado(EstadoAmistad.aceptada).build());
        // c -> a aceptada (c solicita)  => a tambien es amigo por el otro lado
        em.persistAndFlush(Amistad.builder().usuario(c).amigo(a).estado(EstadoAmistad.aceptada).build());
        // b -> c pendiente
        em.persistAndFlush(Amistad.builder().usuario(b).amigo(c).estado(EstadoAmistad.pendiente).build());
    }

    @Test
    void findRelacion_encuentraEnCualquierDireccion() {
        assertThat(amistadRepository.findRelacion(a.getId(), b.getId())).isPresent();
        assertThat(amistadRepository.findRelacion(b.getId(), a.getId())).isPresent();
        assertThat(amistadRepository.findRelacion(a.getId(), c.getId())).isPresent();
    }

    @Test
    void findAmigosAceptados_cuentaLosDosLados() {
        var amigosDeA = amistadRepository.findAmigosAceptados(a.getId(), PageRequest.of(0, 10));
        assertThat(amigosDeA.getContent()).extracting(Usuario::getNombreUsuario)
                .containsExactlyInAnyOrder("b", "c");
    }

    @Test
    void countAmigosAceptados_ignoraPendientes() {
        assertThat(amistadRepository.countAmigosAceptados(a.getId())).isEqualTo(2);
        assertThat(amistadRepository.countAmigosAceptados(b.getId())).isEqualTo(1); // solo a (b-c es pendiente)
        assertThat(amistadRepository.countAmigosAceptados(c.getId())).isEqualTo(1); // solo a
    }

    private Usuario usuario(String n) {
        return em.persistAndFlush(Usuario.builder()
                .nombreUsuario(n).nombreCompleto(n).email(n + "@test.com")
                .passwordHash("hash").activo(true).build());
    }
}
