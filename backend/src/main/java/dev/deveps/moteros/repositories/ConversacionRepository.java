package dev.deveps.moteros.repositories;

import dev.deveps.moteros.entities.Conversacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversacionRepository extends JpaRepository<Conversacion, Integer> {

    Optional<Conversacion> findByUuid(String uuid);

    /** El par se guarda siempre con usuario1Id &lt; usuario2Id (restriccion de la BBDD). */
    Optional<Conversacion> findByUsuario1IdAndUsuario2Id(Integer usuario1Id, Integer usuario2Id);

    /**
     * Conversaciones de un usuario, la de actividad mas reciente primero (ultimo mensaje o,
     * si no tiene, fecha de creacion). Se ocultan las conversaciones entre usuarios con un bloqueo.
     * El orden va en la query: el Pageable ha de llegar sin sort.
     */
    @Query(value = """
            SELECT c FROM Conversacion c
            WHERE (c.usuario1.id = :usuarioId OR c.usuario2.id = :usuarioId)
              AND NOT EXISTS (SELECT b FROM Bloqueo b
                              WHERE (b.bloqueador = c.usuario1 AND b.bloqueado = c.usuario2)
                                 OR (b.bloqueador = c.usuario2 AND b.bloqueado = c.usuario1))
            ORDER BY COALESCE((SELECT MAX(m.fechaEnvio) FROM Mensaje m WHERE m.conversacion = c),
                              c.fechaCreacion) DESC
            """,
            countQuery = """
            SELECT COUNT(c) FROM Conversacion c
            WHERE (c.usuario1.id = :usuarioId OR c.usuario2.id = :usuarioId)
              AND NOT EXISTS (SELECT b FROM Bloqueo b
                              WHERE (b.bloqueador = c.usuario1 AND b.bloqueado = c.usuario2)
                                 OR (b.bloqueador = c.usuario2 AND b.bloqueado = c.usuario1))
            """)
    Page<Conversacion> findByParticipante(@Param("usuarioId") Integer usuarioId, Pageable pageable);
}
