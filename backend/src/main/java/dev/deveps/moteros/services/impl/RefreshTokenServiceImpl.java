package dev.deveps.moteros.services.impl;

import dev.deveps.moteros.entities.RefreshToken;
import dev.deveps.moteros.entities.Usuario;
import dev.deveps.moteros.exceptions.BadRequestException;
import dev.deveps.moteros.repositories.RefreshTokenRepository;
import dev.deveps.moteros.services.RefreshTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
@Transactional
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshExpiracionMs;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository,
                                   @Value("${app.jwt.refresh-expiration:2592000000}") long refreshExpiracionMs) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshExpiracionMs = refreshExpiracionMs;
    }

    @Override
    public RefreshToken crear(Usuario usuario) {
        RefreshToken rt = RefreshToken.builder()
                .token(generarTokenOpaco())
                .usuario(usuario)
                .expiraEn(LocalDateTime.now().plus(refreshExpiracionMs, ChronoUnit.MILLIS))
                .revocado(false)
                .build();
        return refreshTokenRepository.save(rt);
    }

    @Override
    public RefreshToken validarYRotar(String token) {
        RefreshToken actual = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Refresh token invalido"));
        if (!actual.esValido()) {
            throw new BadRequestException("Refresh token expirado o revocado. Vuelve a iniciar sesion");
        }
        actual.setRevocado(true);
        refreshTokenRepository.save(actual);
        return crear(actual.getUsuario());
    }

    @Override
    public void revocar(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevocado(true);
            refreshTokenRepository.save(rt);
        });
    }

    @Override
    public void revocarTodos(Integer usuarioId) {
        refreshTokenRepository.revocarTodosDelUsuario(usuarioId);
    }

    private String generarTokenOpaco() {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
