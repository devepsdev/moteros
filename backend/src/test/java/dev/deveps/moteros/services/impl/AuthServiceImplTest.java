package dev.deveps.moteros.services.impl;

import dev.deveps.moteros.dto.LoginRequestDTO;
import dev.deveps.moteros.dto.LoginResponseDTO;
import dev.deveps.moteros.dto.RefreshTokenRequestDTO;
import dev.deveps.moteros.dto.RegistroUsuarioDTO;
import dev.deveps.moteros.entities.RefreshToken;
import dev.deveps.moteros.entities.Usuario;
import dev.deveps.moteros.exceptions.BadRequestException;
import dev.deveps.moteros.exceptions.DuplicateResourceException;
import dev.deveps.moteros.mapper.EntityDtoMapper;
import dev.deveps.moteros.repositories.AmistadRepository;
import dev.deveps.moteros.repositories.MotoRepository;
import dev.deveps.moteros.repositories.RutaRepository;
import dev.deveps.moteros.repositories.UsuarioRepository;
import dev.deveps.moteros.security.JwtUtil;
import dev.deveps.moteros.security.UsuarioAutenticadoProvider;
import dev.deveps.moteros.services.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private MotoRepository motoRepository;
    @Mock private RutaRepository rutaRepository;
    @Mock private AmistadRepository amistadRepository;
    @Mock private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private UsuarioAutenticadoProvider usuarioAutenticado;
    @Mock private dev.deveps.moteros.security.LoginRateLimiter loginRateLimiter;
    @Mock private dev.deveps.moteros.security.PasswordResetService passwordResetService;
    @Mock private dev.deveps.moteros.services.EmailService emailService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(usuarioRepository, motoRepository, rutaRepository,
                amistadRepository, passwordEncoder, jwtUtil, refreshTokenService, usuarioAutenticado, loginRateLimiter, passwordResetService, emailService,
                new EntityDtoMapper());
        lenient().when(motoRepository.countByUsuarioId(anyInt())).thenReturn(0L);
        lenient().when(rutaRepository.countByCreadorId(anyInt())).thenReturn(0L);
        lenient().when(amistadRepository.countAmigosAceptados(anyInt())).thenReturn(0L);
        lenient().when(jwtUtil.generateToken(anyString(), anyString())).thenReturn("token-jwt");
        lenient().when(jwtUtil.getExpirationSeconds()).thenReturn(900L);
        lenient().when(refreshTokenService.crear(any())).thenAnswer(inv ->
                RefreshToken.builder().token("refresh-nuevo").usuario(inv.getArgument(0)).build());
        // Por defecto ya hay un admin -> los nuevos registros son 'user'
        lenient().when(usuarioRepository.countByRol(dev.deveps.moteros.entities.enums.RolUsuario.admin))
                .thenReturn(1L);
    }

    private RegistroUsuarioDTO registro() {
        return RegistroUsuarioDTO.builder()
                .nombreUsuario("nuevo").nombreCompleto("Nuevo Motero")
                .email("nuevo@test.com").password("password123").ciudad("Madrid").build();
    }

    private Usuario usuario(boolean activo) {
        return Usuario.builder().id(1).uuid("uuid-1").nombreUsuario("nuevo").nombreCompleto("Nuevo Motero")
                .email("nuevo@test.com").passwordHash("hashed").activo(activo)
                .rol(dev.deveps.moteros.entities.enums.RolUsuario.user).build();
    }

    @Test
    void registro_ok_devuelveTokenYUsuario() {
        when(usuarioRepository.existsByEmail("nuevo@test.com")).thenReturn(false);
        when(usuarioRepository.existsByNombreUsuario("nuevo")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(1);
            return u;
        });

        LoginResponseDTO res = authService.registro(registro());

        assertThat(res.getToken()).isEqualTo("token-jwt");
        assertThat(res.getUsuario().getEmail()).isEqualTo("nuevo@test.com");
        assertThat(res.getUsuario().getRol()).isEqualTo(dev.deveps.moteros.entities.enums.RolUsuario.user);
    }

    @Test
    void registro_primerUsuarioDeLaPlataforma_esAdmin() {
        when(usuarioRepository.countByRol(dev.deveps.moteros.entities.enums.RolUsuario.admin)).thenReturn(0L);
        when(usuarioRepository.existsByEmail("nuevo@test.com")).thenReturn(false);
        when(usuarioRepository.existsByNombreUsuario("nuevo")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(1);
            return u;
        });

        LoginResponseDTO res = authService.registro(registro());

        assertThat(res.getUsuario().getRol()).isEqualTo(dev.deveps.moteros.entities.enums.RolUsuario.admin);
    }

    @Test
    void registro_emailDuplicado_lanzaExcepcion() {
        when(usuarioRepository.existsByEmail("nuevo@test.com")).thenReturn(true);
        assertThatThrownBy(() -> authService.registro(registro()))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void registro_nombreUsuarioDuplicado_lanzaExcepcion() {
        when(usuarioRepository.existsByEmail("nuevo@test.com")).thenReturn(false);
        when(usuarioRepository.existsByNombreUsuario("nuevo")).thenReturn(true);
        assertThatThrownBy(() -> authService.registro(registro()))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void login_ok() {
        when(usuarioRepository.findByEmailOrNombreUsuario("nuevo", "nuevo")).thenReturn(Optional.of(usuario(true)));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);

        LoginResponseDTO res = authService.login(new LoginRequestDTO("nuevo", "password123"), "1.2.3.4");

        assertThat(res.getToken()).isEqualTo("token-jwt");
        assertThat(res.getRefreshToken()).isEqualTo("refresh-nuevo");
        assertThat(res.getExpiresIn()).isEqualTo(900L);
        assertThat(res.getUsuario().getEmail()).isEqualTo("nuevo@test.com");
    }

    @Test
    void login_ok_reseteaElContadorDelRateLimiter() {
        when(usuarioRepository.findByEmailOrNombreUsuario("nuevo", "nuevo")).thenReturn(Optional.of(usuario(true)));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);

        authService.login(new LoginRequestDTO("nuevo", "password123"), "1.2.3.4");

        org.mockito.Mockito.verify(loginRateLimiter).checkAllowed("nuevo", "1.2.3.4");
        org.mockito.Mockito.verify(loginRateLimiter).recordSuccess("nuevo", "1.2.3.4");
        org.mockito.Mockito.verify(loginRateLimiter, org.mockito.Mockito.never()).recordFailure(any(), any());
    }

    @Test
    void login_fallido_registraElIntento() {
        when(usuarioRepository.findByEmailOrNombreUsuario("nuevo", "nuevo")).thenReturn(Optional.of(usuario(true)));
        when(passwordEncoder.matches("mala", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequestDTO("nuevo", "mala"), "1.2.3.4"))
                .isInstanceOf(BadRequestException.class);

        org.mockito.Mockito.verify(loginRateLimiter).recordFailure("nuevo", "1.2.3.4");
        org.mockito.Mockito.verify(loginRateLimiter, org.mockito.Mockito.never()).recordSuccess(any(), any());
    }

    @Test
    void login_bloqueado_noConsultaNiLaBBDD() {
        org.mockito.Mockito.doThrow(new dev.deveps.moteros.exceptions.TooManyRequestsException("bloqueado", 60))
                .when(loginRateLimiter).checkAllowed("nuevo", "1.2.3.4");

        assertThatThrownBy(() -> authService.login(new LoginRequestDTO("nuevo", "password123"), "1.2.3.4"))
                .isInstanceOf(dev.deveps.moteros.exceptions.TooManyRequestsException.class);

        org.mockito.Mockito.verifyNoInteractions(usuarioRepository, passwordEncoder);
    }

    @Test
    void recuperarPassword_emailExistente_generaCodigoYEnviaEmail() {
        when(usuarioRepository.findByEmail("nuevo@test.com")).thenReturn(Optional.of(usuario(true)));
        when(passwordResetService.createResetCode(1)).thenReturn("123456");

        authService.recuperarPassword(new dev.deveps.moteros.dto.RecuperarPasswordDTO("nuevo@test.com"), "1.2.3.4");

        org.mockito.Mockito.verify(emailService).enviarCodigoRecuperacion("nuevo@test.com", "Nuevo Motero", "123456");
        org.mockito.Mockito.verify(loginRateLimiter).checkAllowed("reset:nuevo@test.com", "reset:1.2.3.4");
    }

    @Test
    void recuperarPassword_emailInexistente_noEnviaNadaNiFalla() {
        when(usuarioRepository.findByEmail("nadie@test.com")).thenReturn(Optional.empty());

        authService.recuperarPassword(new dev.deveps.moteros.dto.RecuperarPasswordDTO("nadie@test.com"), "1.2.3.4");

        org.mockito.Mockito.verifyNoInteractions(emailService, passwordResetService);
    }

    @Test
    void recuperarPassword_siFallaElEmail_respondeIgual() {
        when(usuarioRepository.findByEmail("nuevo@test.com")).thenReturn(Optional.of(usuario(true)));
        when(passwordResetService.createResetCode(1)).thenReturn("123456");
        org.mockito.Mockito.doThrow(new IllegalStateException("smtp caido"))
                .when(emailService).enviarCodigoRecuperacion(any(), any(), any());

        org.assertj.core.api.Assertions.assertThatNoException().isThrownBy(() -> authService.recuperarPassword(
                new dev.deveps.moteros.dto.RecuperarPasswordDTO("nuevo@test.com"), "1.2.3.4"));
    }

    @Test
    void restablecerPassword_ok_cambiaPasswordYCierraSesiones() {
        Usuario u = usuario(true);
        when(usuarioRepository.findByEmail("nuevo@test.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.encode("nueva12345")).thenReturn("hash-nuevo");

        authService.restablecerPassword(new dev.deveps.moteros.dto.RestablecerPasswordDTO(
                "nuevo@test.com", "123456", "nueva12345"));

        org.mockito.Mockito.verify(passwordResetService).verifyCode(1, "123456");
        assertThat(u.getPasswordHash()).isEqualTo("hash-nuevo");
        org.mockito.Mockito.verify(refreshTokenService).revocarTodos(1);
    }

    @Test
    void restablecerPassword_codigoInvalido_noCambiaNada() {
        Usuario u = usuario(true);
        when(usuarioRepository.findByEmail("nuevo@test.com")).thenReturn(Optional.of(u));
        org.mockito.Mockito.doThrow(new BadRequestException("El codigo no es valido o ha caducado"))
                .when(passwordResetService).verifyCode(1, "000000");

        assertThatThrownBy(() -> authService.restablecerPassword(new dev.deveps.moteros.dto.RestablecerPasswordDTO(
                "nuevo@test.com", "000000", "nueva12345"))).isInstanceOf(BadRequestException.class);

        assertThat(u.getPasswordHash()).isEqualTo("hashed");
        org.mockito.Mockito.verify(refreshTokenService, org.mockito.Mockito.never()).revocarTodos(any());
    }

    @Test
    void refrescar_rotaYDevuelveNuevoPar() {
        Usuario u = usuario(true);
        when(refreshTokenService.validarYRotar("rt-viejo")).thenReturn(
                RefreshToken.builder().token("rt-rotado").usuario(u).build());

        LoginResponseDTO res = authService.refrescar(new RefreshTokenRequestDTO("rt-viejo"));

        assertThat(res.getRefreshToken()).isEqualTo("rt-rotado");
        assertThat(res.getToken()).isEqualTo("token-jwt");
    }

    @Test
    void refrescar_tokenInvalido_propagaBadRequest() {
        when(refreshTokenService.validarYRotar("malo"))
                .thenThrow(new BadRequestException("Refresh token invalido"));
        assertThatThrownBy(() -> authService.refrescar(new RefreshTokenRequestDTO("malo")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void logout_revocaElRefreshToken() {
        authService.logout(new RefreshTokenRequestDTO("rt-1"));
        org.mockito.Mockito.verify(refreshTokenService).revocar("rt-1");
    }

    @Test
    void cambiarPassword_revocaTodasLasSesiones() {
        Usuario u = usuario(true);
        when(usuarioAutenticado.obtenerUsuarioActual()).thenReturn(u);
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(passwordEncoder.encode("password456")).thenReturn("hashed2");

        authService.cambiarPassword(new dev.deveps.moteros.dto.CambioPasswordDTO("password123", "password456"));

        org.mockito.Mockito.verify(refreshTokenService).revocarTodos(u.getId());
    }

    @Test
    void login_passwordIncorrecta_lanzaBadRequest() {
        when(usuarioRepository.findByEmailOrNombreUsuario("nuevo", "nuevo")).thenReturn(Optional.of(usuario(true)));
        when(passwordEncoder.matches("mala", "hashed")).thenReturn(false);
        assertThatThrownBy(() -> authService.login(new LoginRequestDTO("nuevo", "mala"), "1.2.3.4"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void login_usuarioInactivo_lanzaBadRequest() {
        when(usuarioRepository.findByEmailOrNombreUsuario("nuevo", "nuevo")).thenReturn(Optional.of(usuario(false)));
        assertThatThrownBy(() -> authService.login(new LoginRequestDTO("nuevo", "password123"), "1.2.3.4"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void login_usuarioNoExiste_lanzaBadRequest() {
        when(usuarioRepository.findByEmailOrNombreUsuario("x", "x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.login(new LoginRequestDTO("x", "y"), "1.2.3.4"))
                .isInstanceOf(BadRequestException.class);
    }
}
