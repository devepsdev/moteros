package dev.deveps.moteros.security;

import dev.deveps.moteros.entities.PasswordResetToken;
import dev.deveps.moteros.exceptions.BadRequestException;
import dev.deveps.moteros.repositories.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Genera y valida los codigos de 6 digitos para recuperar la contrasena.
 *
 * Un codigo de 6 digitos tiene mucha menos entropia que un refresh token
 * (1 millon de combinaciones), asi que aqui la seguridad no viene del propio
 * codigo sino de limitarlo por todos los lados: caduca pronto (15 min por
 * defecto), solo hay un codigo activo por usuario a la vez (pedir uno nuevo
 * invalida el anterior) y se bloquea tras unos pocos intentos fallidos.
 */
@Component
@RequiredArgsConstructor
public class PasswordResetService {

    private static final int MAX_ATTEMPTS = 5;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Value("${security.password-reset.expiration-minutes:15}")
    private long expirationMinutes;

    @Transactional
    public String createResetCode(Integer usuarioId) {
        // Solo puede haber un codigo activo por usuario: pedir uno nuevo invalida el anterior.
        passwordResetTokenRepository.deleteByUsuarioId(usuarioId);

        String rawCode = generateCode();
        PasswordResetToken entity = PasswordResetToken.builder()
                .usuarioId(usuarioId)
                .codeHash(HashUtils.sha256(rawCode))
                .fechaExpiracion(LocalDateTime.now().plusMinutes(expirationMinutes))
                .usado(false)
                .intentos(0)
                .build();
        passwordResetTokenRepository.save(entity);
        return rawCode;
    }

    /**
     * Valida el codigo para el usuario indicado y lo marca como usado.
     * Lanza el mismo mensaje generico tanto si el codigo no existe, ha caducado,
     * se han agotado los intentos o no coincide, para no dar pistas del motivo.
     */
    @Transactional(noRollbackFor = BadRequestException.class)
    public void verifyCode(Integer usuarioId, String rawCode) {
        PasswordResetToken token = passwordResetTokenRepository.findByUsuarioIdAndUsadoFalse(usuarioId)
                .orElseThrow(this::invalidCode);

        if (token.getFechaExpiracion().isBefore(LocalDateTime.now()) || token.getIntentos() >= MAX_ATTEMPTS) {
            passwordResetTokenRepository.delete(token);
            throw invalidCode();
        }

        if (!HashUtils.sha256(rawCode).equals(token.getCodeHash())) {
            token.setIntentos(token.getIntentos() + 1);
            passwordResetTokenRepository.save(token);
            throw invalidCode();
        }

        token.setUsado(true);
        passwordResetTokenRepository.save(token);
    }

    @Scheduled(fixedRate = 24 * 60 * 60 * 1000)
    @Transactional
    public void cleanupExpiredOrUsed() {
        passwordResetTokenRepository.deleteByFechaExpiracionBeforeOrUsadoTrue(LocalDateTime.now());
    }

    private String generateCode() {
        int code = SECURE_RANDOM.nextInt(1_000_000);
        return String.format("%06d", code);
    }

    private BadRequestException invalidCode() {
        return new BadRequestException("El codigo no es valido o ha caducado");
    }
}
