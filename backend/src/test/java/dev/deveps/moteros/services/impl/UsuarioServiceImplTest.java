package dev.deveps.moteros.services.impl;

import dev.deveps.moteros.entities.Usuario;
import dev.deveps.moteros.entities.enums.RolUsuario;
import dev.deveps.moteros.exceptions.BadRequestException;
import dev.deveps.moteros.mapper.EntityDtoMapper;
import dev.deveps.moteros.repositories.AmistadRepository;
import dev.deveps.moteros.repositories.MotoRepository;
import dev.deveps.moteros.repositories.PublicacionRepository;
import dev.deveps.moteros.repositories.RutaRepository;
import dev.deveps.moteros.repositories.UsuarioRepository;
import dev.deveps.moteros.security.UsuarioAutenticadoProvider;
import dev.deveps.moteros.services.AlmacenamientoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private MotoRepository motoRepository;
    @Mock private RutaRepository rutaRepository;
    @Mock private AmistadRepository amistadRepository;
    @Mock private PublicacionRepository publicacionRepository;
    @Mock private AlmacenamientoService almacenamientoService;
    @Mock private UsuarioAutenticadoProvider usuarioAutenticado;

    private UsuarioServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UsuarioServiceImpl(usuarioRepository, motoRepository, rutaRepository, amistadRepository,
                publicacionRepository, almacenamientoService, usuarioAutenticado, new EntityDtoMapper());
    }

    private Usuario usuario(RolUsuario rol) {
        return Usuario.builder().id(7).uuid("u-7").nombreUsuario("motero").nombreCompleto("Motero")
                .email("m@test.com").passwordHash("h").activo(true).rol(rol)
                .fotoPerfilUrl("/uploads/perfil.png").build();
    }

    @Test
    void eliminarCuenta_borraUsuarioYTodasSusImagenes() {
        Usuario u = usuario(RolUsuario.user);
        when(usuarioAutenticado.obtenerUsuarioActual()).thenReturn(u);
        when(motoRepository.fotosDeUsuario(7)).thenReturn(List.of("/uploads/moto.jpg"));
        when(publicacionRepository.imagenesDeUsuario(7)).thenReturn(List.of("/uploads/post1.webp", "/uploads/post2.gif"));

        service.eliminarCuentaActual();

        verify(usuarioRepository).delete(u);
        verify(almacenamientoService).eliminarPorUrl("/uploads/perfil.png");
        verify(almacenamientoService).eliminarPorUrl("/uploads/moto.jpg");
        verify(almacenamientoService).eliminarPorUrl("/uploads/post1.webp");
        verify(almacenamientoService).eliminarPorUrl("/uploads/post2.gif");
    }

    @Test
    void eliminarCuenta_unicoAdmin_noSePermite() {
        when(usuarioAutenticado.obtenerUsuarioActual()).thenReturn(usuario(RolUsuario.admin));
        when(usuarioRepository.countByRol(RolUsuario.admin)).thenReturn(1L);

        assertThatThrownBy(() -> service.eliminarCuentaActual()).isInstanceOf(BadRequestException.class);

        verify(usuarioRepository, never()).delete(any());
        verify(almacenamientoService, never()).eliminarPorUrl(any());
    }

    @Test
    void eliminarCuenta_adminConOtrosAdmins_siSePermite() {
        Usuario u = usuario(RolUsuario.admin);
        when(usuarioAutenticado.obtenerUsuarioActual()).thenReturn(u);
        when(usuarioRepository.countByRol(RolUsuario.admin)).thenReturn(2L);

        service.eliminarCuentaActual();

        verify(usuarioRepository).delete(u);
    }
}
