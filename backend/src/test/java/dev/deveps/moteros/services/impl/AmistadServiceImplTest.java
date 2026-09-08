package dev.deveps.moteros.services.impl;

import dev.deveps.moteros.dto.AmistadResponseDTO;
import dev.deveps.moteros.entities.Amistad;
import dev.deveps.moteros.entities.Usuario;
import dev.deveps.moteros.entities.enums.EstadoAmistad;
import dev.deveps.moteros.entities.enums.TipoNotificacion;
import dev.deveps.moteros.exceptions.BadRequestException;
import dev.deveps.moteros.mapper.EntityDtoMapper;
import dev.deveps.moteros.repositories.AmistadRepository;
import dev.deveps.moteros.repositories.UsuarioRepository;
import dev.deveps.moteros.security.UsuarioAutenticadoProvider;
import dev.deveps.moteros.services.NotificacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AmistadServiceImplTest {

    @Mock private AmistadRepository amistadRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UsuarioAutenticadoProvider usuarioAutenticado;
    @Mock private NotificacionService notificacionService;

    private AmistadServiceImpl service;

    private Usuario yo;
    private Usuario otro;

    @BeforeEach
    void setUp() {
        service = new AmistadServiceImpl(amistadRepository, usuarioRepository, usuarioAutenticado,
                notificacionService, new EntityDtoMapper());
        yo = Usuario.builder().id(1).uuid("uuid-yo").nombreUsuario("yo").nombreCompleto("Yo Motero")
                .email("yo@test.com").passwordHash("h").activo(true).build();
        otro = Usuario.builder().id(2).uuid("uuid-otro").nombreUsuario("otro").nombreCompleto("Otro Motero")
                .email("otro@test.com").passwordHash("h").activo(true).build();
    }

    @Test
    void enviarSolicitud_aSiMismo_lanzaBadRequest() {
        when(usuarioAutenticado.obtenerUsuarioActual()).thenReturn(yo);
        when(usuarioRepository.findByUuid("uuid-yo")).thenReturn(Optional.of(yo));

        assertThatThrownBy(() -> service.enviarSolicitud("uuid-yo"))
                .isInstanceOf(BadRequestException.class);
        verify(notificacionService, never()).notificar(any(), any(), any(), any(), any());
    }

    @Test
    void enviarSolicitud_yaSonAmigos_lanzaBadRequest() {
        when(usuarioAutenticado.obtenerUsuarioActual()).thenReturn(yo);
        when(usuarioRepository.findByUuid("uuid-otro")).thenReturn(Optional.of(otro));
        when(amistadRepository.findRelacion(1, 2)).thenReturn(Optional.of(
                Amistad.builder().usuario(yo).amigo(otro).estado(EstadoAmistad.aceptada).build()));

        assertThatThrownBy(() -> service.enviarSolicitud("uuid-otro"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void enviarSolicitud_pendienteExistente_lanzaBadRequest() {
        when(usuarioAutenticado.obtenerUsuarioActual()).thenReturn(yo);
        when(usuarioRepository.findByUuid("uuid-otro")).thenReturn(Optional.of(otro));
        when(amistadRepository.findRelacion(1, 2)).thenReturn(Optional.of(
                Amistad.builder().usuario(otro).amigo(yo).estado(EstadoAmistad.pendiente).build()));

        assertThatThrownBy(() -> service.enviarSolicitud("uuid-otro"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void enviarSolicitud_ok_guardaYNotifica() {
        when(usuarioAutenticado.obtenerUsuarioActual()).thenReturn(yo);
        when(usuarioRepository.findByUuid("uuid-otro")).thenReturn(Optional.of(otro));
        when(amistadRepository.findRelacion(1, 2)).thenReturn(Optional.empty());
        when(amistadRepository.save(any(Amistad.class))).thenAnswer(inv -> inv.getArgument(0));

        AmistadResponseDTO res = service.enviarSolicitud("uuid-otro");

        assertThat(res.getEstado()).isEqualTo(EstadoAmistad.pendiente);
        assertThat(res.getEnviadaPorMi()).isTrue();
        verify(notificacionService).notificar(eq(otro), eq(TipoNotificacion.solicitud_amistad),
                isNull(), eq(yo), any());
    }

    @Test
    void responder_noEsDestinatario_lanzaBadRequest() {
        when(usuarioAutenticado.obtenerUsuarioActual()).thenReturn(yo);
        // solicitud dirigida a 'otro', no a 'yo'
        Amistad amistad = Amistad.builder().uuid("a-1").usuario(otro).amigo(
                        Usuario.builder().id(3).nombreUsuario("tercero").build())
                .estado(EstadoAmistad.pendiente).build();
        when(amistadRepository.findByUuid("a-1")).thenReturn(Optional.of(amistad));

        assertThatThrownBy(() -> service.responder("a-1", true))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void responder_aceptar_marcaAceptadaYNotificaAlSolicitante() {
        when(usuarioAutenticado.obtenerUsuarioActual()).thenReturn(yo);
        Amistad amistad = Amistad.builder().uuid("a-1").usuario(otro).amigo(yo)
                .estado(EstadoAmistad.pendiente).build();
        when(amistadRepository.findByUuid("a-1")).thenReturn(Optional.of(amistad));
        when(amistadRepository.save(any(Amistad.class))).thenAnswer(inv -> inv.getArgument(0));

        AmistadResponseDTO res = service.responder("a-1", true);

        assertThat(res.getEstado()).isEqualTo(EstadoAmistad.aceptada);
        verify(notificacionService).notificar(eq(otro), eq(TipoNotificacion.amistad_aceptada),
                isNull(), eq(yo), any());
    }
}
