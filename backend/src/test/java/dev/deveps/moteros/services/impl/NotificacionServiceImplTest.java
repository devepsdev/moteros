package dev.deveps.moteros.services.impl;

import dev.deveps.moteros.dto.NotificacionResponseDTO;
import dev.deveps.moteros.entities.Conversacion;
import dev.deveps.moteros.entities.Notificacion;
import dev.deveps.moteros.entities.Publicacion;
import dev.deveps.moteros.entities.Usuario;
import dev.deveps.moteros.entities.enums.TipoNotificacion;
import dev.deveps.moteros.mapper.EntityDtoMapper;
import dev.deveps.moteros.repositories.ConversacionRepository;
import dev.deveps.moteros.repositories.NotificacionRepository;
import dev.deveps.moteros.repositories.PublicacionRepository;
import dev.deveps.moteros.repositories.QuedadaRepository;
import dev.deveps.moteros.repositories.RutaRepository;
import dev.deveps.moteros.security.UsuarioAutenticadoProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceImplTest {

    @Mock private NotificacionRepository notificacionRepository;
    @Mock private PublicacionRepository publicacionRepository;
    @Mock private QuedadaRepository quedadaRepository;
    @Mock private ConversacionRepository conversacionRepository;
    @Mock private RutaRepository rutaRepository;
    @Mock private UsuarioAutenticadoProvider usuarioAutenticado;
    @Mock private dev.deveps.moteros.services.PushService pushService;

    private NotificacionServiceImpl service;

    private final Usuario destino = Usuario.builder().id(1).uuid("u-1").nombreUsuario("d").nombreCompleto("Destino").build();
    private final Usuario origen = Usuario.builder().id(2).uuid("u-2").nombreUsuario("o").nombreCompleto("Origen").build();

    @BeforeEach
    void setUp() {
        service = new NotificacionServiceImpl(notificacionRepository, publicacionRepository, quedadaRepository,
                conversacionRepository, rutaRepository, usuarioAutenticado, pushService, new EntityDtoMapper());
    }

    @Test
    void notificar_avisaTambienAlMovilDelDestinatario() {
        service.notificar(destino, TipoNotificacion.like, 7, origen, "A Origen le gusta tu publicacion");

        verify(pushService).enviar(eq(destino), anyString(), eq("A Origen le gusta tu publicacion"), anyMap());
    }

    @Test
    void notificar_aUnoMismo_noAvisaAlMovil() {
        service.notificar(destino, TipoNotificacion.like, 7, destino, "algo");

        verifyNoInteractions(pushService);
    }

    @Test
    void notificar_mensaje_sustituyeLaNotificacionSinLeerDeLaMismaConversacion() {
        Notificacion anterior = Notificacion.builder().id(50).usuario(destino).tipo(TipoNotificacion.mensaje).referenciaId(9).build();
        when(notificacionRepository.findFirstByUsuarioIdAndTipoAndReferenciaIdAndLeidoFalse(1, TipoNotificacion.mensaje, 9))
                .thenReturn(Optional.of(anterior));

        service.notificar(destino, TipoNotificacion.mensaje, 9, origen, "Origen te ha enviado un mensaje.");

        verify(notificacionRepository).delete(anterior);
        ArgumentCaptor<Notificacion> guardada = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(guardada.capture());
        assertThat(guardada.getValue().getReferenciaId()).isEqualTo(9);
    }

    @Test
    void notificar_otrosTipos_noBuscanDuplicados() {
        service.notificar(destino, TipoNotificacion.like, 3, origen, "like");

        verify(notificacionRepository, never()).findFirstByUsuarioIdAndTipoAndReferenciaIdAndLeidoFalse(any(), any(), any());
        verify(notificacionRepository).save(any(Notificacion.class));
    }

    @Test
    void listar_resuelveElUuidDeLaReferenciaSegunElTipo() {
        when(usuarioAutenticado.obtenerUuidUsuarioActual()).thenReturn("u-1");
        var pageable = PageRequest.of(0, 20);
        List<Notificacion> notificaciones = List.of(
                Notificacion.builder().uuid("n1").usuario(destino).tipo(TipoNotificacion.like).referenciaId(3).build(),
                Notificacion.builder().uuid("n2").usuario(destino).tipo(TipoNotificacion.mensaje).referenciaId(9).build(),
                Notificacion.builder().uuid("n3").usuario(destino).tipo(TipoNotificacion.solicitud_amistad).usuarioOrigen(origen).build(),
                // Publicacion borrada: no se encuentra y queda sin uuid.
                Notificacion.builder().uuid("n4").usuario(destino).tipo(TipoNotificacion.comentario).referenciaId(404).build());
        when(notificacionRepository.findByUsuarioUuidOrderByFechaCreacionDesc("u-1", pageable))
                .thenReturn(new PageImpl<>(notificaciones, pageable, notificaciones.size()));
        when(publicacionRepository.findAllById(any())).thenReturn(List.of(Publicacion.builder().id(3).uuid("pub-3").build()));
        when(conversacionRepository.findAllById(any())).thenReturn(List.of(Conversacion.builder().id(9).uuid("conv-9").build()));

        List<NotificacionResponseDTO> resultado = service.listar(false, pageable).getContent();

        assertThat(resultado).extracting(NotificacionResponseDTO::getReferenciaUuid)
                .containsExactly("pub-3", "conv-9", null, null);
        verifyNoInteractions(quedadaRepository, rutaRepository);
    }
}
