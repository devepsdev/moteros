package dev.deveps.moteros.repositories;

import dev.deveps.moteros.entities.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Integer> {

    Optional<PasswordResetToken> findByUsuarioIdAndUsadoFalse(Integer usuarioId);

    void deleteByUsuarioId(Integer usuarioId);

    void deleteByFechaExpiracionBeforeOrUsadoTrue(LocalDateTime cutoff);
}
