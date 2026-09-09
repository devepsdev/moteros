package dev.deveps.moteros.services.impl;

import dev.deveps.moteros.dto.QuedadaRequestDTO;
import dev.deveps.moteros.entities.InscripcionQuedada;
import dev.deveps.moteros.entities.Quedada;
import dev.deveps.moteros.entities.Usuario;
import dev.deveps.moteros.entities.enums.EstadoInscripcion;
import dev.deveps.moteros.entities.enums.EstadoQuedada;
import dev.deveps.moteros.entities.enums.TipoNotificacion;
import dev.deveps.moteros.mapper.EntityDtoMapper;
import dev.deveps.moteros.repositories.AmistadRepository;
import dev.deveps.moteros.repositories.InscripcionQuedadaRepository;
import dev.deveps.moteros.repositories.QuedadaRepository;
import dev.deveps.moteros.repositories.RutaRepository;
import dev.deveps.moteros.repositories.ValoracionRutaRepository;
import dev.deveps.moteros.security.UsuarioAutenticadoProvider;
import dev.deveps.moteros.services.NotificacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuedadaServiceImplTest {

    @Mock private QuedadaRepository quedadaRepository;
    @Mock private InscripcionQuedadaRepository inscripcionRepository;
    @Mock private RutaRepository rutaRepository;
    @Mock private ValoracionRutaRepository valoracionRutaRepository;
    @Mock private AmistadRepository amistadRepository;
    @Mock private UsuarioAutenticadoProvider usuarioAutenticado;
    @Mock private NotificacionService notificacionService;

    private QuedadaServiceImpl service;

    private Usuario organizador;
    private Usuario otro;

    @BeforeEach
    void setUp() {
        service = new QuedadaServiceImpl(quedadaRepository, inscripcionRepository, rutaRepository,
                valoracionRutaRepository, amistadRepository, usuarioAutenticado, notificacionService,
                new EntityDtoMapper());
        organizador = usuario(1, "org");
        otro = usuario(2, "otro");

        // Stubs neutros para el metodo detalle() que se invoca al final de casi todo
        lenient().when(inscripcionRepository.countByQuedadaId(anyInt())).thenReturn(0L);
        lenient().when(inscripcionRepository.countByQuedadaIdAndEstado(anyInt(), any())).thenReturn(0L);
        lenient().when(inscripcionRepository.findByQuedadaId(anyInt())).thenReturn(List.of());
        lenient().when(inscripcionRepository.findByQuedadaIdAndUsuarioId(anyInt(), anyInt()))
                .thenReturn(Optional.empty());
        lenient().when(usuarioAutenticado.obtenerIdUsuarioActual()).thenReturn(1);
    }

    @Test
    void inscribirse_notificaAlOrganizador() {
        Quedada quedada = quedada(10, EstadoQuedada.programada);
        when(quedadaRepository.findByUuid("q-10")).thenReturn(Optional.of(quedada));
        when(usuarioAutenticado.obtenerUsuarioActual()).thenReturn(otro);
        when(inscripcionRepository.findByQuedadaIdAndUsuarioId(10, 2)).thenReturn(Optional.empty());
        when(inscripcionRepository.save(any(InscripcionQuedada.class))).thenAnswer(inv -> inv.getArgument(0));

        service.inscribirse("q-10");

        verify(notificacionService).notificar(eq(organizador), eq(TipoNotificacion.inscripcion_quedada),
                eq(10), eq(otro), any());
    }

    @Test
    void cambiarEstado_cancelada_notificaACadaInscrito() {
        Quedada quedada = quedada(10, EstadoQuedada.programada);
        when(quedadaRepository.findByUuid("q-10")).thenReturn(Optional.of(quedada));
        when(quedadaRepository.save(any(Quedada.class))).thenAnswer(inv -> inv.getArgument(0));
        when(inscripcionRepository.findByQuedadaId(10)).thenReturn(List.of(
                inscripcion(quedada, usuario(2, "a")),
                inscripcion(quedada, usuario(3, "b"))));

        service.cambiarEstado("q-10", EstadoQuedada.cancelada);

        verify(notificacionService, times(2)).notificar(any(), eq(TipoNotificacion.quedada_cancelada),
                eq(10), eq(organizador), any());
    }

    @Test
    void crear_notificaALosAmigosDelOrganizador() {
        when(usuarioAutenticado.obtenerUsuarioActual()).thenReturn(organizador);
        when(quedadaRepository.save(any(Quedada.class))).thenAnswer(inv -> {
            Quedada q = inv.getArgument(0);
            q.setId(99);
            return q;
        });
        when(amistadRepository.findAmigosAceptadosLista(1))
                .thenReturn(List.of(usuario(2, "amigo1"), usuario(3, "amigo2")));

        QuedadaRequestDTO dto = QuedadaRequestDTO.builder()
                .titulo("Ruta del domingo")
                .puntoEncuentro("Gasolinera")
                .fechaHora(LocalDateTime.now().plusDays(3))
                .build();

        service.crear(dto);

        verify(notificacionService, times(2)).notificar(any(), eq(TipoNotificacion.nueva_quedada),
                eq(99), eq(organizador), any());
    }

    // ===== helpers =====

    private Usuario usuario(int id, String n) {
        return Usuario.builder().id(id).uuid("u-" + id).nombreUsuario(n).nombreCompleto(n)
                .email(n + "@test.com").passwordHash("h").activo(true).build();
    }

    private Quedada quedada(int id, EstadoQuedada estado) {
        return Quedada.builder().id(id).uuid("q-" + id).titulo("Quedada " + id)
                .organizador(organizador).estado(estado).maxParticipantes(20)
                .fechaHora(LocalDateTime.now().plusDays(2)).build();
    }

    private InscripcionQuedada inscripcion(Quedada q, Usuario u) {
        return InscripcionQuedada.builder().quedada(q).usuario(u).estado(EstadoInscripcion.confirmado).build();
    }
}
