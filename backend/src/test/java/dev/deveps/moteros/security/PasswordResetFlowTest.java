package dev.deveps.moteros.security;

import dev.deveps.moteros.dto.RestablecerPasswordDTO;
import dev.deveps.moteros.entities.PasswordResetToken;
import dev.deveps.moteros.entities.Usuario;
import dev.deveps.moteros.exceptions.BadRequestException;
import dev.deveps.moteros.repositories.PasswordResetTokenRepository;
import dev.deveps.moteros.repositories.UsuarioRepository;
import dev.deveps.moteros.services.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Flujo real (beans con proxy transaccional y commit en H2): comprueba que los intentos
 * fallidos de codigo quedan guardados aunque la peticion termine en error, que es lo que
 * hace efectivo el bloqueo tras 5 intentos.
 */
@SpringBootTest
@ActiveProfiles("test")
class PasswordResetFlowTest {

    @Autowired private AuthService authService;
    @Autowired private PasswordResetService passwordResetService;
    @Autowired private PasswordResetTokenRepository tokenRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("reset_user").nombreCompleto("Reset User").email("reset@test.com")
                .passwordHash(passwordEncoder.encode("vieja12345")).activo(true).build());
    }

    @AfterEach
    void tearDown() {
        tokenRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    private RestablecerPasswordDTO dto(String codigo) {
        return new RestablecerPasswordDTO("reset@test.com", codigo, "nueva12345");
    }

    @Test
    void codigoCorrecto_cambiaLaContrasenaYConsumeElCodigo() {
        String codigo = passwordResetService.createResetCode(usuario.getId());

        authService.restablecerPassword(dto(codigo));

        Usuario actualizado = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("nueva12345", actualizado.getPasswordHash())).isTrue();
        assertThatThrownBy(() -> authService.restablecerPassword(dto(codigo)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void codigoIncorrecto_elIntentoQuedaGuardado() {
        String codigo = passwordResetService.createResetCode(usuario.getId());
        String malo = codigo.equals("000000") ? "111111" : "000000";

        assertThatThrownBy(() -> authService.restablecerPassword(dto(malo)))
                .isInstanceOf(BadRequestException.class);

        PasswordResetToken token = tokenRepository.findByUsuarioIdAndUsadoFalse(usuario.getId()).orElseThrow();
        assertThat(token.getIntentos()).isEqualTo(1);
    }

    @Test
    void trasCincoFallos_niElCodigoCorrectoSirve() {
        String codigo = passwordResetService.createResetCode(usuario.getId());
        String malo = codigo.equals("000000") ? "111111" : "000000";
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> authService.restablecerPassword(dto(malo)))
                    .isInstanceOf(BadRequestException.class);
        }

        assertThatThrownBy(() -> authService.restablecerPassword(dto(codigo)))
                .isInstanceOf(BadRequestException.class);
        Usuario sinCambios = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("vieja12345", sinCambios.getPasswordHash())).isTrue();
    }

    @Test
    void codigoCaducado_noSirve() {
        String codigo = passwordResetService.createResetCode(usuario.getId());
        PasswordResetToken token = tokenRepository.findByUsuarioIdAndUsadoFalse(usuario.getId()).orElseThrow();
        token.setFechaExpiracion(LocalDateTime.now().minusMinutes(1));
        tokenRepository.save(token);

        assertThatThrownBy(() -> authService.restablecerPassword(dto(codigo)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void pedirUnCodigoNuevo_invalidaElAnterior() {
        String primero = passwordResetService.createResetCode(usuario.getId());
        String segundo = passwordResetService.createResetCode(usuario.getId());

        if (!primero.equals(segundo)) {
            assertThatThrownBy(() -> authService.restablecerPassword(dto(primero)))
                    .isInstanceOf(BadRequestException.class);
        }
        authService.restablecerPassword(dto(segundo));
    }
}
