package dev.deveps.moteros.services.impl;

import dev.deveps.moteros.dto.DenunciaRequestDTO;
import dev.deveps.moteros.dto.DenunciaResponseDTO;
import dev.deveps.moteros.dto.ResolverDenunciaDTO;
import dev.deveps.moteros.entities.Conversacion;
import dev.deveps.moteros.entities.Denuncia;
import dev.deveps.moteros.entities.Mensaje;
import dev.deveps.moteros.entities.Publicacion;
import dev.deveps.moteros.entities.Usuario;
import dev.deveps.moteros.entities.enums.EstadoDenuncia;
import dev.deveps.moteros.entities.enums.MotivoDenuncia;
import dev.deveps.moteros.entities.enums.RolUsuario;
import dev.deveps.moteros.entities.enums.TipoDenuncia;
import dev.deveps.moteros.exceptions.BadRequestException;
import dev.deveps.moteros.exceptions.ResourceNotFoundException;
import dev.deveps.moteros.exceptions.TooManyRequestsException;
import dev.deveps.moteros.mapper.EntityDtoMapper;
import dev.deveps.moteros.repositories.ComentarioRepository;
import dev.deveps.moteros.repositories.DenunciaRepository;
import dev.deveps.moteros.repositories.MensajeRepository;
import dev.deveps.moteros.repositories.PublicacionRepository;
import dev.deveps.moteros.repositories.QuedadaRepository;
import dev.deveps.moteros.repositories.RutaRepository;
import dev.deveps.moteros.repositories.UsuarioRepository;
import dev.deveps.moteros.security.UsuarioAutenticadoProvider;
import dev.deveps.moteros.services.AdminService;
import dev.deveps.moteros.services.AlmacenamientoService;
import dev.deveps.moteros.services.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DenunciaServiceImplTest {

    @Mock private DenunciaRepository denunciaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PublicacionRepository publicacionRepository;
    @Mock private ComentarioRepository comentarioRepository;
    @Mock private MensajeRepository mensajeRepository;
    @Mock private RutaRepository rutaRepository;
    @Mock private QuedadaRepository quedadaRepository;
    @Mock private UsuarioAutenticadoProvider usuarioAutenticado;
    @Mock private AdminService adminService;
    @Mock private AlmacenamientoService almacenamientoService;
    @Mock private EmailService emailService;

    private DenunciaServiceImpl service;
    private Usuario yo;
    private Usuario autor;
    private Usuario admin;

    @BeforeEach
    void setUp() {
        service = new DenunciaServiceImpl(denunciaRepository, usuarioRepository, publicacionRepository,
                comentarioRepository, mensajeRepository, rutaRepository, quedadaRepository, usuarioAutenticado,
                adminService, almacenamientoService, emailService, new EntityDtoMapper());
        yo = Usuario.builder().id(1).uuid("uuid-yo").nombreUsuario("yo").activo(true).build();
        autor = Usuario.builder().id(2).uuid("uuid-autor").nombreUsuario("autor").activo(true).build();
        admin = Usuario.builder().id(3).uuid("uuid-admin").nombreUsuario("admin").email("admin@test.com")
                .rol(RolUsuario.admin).activo(true).build();
    }

    private DenunciaRequestDTO denunciaDe(TipoDenuncia tipo, String uuid) {
        return DenunciaRequestDTO.builder().tipo(tipo).referenciaUuid(uuid).motivo(MotivoDenuncia.acoso).build();
    }

    @Test
    void denunciar_publicacion_guardaCopiaDelContenidoYAvisaAlAdmin() {
        when(usuarioAutenticado.obtenerUsuarioActual()).thenReturn(yo);
        when(publicacionRepository.findByUuid("p-1")).thenReturn(Optional.of(
                Publicacion.builder().uuid("p-1").usuario(autor).contenido("texto ofensivo").imagenUrl("/uploads/a.jpg").build()));
        when(usuarioRepository.findByRolAndActivoTrue(RolUsuario.admin)).thenReturn(List.of(admin));

        service.denunciar(denunciaDe(TipoDenuncia.publicacion, "p-1"));

        ArgumentCaptor<Denuncia> captor = ArgumentCaptor.forClass(Denuncia.class);
        verify(denunciaRepository).save(captor.capture());
        Denuncia d = captor.getValue();
        assertThat(d.getDenunciado()).isEqualTo(autor);
        assertThat(d.getContenido()).isEqualTo("texto ofensivo");
        assertThat(d.getImagenUrl()).isEqualTo("/uploads/a.jpg");
        assertThat(d.getEstado()).isEqualTo(EstadoDenuncia.pendiente);
    }

    @Test
    void denunciar_contenidoPropio_lanzaBadRequest() {
        when(usuarioAutenticado.obtenerUsuarioActual()).thenReturn(yo);
        when(publicacionRepository.findByUuid("p-1")).thenReturn(Optional.of(
                Publicacion.builder().uuid("p-1").usuario(yo).contenido("mio").build()));

        assertThatThrownBy(() -> service.denunciar(denunciaDe(TipoDenuncia.publicacion, "p-1")))
                .isInstanceOf(BadRequestException.class);
        verify(denunciaRepository, never()).save(any());
    }

    @Test
    void denunciar_repetida_noDuplica() {
        when(usuarioAutenticado.obtenerUsuarioActual()).thenReturn(yo);
        when(usuarioRepository.findByUuid("uuid-autor")).thenReturn(Optional.of(autor));
        when(denunciaRepository.existsByDenuncianteIdAndTipoAndReferenciaUuidAndEstado(
                1, TipoDenuncia.usuario, "uuid-autor", EstadoDenuncia.pendiente)).thenReturn(true);

        service.denunciar(denunciaDe(TipoDenuncia.usuario, "uuid-autor"));

        verify(denunciaRepository, never()).save(any());
    }

    @Test
    void denunciar_demasiadasEnUnDia_lanzaTooManyRequests() {
        when(usuarioAutenticado.obtenerUsuarioActual()).thenReturn(yo);
        when(usuarioRepository.findByUuid("uuid-autor")).thenReturn(Optional.of(autor));
        when(denunciaRepository.countByDenuncianteIdAndFechaCreacionAfter(eq(1), any()))
                .thenReturn((long) DenunciaServiceImpl.MAX_DENUNCIAS_DIA);

        assertThatThrownBy(() -> service.denunciar(denunciaDe(TipoDenuncia.usuario, "uuid-autor")))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void denunciar_mensajeDeConversacionAjena_404() {
        Usuario otro = Usuario.builder().id(9).build();
        Conversacion ajena = Conversacion.builder().usuario1(autor).usuario2(otro).build();
        when(usuarioAutenticado.obtenerUsuarioActual()).thenReturn(yo);
        when(mensajeRepository.findByUuid("m-1")).thenReturn(Optional.of(
                Mensaje.builder().uuid("m-1").conversacion(ajena).remitente(autor).contenido("hola").build()));

        assertThatThrownBy(() -> service.denunciar(denunciaDe(TipoDenuncia.mensaje, "m-1")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void resolver_eliminaContenidoDaDeBajaYCierraLasDemasPendientes() {
        Publicacion publicacion = Publicacion.builder().uuid("p-1").usuario(autor).contenido("x").build();
        Denuncia d1 = Denuncia.builder().id(10).uuid("d-1").denunciante(yo).denunciado(autor)
                .tipo(TipoDenuncia.publicacion).referenciaUuid("p-1").motivo(MotivoDenuncia.odio)
                .estado(EstadoDenuncia.pendiente).build();
        Denuncia d2 = Denuncia.builder().id(11).uuid("d-2").denunciado(autor)
                .tipo(TipoDenuncia.publicacion).referenciaUuid("p-1").motivo(MotivoDenuncia.spam)
                .estado(EstadoDenuncia.pendiente).build();
        when(denunciaRepository.findByUuid("d-1")).thenReturn(Optional.of(d1));
        when(usuarioAutenticado.obtenerUsuarioActual()).thenReturn(admin);
        when(publicacionRepository.findByUuid("p-1")).thenReturn(Optional.of(publicacion), Optional.empty());
        when(denunciaRepository.findByTipoAndReferenciaUuidAndEstado(TipoDenuncia.publicacion, "p-1", EstadoDenuncia.pendiente))
                .thenReturn(List.of(d1, d2));

        DenunciaResponseDTO res = service.resolver("d-1",
                ResolverDenunciaDTO.builder().eliminarContenido(true).darDeBaja(true).nota("insultos").build());

        verify(publicacionRepository).delete(publicacion);
        verify(adminService).cambiarActivo("uuid-autor", false);
        verify(denunciaRepository).saveAll(anyList());
        assertThat(res.getEstado()).isEqualTo(EstadoDenuncia.resuelta);
        assertThat(res.isContenidoExiste()).isFalse();
        assertThat(d2.getEstado()).isEqualTo(EstadoDenuncia.resuelta);
        assertThat(d2.getNotaResolucion()).isEqualTo("insultos");
        assertThat(d2.getResueltaPor()).isEqualTo(admin);
    }

    @Test
    void resolver_sinMedidas_quedaDescartada() {
        Denuncia d = Denuncia.builder().id(10).uuid("d-1").denunciado(autor)
                .tipo(TipoDenuncia.usuario).referenciaUuid("uuid-autor").motivo(MotivoDenuncia.otro)
                .estado(EstadoDenuncia.pendiente).build();
        when(denunciaRepository.findByUuid("d-1")).thenReturn(Optional.of(d));
        when(usuarioAutenticado.obtenerUsuarioActual()).thenReturn(admin);

        DenunciaResponseDTO res = service.resolver("d-1", new ResolverDenunciaDTO());

        assertThat(res.getEstado()).isEqualTo(EstadoDenuncia.descartada);
        verify(adminService, never()).cambiarActivo(any(), eq(false));
    }

    @Test
    void resolver_yaCerrada_lanzaBadRequest() {
        when(denunciaRepository.findByUuid("d-1")).thenReturn(Optional.of(
                Denuncia.builder().uuid("d-1").estado(EstadoDenuncia.resuelta).build()));

        assertThatThrownBy(() -> service.resolver("d-1", new ResolverDenunciaDTO()))
                .isInstanceOf(BadRequestException.class);
        verify(denunciaRepository, never()).countByDenunciadoId(anyInt());
    }
}
