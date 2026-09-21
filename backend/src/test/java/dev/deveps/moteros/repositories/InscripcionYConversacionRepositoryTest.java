package dev.deveps.moteros.repositories;

import dev.deveps.moteros.entities.Conversacion;
import dev.deveps.moteros.entities.InscripcionQuedada;
import dev.deveps.moteros.entities.Mensaje;
import dev.deveps.moteros.entities.Quedada;
import dev.deveps.moteros.entities.Usuario;
import dev.deveps.moteros.entities.enums.EstadoInscripcion;
import dev.deveps.moteros.entities.enums.EstadoQuedada;
import dev.deveps.moteros.entities.enums.NivelRecomendado;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class InscripcionYConversacionRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private InscripcionQuedadaRepository inscripcionRepository;

    @Autowired
    private ConversacionRepository conversacionRepository;

    @Autowired
    private QuedadaRepository quedadaRepository;

    @Test
    void filtrar_incluyeLasQuedadasSinRutaAsociada() {
        // Una quedada no tiene por que ir ligada a una ruta del catalogo; antes el filtro las
        // perdia todas y la pestana de quedadas salia vacia.
        Usuario org = usuario("org");
        quedada(org, "sin ruta", LocalDateTime.now().plusDays(3));

        var todas = quedadaRepository.filtrar(null, null, null, null, null, null, null, null, PageRequest.of(0, 10));
        var proximas = quedadaRepository.filtrar(null, null, EstadoQuedada.programada, null, null, null, null,
                LocalDateTime.now(), PageRequest.of(0, 10));

        assertThat(todas.getContent()).extracting(Quedada::getTitulo).containsExactly("sin ruta");
        assertThat(proximas.getTotalElements()).isEqualTo(1);
    }

    @Test
    void findActivasByUsuarioUuid_excluyeCanceladasYOrdenaPorFechaDeQuedadaDesc() {
        Usuario org = usuario("org");
        Usuario yo = usuario("yo");
        Quedada antigua = quedada(org, "antigua", LocalDateTime.now().plusDays(1));
        Quedada reciente = quedada(org, "reciente", LocalDateTime.now().plusDays(10));
        Quedada cancelada = quedada(org, "cancelada", LocalDateTime.now().plusDays(5));
        inscripcion(antigua, yo, EstadoInscripcion.confirmado);
        inscripcion(reciente, yo, EstadoInscripcion.confirmado);
        inscripcion(cancelada, yo, EstadoInscripcion.cancelado);

        var pagina = inscripcionRepository.findActivasByUsuarioUuid(yo.getUuid(), PageRequest.of(0, 10));

        assertThat(pagina.getTotalElements()).isEqualTo(2);
        assertThat(pagina.getContent()).extracting(i -> i.getQuedada().getTitulo())
                .containsExactly("reciente", "antigua");
    }

    @Test
    void findByParticipante_ordenaPorUltimoMensajeDesc() {
        Usuario yo = usuario("yo");
        Usuario a = usuario("a");
        Usuario b = usuario("b");
        Usuario c = usuario("c");
        Conversacion conA = conversacion(yo, a);
        Conversacion conB = conversacion(yo, b);
        Conversacion sinMensajes = conversacion(yo, c);
        mensaje(conA, a, LocalDateTime.now().minusHours(3));
        mensaje(conB, b, LocalDateTime.now().minusHours(5));
        mensaje(conB, yo, LocalDateTime.now().minusMinutes(1));

        var pagina = conversacionRepository.findByParticipante(yo.getId(), PageRequest.of(0, 10));

        assertThat(pagina.getTotalElements()).isEqualTo(3);
        assertThat(pagina.getContent()).extracting(Conversacion::getId)
                .containsExactly(conB.getId(), conA.getId(), sinMensajes.getId());
    }

    private Usuario usuario(String n) {
        return em.persistAndFlush(Usuario.builder()
                .nombreUsuario(n).nombreCompleto(n).email(n + "@test.com")
                .passwordHash("hash").activo(true).build());
    }

    private Quedada quedada(Usuario organizador, String titulo, LocalDateTime fecha) {
        return em.persistAndFlush(Quedada.builder()
                .organizador(organizador).titulo(titulo).puntoEncuentro("Plaza")
                .fechaHora(fecha).nivelRecomendado(NivelRecomendado.cualquiera)
                .estado(EstadoQuedada.programada).build());
    }

    private void inscripcion(Quedada q, Usuario u, EstadoInscripcion estado) {
        em.persistAndFlush(InscripcionQuedada.builder().quedada(q).usuario(u).estado(estado).build());
    }

    private Conversacion conversacion(Usuario x, Usuario y) {
        // La BBDD exige usuario1Id < usuario2Id.
        boolean xPrimero = x.getId() < y.getId();
        Conversacion c = em.persistAndFlush(Conversacion.builder()
                .usuario1(xPrimero ? x : y).usuario2(xPrimero ? y : x).build());
        // Creada hace un dia: sin mensajes ha de quedar la ultima.
        fijarFecha("conversaciones", "fecha_creacion", c.getId(), LocalDateTime.now().minusDays(1));
        return c;
    }

    private void mensaje(Conversacion c, Usuario remitente, LocalDateTime fecha) {
        Mensaje m = em.persistAndFlush(Mensaje.builder()
                .conversacion(c).remitente(remitente).contenido("hola").leido(false).build());
        // @CreationTimestamp pisa la fecha al insertar y la columna no es updatable: SQL directo.
        fijarFecha("mensajes", "fecha_envio", m.getId(), fecha);
    }

    private void fijarFecha(String tabla, String columna, Integer id, LocalDateTime fecha) {
        em.getEntityManager()
                .createNativeQuery("UPDATE " + tabla + " SET " + columna + " = ?1 WHERE id = ?2")
                .setParameter(1, fecha).setParameter(2, id)
                .executeUpdate();
        em.clear();
    }
}
