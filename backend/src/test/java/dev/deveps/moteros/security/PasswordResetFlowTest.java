package dev.deveps.moteros.security;

import dev.deveps.moteros.dto.RestablecerPasswordDTO;
import dev.deveps.moteros.entities.PasswordResetToken;
import dev.deveps.moteros.entities.Usuario;
import dev.deveps.moteros.exceptions.BadRequestException;
import dev.deveps.moteros.exceptions.TooManyRequestsException;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Flujo real (beans con proxy transaccional y commit en H2): comprueba que los intentos
 * fallidos de codigo quedan guardados aunque la peticion termine en error, que el limite
 * no se puede saltar con peticiones en paralelo, y el rate limit propio del endpoint.
 */
@SpringBootTest
@ActiveProfiles("test")
class PasswordResetFlowTest {

    private static final String EMAIL = "reset@test.com";
    private static final String IP = "10.0.0.1";

    @Autowired private AuthService authService;
    @Autowired private PasswordResetService passwordResetService;
    @Autowired private PasswordResetTokenRepository tokenRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private LoginRateLimiter loginRateLimiter;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        limpiarLimitador();
        usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("reset_user").nombreCompleto("Reset User").email(EMAIL)
                .passwordHash(passwordEncoder.encode("vieja12345")).activo(true).build());
    }

    @AfterEach
    void tearDown() {
        tokenRepository.deleteAll();
        usuarioRepository.deleteAll();
        limpiarLimitador();
    }

    private void limpiarLimitador() {
        loginRateLimiter.recordSuccess("reset-verify:" + EMAIL, "reset-verify:" + IP);
    }

    private void restablecer(String codigo) {
        authService.restablecerPassword(new RestablecerPasswordDTO(EMAIL, codigo, "nueva12345"), IP);
    }

    private static String malo(String codigo) {
        return codigo.equals("000000") ? "111111" : "000000";
    }

    @Test
    void codigoCorrecto_cambiaLaContrasenaYConsumeElCodigo() {
        String codigo = passwordResetService.createResetCode(usuario.getId());

        restablecer(codigo);

        Usuario actualizado = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("nueva12345", actualizado.getPasswordHash())).isTrue();
        assertThatThrownBy(() -> restablecer(codigo)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void codigoIncorrecto_elIntentoQuedaGuardado() {
        String codigo = passwordResetService.createResetCode(usuario.getId());

        assertThatThrownBy(() -> restablecer(malo(codigo))).isInstanceOf(BadRequestException.class);

        PasswordResetToken token = tokenRepository.findByUsuarioIdAndUsadoFalse(usuario.getId()).orElseThrow();
        assertThat(token.getIntentos()).isEqualTo(1);
    }

    @Test
    void trasCincoFallos_niElCodigoCorrectoSirve_yElEndpointQuedaLimitado() {
        String codigo = passwordResetService.createResetCode(usuario.getId());
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> restablecer(malo(codigo))).isInstanceOf(BadRequestException.class);
        }

        // El codigo ha agotado sus intentos: ni el correcto sirve.
        assertThatThrownBy(() -> passwordResetService.verifyCode(usuario.getId(), codigo))
                .isInstanceOf(BadRequestException.class);
        // Y el endpoint esta limitado para ese email/IP.
        assertThatThrownBy(() -> restablecer(codigo)).isInstanceOf(TooManyRequestsException.class);

        Usuario sinCambios = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("vieja12345", sinCambios.getPasswordHash())).isTrue();
    }

    @Test
    void peticionesEnParalelo_noSuperanElMaximoDeIntentos() throws Exception {
        String codigo = passwordResetService.createResetCode(usuario.getId());
        String incorrecto = malo(codigo);

        ExecutorService pool = Executors.newFixedThreadPool(10);
        try {
            List<Callable<Boolean>> tareas = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                tareas.add(() -> {
                    try {
                        passwordResetService.verifyCode(usuario.getId(), incorrecto);
                        return true;
                    } catch (BadRequestException ex) {
                        return false;
                    }
                });
            }
            for (Future<Boolean> f : pool.invokeAll(tareas)) {
                assertThat(f.get()).isFalse();
            }
        } finally {
            pool.shutdown();
        }

        PasswordResetToken token = tokenRepository.findByUsuarioIdAndUsadoFalse(usuario.getId()).orElseThrow();
        assertThat(token.getIntentos()).isEqualTo(5);
        assertThatThrownBy(() -> passwordResetService.verifyCode(usuario.getId(), codigo))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void codigoCaducado_noSirve() {
        String codigo = passwordResetService.createResetCode(usuario.getId());
        PasswordResetToken token = tokenRepository.findByUsuarioIdAndUsadoFalse(usuario.getId()).orElseThrow();
        token.setFechaExpiracion(LocalDateTime.now().minusMinutes(1));
        tokenRepository.save(token);

        assertThatThrownBy(() -> restablecer(codigo)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void pedirUnCodigoNuevo_invalidaElAnterior() {
        String primero = passwordResetService.createResetCode(usuario.getId());
        String segundo = passwordResetService.createResetCode(usuario.getId());

        if (!primero.equals(segundo)) {
            assertThatThrownBy(() -> restablecer(primero)).isInstanceOf(BadRequestException.class);
        }
        restablecer(segundo);
    }
}
