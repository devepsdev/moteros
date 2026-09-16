package dev.deveps.moteros.services.impl;

import dev.deveps.moteros.entities.Amistad;
import dev.deveps.moteros.entities.Bloqueo;
import dev.deveps.moteros.entities.Usuario;
import dev.deveps.moteros.entities.enums.EstadoAmistad;
import dev.deveps.moteros.exceptions.BadRequestException;
import dev.deveps.moteros.exceptions.ResourceNotFoundException;
import dev.deveps.moteros.mapper.EntityDtoMapper;
import dev.deveps.moteros.repositories.AmistadRepository;
import dev.deveps.moteros.repositories.BloqueoRepository;
import dev.deveps.moteros.repositories.UsuarioRepository;
import dev.deveps.moteros.security.UsuarioAutenticadoProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BloqueoServiceImplTest {

    @Mock private BloqueoRepository bloqueoRepository;
    @Mock private AmistadRepository amistadRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UsuarioAutenticadoProvider usuarioAutenticado;

    private BloqueoServiceImpl service;
    private Usuario yo;
    private Usuario otro;

    @BeforeEach
    void setUp() {
        service = new BloqueoServiceImpl(bloqueoRepository, amistadRepository, usuarioRepository,
                usuarioAutenticado, new EntityDtoMapper());
        yo = Usuario.builder().id(1).uuid("uuid-yo").nombreUsuario("yo").build();
        otro = Usuario.builder().id(2).uuid("uuid-otro").nombreUsuario("otro").build();
    }

    @Test
    void bloquear_aSiMismo_lanzaBadRequest() {
        when(usuarioAutenticado.obtenerUsuarioActual()).thenReturn(yo);
        when(usuarioRepository.findByUuid("uuid-yo")).thenReturn(Optional.of(yo));

        assertThatThrownBy(() -> service.bloquear("uuid-yo")).isInstanceOf(BadRequestException.class);
        verify(bloqueoRepository, never()).save(any());
    }

    @Test
    void bloquear_borraLaAmistadYGuardaElBloqueo() {
        Amistad amistad = Amistad.builder().usuario(otro).amigo(yo).estado(EstadoAmistad.aceptada).build();
        when(usuarioAutenticado.obtenerUsuarioActual()).thenReturn(yo);
        when(usuarioRepository.findByUuid("uuid-otro")).thenReturn(Optional.of(otro));
        when(amistadRepository.findRelacion(1, 2)).thenReturn(Optional.of(amistad));

        service.bloquear("uuid-otro");

        verify(amistadRepository).delete(amistad);
        ArgumentCaptor<Bloqueo> captor = ArgumentCaptor.forClass(Bloqueo.class);
        verify(bloqueoRepository).save(captor.capture());
        assertThat(captor.getValue().getBloqueador()).isEqualTo(yo);
        assertThat(captor.getValue().getBloqueado()).isEqualTo(otro);
    }

    @Test
    void bloquear_yaBloqueado_noHaceNada() {
        when(usuarioAutenticado.obtenerUsuarioActual()).thenReturn(yo);
        when(usuarioRepository.findByUuid("uuid-otro")).thenReturn(Optional.of(otro));
        when(bloqueoRepository.existsByBloqueadorIdAndBloqueadoId(1, 2)).thenReturn(true);

        service.bloquear("uuid-otro");

        verify(bloqueoRepository, never()).save(any());
    }

    @Test
    void exigirSinBloqueo_conBloqueo_respondeComoSiNoExistiera() {
        when(usuarioAutenticado.obtenerIdUsuarioActual()).thenReturn(1);
        when(bloqueoRepository.existeEntre(1, 2)).thenReturn(true);

        assertThatThrownBy(() -> service.exigirSinBloqueo(2)).isInstanceOf(ResourceNotFoundException.class);
    }
}
