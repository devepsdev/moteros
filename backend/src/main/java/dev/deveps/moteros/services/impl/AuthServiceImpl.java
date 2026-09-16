package dev.deveps.moteros.services.impl;

import dev.deveps.moteros.dto.CambioPasswordDTO;
import dev.deveps.moteros.dto.LoginRequestDTO;
import dev.deveps.moteros.dto.LoginResponseDTO;
import dev.deveps.moteros.dto.RecuperarPasswordDTO;
import dev.deveps.moteros.dto.RefreshTokenRequestDTO;
import dev.deveps.moteros.dto.RegistroUsuarioDTO;
import dev.deveps.moteros.dto.RestablecerPasswordDTO;
import dev.deveps.moteros.entities.RefreshToken;
import dev.deveps.moteros.entities.Usuario;
import dev.deveps.moteros.entities.enums.RolUsuario;
import dev.deveps.moteros.exceptions.BadRequestException;
import dev.deveps.moteros.exceptions.DuplicateResourceException;
import dev.deveps.moteros.mapper.EntityDtoMapper;
import dev.deveps.moteros.repositories.AmistadRepository;
import dev.deveps.moteros.repositories.MotoRepository;
import dev.deveps.moteros.repositories.RutaRepository;
import dev.deveps.moteros.repositories.UsuarioRepository;
import dev.deveps.moteros.security.JwtUtil;
import dev.deveps.moteros.security.LoginRateLimiter;
import dev.deveps.moteros.security.PasswordResetService;
import dev.deveps.moteros.security.UsuarioAutenticadoProvider;
import dev.deveps.moteros.services.AuthService;
import dev.deveps.moteros.services.EmailService;
import dev.deveps.moteros.services.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final MotoRepository motoRepository;
    private final RutaRepository rutaRepository;
    private final AmistadRepository amistadRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final UsuarioAutenticadoProvider usuarioAutenticado;
    private final LoginRateLimiter loginRateLimiter;
    private final PasswordResetService passwordResetService;
    private final EmailService emailService;
    private final EntityDtoMapper mapper;

    @Override
    public LoginResponseDTO registro(RegistroUsuarioDTO dto) {
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("Ya existe un usuario con el email: " + dto.getEmail());
        }
        if (usuarioRepository.existsByNombreUsuario(dto.getNombreUsuario())) {
            throw new DuplicateResourceException("El nombre de usuario ya esta en uso: " + dto.getNombreUsuario());
        }

        // El primer usuario registrado en la plataforma queda como admin; el resto, como user.
        RolUsuario rol = usuarioRepository.countByRol(RolUsuario.admin) == 0
                ? RolUsuario.admin
                : RolUsuario.user;

        Usuario usuario = Usuario.builder()
                .nombreUsuario(dto.getNombreUsuario())
                .nombreCompleto(dto.getNombreCompleto())
                .email(dto.getEmail())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .ciudad(dto.getCiudad())
                .fechaAceptacionTerminos(java.time.LocalDateTime.now())
                .activo(true)
                .rol(rol)
                .build();

        Usuario guardado = usuarioRepository.save(usuario);
        return construirRespuesta(guardado);
    }

    @Override
    public LoginResponseDTO login(LoginRequestDTO dto, String clientIp) {
        loginRateLimiter.checkAllowed(dto.getIdentificador(), clientIp);
        Usuario usuario;
        try {
            usuario = usuarioRepository
                    .findByEmailOrNombreUsuario(dto.getIdentificador(), dto.getIdentificador())
                    .orElseThrow(() -> new BadRequestException("Credenciales invalidas"));

            if (Boolean.FALSE.equals(usuario.getActivo())) {
                throw new BadRequestException("La cuenta esta desactivada");
            }
            if (!passwordEncoder.matches(dto.getPassword(), usuario.getPasswordHash())) {
                throw new BadRequestException("Credenciales invalidas");
            }
        } catch (BadRequestException ex) {
            loginRateLimiter.recordFailure(dto.getIdentificador(), clientIp);
            throw ex;
        }
        loginRateLimiter.recordSuccess(dto.getIdentificador(), clientIp);

        return construirRespuesta(usuario);
    }

    @Override
    public LoginResponseDTO refrescar(RefreshTokenRequestDTO dto) {
        RefreshToken nuevo = refreshTokenService.validarYRotar(dto.getRefreshToken());
        Usuario usuario = nuevo.getUsuario();
        if (Boolean.FALSE.equals(usuario.getActivo())) {
            throw new BadRequestException("La cuenta esta desactivada");
        }
        return construirRespuesta(usuario, nuevo.getToken());
    }

    @Override
    public void logout(RefreshTokenRequestDTO dto) {
        refreshTokenService.revocar(dto.getRefreshToken());
    }

    @Override
    public void logoutTodos() {
        refreshTokenService.revocarTodos(usuarioAutenticado.obtenerIdUsuarioActual());
    }

    @Override
    public void cambiarPassword(CambioPasswordDTO dto) {
        Usuario usuario = usuarioAutenticado.obtenerUsuarioActual();
        if (!passwordEncoder.matches(dto.getPasswordActual(), usuario.getPasswordHash())) {
            throw new BadRequestException("La contrasena actual es incorrecta");
        }
        usuario.setPasswordHash(passwordEncoder.encode(dto.getPasswordNueva()));
        usuarioRepository.save(usuario);
        // Al cambiar la contrasena se cierran todas las sesiones.
        refreshTokenService.revocarTodos(usuario.getId());
    }

    @Override
    public void recuperarPassword(RecuperarPasswordDTO dto, String clientIp) {
        // Mismo limitador que el login con prefijo propio: no comparte contador con los
        // intentos de login reales, pero si el "maximo N por email/IP en una ventana".
        String email = dto.getEmail();
        loginRateLimiter.checkAllowed("reset:" + email, "reset:" + clientIp);
        loginRateLimiter.recordFailure("reset:" + email, "reset:" + clientIp);

        // Siempre se responde igual, exista o no el email: si no, el endpoint serviria
        // para averiguar que emails estan registrados.
        usuarioRepository.findByEmail(email)
                .filter(u -> !Boolean.FALSE.equals(u.getActivo()))
                .ifPresent(usuario -> {
                    String codigo = passwordResetService.createResetCode(usuario.getId());
                    try {
                        emailService.enviarCodigoRecuperacion(usuario.getEmail(), usuario.getNombreCompleto(), codigo);
                    } catch (Exception ex) {
                        // Se identifica por uuid: los registros no guardan el email.
                        log.error("No se ha podido enviar el codigo de recuperacion al usuario {}: {}",
                                usuario.getUuid(), ex.getMessage());
                    }
                });
    }

    @Override
    @Transactional(noRollbackFor = BadRequestException.class)
    public void restablecerPassword(RestablecerPasswordDTO dto, String clientIp) {
        // Limite propio por email e IP: pedir un codigo nuevo reinicia su contador de 5 intentos,
        // asi que sin esto se podrian encadenar codigos para seguir probando.
        String claveEmail = "reset-verify:" + dto.getEmail();
        String claveIp = "reset-verify:" + clientIp;
        loginRateLimiter.checkAllowed(claveEmail, claveIp);

        Usuario usuario;
        try {
            usuario = usuarioRepository.findByEmail(dto.getEmail())
                    .orElseThrow(() -> new BadRequestException("El codigo no es valido o ha caducado"));
            // Si el codigo falla, el contador de intentos debe quedar guardado (noRollbackFor).
            passwordResetService.verifyCode(usuario.getId(), dto.getCodigo());
        } catch (BadRequestException ex) {
            loginRateLimiter.recordFailure(claveEmail, claveIp);
            throw ex;
        }
        loginRateLimiter.recordSuccess(claveEmail, claveIp);

        usuario.setPasswordHash(passwordEncoder.encode(dto.getPasswordNueva()));
        usuarioRepository.save(usuario);
        // Tras restablecer se cierran todas las sesiones abiertas.
        refreshTokenService.revocarTodos(usuario.getId());
    }

    // ===================== PRIVADOS =====================

    private LoginResponseDTO construirRespuesta(Usuario usuario) {
        return construirRespuesta(usuario, refreshTokenService.crear(usuario).getToken());
    }

    private LoginResponseDTO construirRespuesta(Usuario usuario, String refreshToken) {
        String token = jwtUtil.generateToken(usuario.getEmail(),
                usuario.getRol() != null ? usuario.getRol().name() : RolUsuario.user.name());
        long numMotos = motoRepository.countByUsuarioId(usuario.getId());
        long numRutas = rutaRepository.countByCreadorId(usuario.getId());
        long numAmigos = amistadRepository.countAmigosAceptados(usuario.getId());
        return LoginResponseDTO.builder()
                .token(token)
                .type("Bearer")
                .expiresIn(jwtUtil.getExpirationSeconds())
                .refreshToken(refreshToken)
                .usuario(mapper.usuarioResponse(usuario, numMotos, numRutas, numAmigos))
                .build();
    }
}
