package dev.deveps.moteros.repositories;

import dev.deveps.moteros.entities.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Integer> {

    Optional<PasswordResetToken> findByUsuarioIdAndUsadoFalse(Integer usuarioId);

    void deleteByUsuarioId(Integer usuarioId);

    void deleteByFechaExpiracionBeforeOrUsadoTrue(LocalDateTime cutoff);

    /**
     * Gasta un intento de forma atomica si el codigo sigue vigente, sin usar y por debajo del maximo.
     * Devuelve 1 si se ha podido gastar y 0 si el codigo ya no admite mas intentos.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE PasswordResetToken t SET t.intentos = t.intentos + 1
            WHERE t.id = :id AND t.usado = false AND t.intentos < :max AND t.fechaExpiracion > :ahora
            """)
    int consumirIntento(@Param("id") Integer id, @Param("max") int max, @Param("ahora") LocalDateTime ahora);

    /** Marca el codigo como usado solo si nadie lo ha usado antes. Devuelve 1 si lo ha marcado. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE PasswordResetToken t SET t.usado = true WHERE t.id = :id AND t.usado = false")
    int marcarUsado(@Param("id") Integer id);
}
