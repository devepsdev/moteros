package dev.deveps.moteros.repositories;

import dev.deveps.moteros.entities.Mensaje;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MensajeRepository extends JpaRepository<Mensaje, Integer> {

    Optional<Mensaje> findByUuid(String uuid);

    Page<Mensaje> findByConversacionUuidOrderByFechaEnvioDesc(String conversacionUuid, Pageable pageable);

    Optional<Mensaje> findTop1ByConversacionIdOrderByFechaEnvioDesc(Integer conversacionId);

    /** Mensajes no leidos recibidos por el usuario (los que no ha enviado el) en una conversacion. */
    long countByConversacionIdAndLeidoFalseAndRemitenteIdNot(Integer conversacionId, Integer usuarioId);

    /** Total de mensajes no leidos del usuario en todas sus conversaciones. */
    @Query("""
            SELECT COUNT(m) FROM Mensaje m
            WHERE m.leido = false AND m.remitente.id <> :usuarioId
              AND (m.conversacion.usuario1.id = :usuarioId OR m.conversacion.usuario2.id = :usuarioId)
            """)
    long countNoLeidosTotal(@Param("usuarioId") Integer usuarioId);

    @Modifying
    @Query("""
            UPDATE Mensaje m SET m.leido = true
            WHERE m.conversacion.id = :conversacionId
              AND m.remitente.id <> :usuarioId
              AND m.leido = false
            """)
    int marcarLeidos(@Param("conversacionId") Integer conversacionId, @Param("usuarioId") Integer usuarioId);
}
