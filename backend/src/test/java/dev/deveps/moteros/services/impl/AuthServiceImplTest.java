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

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(usuarioRepository, motoRepository, rutaRepository,
                amistadRepository, passwordEncoder, jwtUtil, refreshTokenService, usuarioAutenticado,
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

        LoginResponseDTO res = authService.login(new LoginRequestDTO("nuevo", "password123"));

        assertThat(res.getToken()).isEqualTo("token-jwt");
        assertThat(res.getRefreshToken()).isEqualTo("refresh-nuevo");
        assertThat(res.getExpiresIn()).isEqualTo(900L);
        assertThat(res.getUsuario().getEmail()).isEqualTo("nuevo@test.com");
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
        assertThatThrownBy(() -> authService.login(new LoginRequestDTO("nuevo", "mala")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void login_usuarioInactivo_lanzaBadRequest() {
        when(usuarioRepository.findByEmailOrNombreUsuario("nuevo", "nuevo")).thenReturn(Optional.of(usuario(false)));
        assertThatThrownBy(() -> authService.login(new LoginRequestDTO("nuevo", "password123")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void login_usuarioNoExiste_lanzaBadRequest() {
        when(usuarioRepository.findByEmailOrNombreUsuario("x", "x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.login(new LoginRequestDTO("x", "y")))
                .isInstanceOf(BadRequestException.class);
    }
}
