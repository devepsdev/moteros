package dev.deveps.moteros.repositories;

import dev.deveps.moteros.entities.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revocado = true WHERE rt.usuario.id = :usuarioId AND rt.revocado = false")
    int revocarTodosDelUsuario(@Param("usuarioId") Integer usuarioId);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiraEn < :momento OR rt.revocado = true")
    int limpiarCaducadosYRevocados(@Param("momento") LocalDateTime momento);
}
