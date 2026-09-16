package dev.deveps.moteros.repositories;

import dev.deveps.moteros.entities.Bloqueo;
import dev.deveps.moteros.entities.Comentario;
import dev.deveps.moteros.entities.Conversacion;
import dev.deveps.moteros.entities.Mensaje;
import dev.deveps.moteros.entities.Publicacion;
import dev.deveps.moteros.entities.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** Consultas que ocultan el contenido entre usuarios con un bloqueo. */
@DataJpaTest
@ActiveProfiles("test")
class BloqueoRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private BloqueoRepository bloqueoRepository;
    @Autowired private PublicacionRepository publicacionRepository;
    @Autowired private ComentarioRepository comentarioRepository;
    @Autowired private ConversacionRepository conversacionRepository;
    @Autowired private MensajeRepository mensajeRepository;

    private Usuario yo;
    private Usuario pesado;
    private Usuario otro;

    @BeforeEach
    void setUp() {
        yo = usuario("yo");
        pesado = usuario("pesado");
        otro = usuario("otro");
        // Lo bloquea el otro lado: los efectos han de ser los mismos.
        em.persistAndFlush(Bloqueo.builder().bloqueador(pesado).bloqueado(yo).build());
    }

    @Test
    void existeEntre_enLosDosSentidos() {
        assertThat(bloqueoRepository.existeEntre(yo.getId(), pesado.getId())).isTrue();
        assertThat(bloqueoRepository.existeEntre(pesado.getId(), yo.getId())).isTrue();
        assertThat(bloqueoRepository.existeEntre(yo.getId(), otro.getId())).isFalse();
        assertThat(bloqueoRepository.existeEntreUuid(yo.getId(), pesado.getUuid())).isTrue();
        assertThat(bloqueoRepository.existeEntreUuid(yo.getId(), otro.getUuid())).isFalse();
    }

    @Test
    void findBloqueados_soloLosDelBloqueador() {
        assertThat(bloqueoRepository.findBloqueados(pesado.getId(), PageRequest.of(0, 10)).getContent())
                .extracting(Usuario::getNombreUsuario).containsExactly("yo");
        assertThat(bloqueoRepository.findBloqueados(yo.getId(), PageRequest.of(0, 10))).isEmpty();
    }

    @Test
    void buscarPublicaciones_ocultaLasDeUsuariosBloqueados() {
        publicacion(pesado, "ruta por el puerto");
        publicacion(otro, "ruta por la costa");

        assertThat(publicacionRepository.buscarPorTexto("ruta", yo.getId(), PageRequest.of(0, 10)).getContent())
                .extracting(p -> p.getUsuario().getNombreUsuario()).containsExactly("otro");
        assertThat(publicacionRepository.buscarPorTexto("ruta", otro.getId(), PageRequest.of(0, 10)))
                .hasSize(2);
    }

    @Test
    void comentarios_ocultaLosDeUsuariosBloqueados() {
        Publicacion p = publicacion(otro, "salida del domingo");
        comentario(p, pesado, "yo voy");
        comentario(p, otro, "genial");

        assertThat(comentarioRepository.findVisibles(p.getUuid(), yo.getId(), PageRequest.of(0, 10)).getContent())
                .extracting(Comentario::getContenido).containsExactly("genial");
        assertThat(comentarioRepository.findUltimosVisibles(p.getId(), yo.getId(), PageRequest.of(0, 3)))
                .extracting(Comentario::getContenido).containsExactly("genial");
        assertThat(comentarioRepository.findVisibles(p.getUuid(), otro.getId(), PageRequest.of(0, 10)))
                .hasSize(2);
    }

    @Test
    void conversacionesYNoLeidos_ocultanLasDeUsuariosBloqueados() {
        Conversacion conPesado = conversacion(yo, pesado);
        Conversacion conOtro = conversacion(yo, otro);
        mensaje(conPesado, pesado);
        mensaje(conOtro, otro);

        assertThat(conversacionRepository.findByParticipante(yo.getId(), PageRequest.of(0, 10)).getContent())
                .extracting(Conversacion::getId).containsExactly(conOtro.getId());
        assertThat(conversacionRepository.findByParticipante(pesado.getId(), PageRequest.of(0, 10))).isEmpty();
        assertThat(mensajeRepository.countNoLeidosTotal(yo.getId())).isEqualTo(1);
    }

    private Usuario usuario(String n) {
        return em.persistAndFlush(Usuario.builder()
                .nombreUsuario(n).nombreCompleto(n).email(n + "@test.com")
                .passwordHash("hash").activo(true).build());
    }

    private Publicacion publicacion(Usuario autor, String contenido) {
        return em.persistAndFlush(Publicacion.builder().usuario(autor).contenido(contenido).build());
    }

    private void comentario(Publicacion p, Usuario autor, String contenido) {
        em.persistAndFlush(Comentario.builder().publicacion(p).usuario(autor).contenido(contenido).build());
    }

    private Conversacion conversacion(Usuario x, Usuario y) {
        boolean xPrimero = x.getId() < y.getId();
        return em.persistAndFlush(Conversacion.builder()
                .usuario1(xPrimero ? x : y).usuario2(xPrimero ? y : x).build());
    }

    private void mensaje(Conversacion c, Usuario remitente) {
        em.persistAndFlush(Mensaje.builder().conversacion(c).remitente(remitente).contenido("hola").leido(false).build());
    }
}
