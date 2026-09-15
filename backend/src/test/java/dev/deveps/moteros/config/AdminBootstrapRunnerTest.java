package dev.deveps.moteros.config;

import dev.deveps.moteros.entities.Usuario;
import dev.deveps.moteros.entities.enums.RolUsuario;
import dev.deveps.moteros.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapRunnerTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private AdminBootstrapRunner runner;

    @BeforeEach
    void setUp() {
        runner = new AdminBootstrapRunner(usuarioRepository, passwordEncoder);
    }

    private void configurar(String email, String password, String nombreUsuario) {
        ReflectionTestUtils.setField(runner, "email", email);
        ReflectionTestUtils.setField(runner, "password", password);
        ReflectionTestUtils.setField(runner, "nombreUsuario", nombreUsuario);
        ReflectionTestUtils.setField(runner, "nombreCompleto", "Kike");
    }

    @Test
    void sinEmail_noHaceNada() {
        configurar("", "secreta123", "kike");
        runner.run(null);
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void creaElAdministradorSiNoExiste() {
        configurar(" kike@test.com ", "secreta123", "kike");
        when(usuarioRepository.findByEmail("kike@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secreta123")).thenReturn("hash");

        runner.run(null);

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(guardado.capture());
        assertThat(guardado.getValue().getRol()).isEqualTo(RolUsuario.admin);
        assertThat(guardado.getValue().getNombreUsuario()).isEqualTo("kike");
        assertThat(guardado.getValue().getEmail()).isEqualTo("kike@test.com");
        assertThat(guardado.getValue().getPasswordHash()).isEqualTo("hash");
    }

    @Test
    void siYaExiste_loAscienteSinTocarLaContrasena() {
        // Sin contraseña en el .env: es el caso normal una vez creada la cuenta.
        configurar("kike@test.com", "", "kike");
        Usuario existente = Usuario.builder().email("kike@test.com").passwordHash("hash-original")
                .rol(RolUsuario.user).activo(false).build();
        when(usuarioRepository.findByEmail("kike@test.com")).thenReturn(Optional.of(existente));

        runner.run(null);

        verify(usuarioRepository).save(existente);
        assertThat(existente.getRol()).isEqualTo(RolUsuario.admin);
        assertThat(existente.getActivo()).isTrue();
        assertThat(existente.getPasswordHash()).isEqualTo("hash-original");
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void noCreaNadaConContrasenaCortaONombreDeUsuarioOcupado() {
        configurar("kike@test.com", "corta", "kike");
        when(usuarioRepository.findByEmail("kike@test.com")).thenReturn(Optional.empty());
        runner.run(null);

        configurar("kike@test.com", "secreta123", "kike");
        when(usuarioRepository.existsByNombreUsuario("kike")).thenReturn(true);
        runner.run(null);

        verify(usuarioRepository, never()).save(any());
    }
}
