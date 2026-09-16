package dev.deveps.moteros.repositories;

import dev.deveps.moteros.entities.Comentario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComentarioRepository extends JpaRepository<Comentario, Integer> {

    Optional<Comentario> findByUuid(String uuid);

    /** Comentarios de una publicacion, sin los de usuarios con los que {@code yoId} tiene un bloqueo. */
    @Query(value = """
            SELECT c FROM Comentario c
            WHERE c.publicacion.uuid = :publicacionUuid
              AND NOT EXISTS (SELECT b FROM Bloqueo b
                              WHERE (b.bloqueador.id = :yoId AND b.bloqueado = c.usuario)
                                 OR (b.bloqueado.id = :yoId AND b.bloqueador = c.usuario))
            ORDER BY c.fecha ASC
            """,
            countQuery = """
            SELECT COUNT(c) FROM Comentario c
            WHERE c.publicacion.uuid = :publicacionUuid
              AND NOT EXISTS (SELECT b FROM Bloqueo b
                              WHERE (b.bloqueador.id = :yoId AND b.bloqueado = c.usuario)
                                 OR (b.bloqueado.id = :yoId AND b.bloqueador = c.usuario))
            """)
    Page<Comentario> findVisibles(@Param("publicacionUuid") String publicacionUuid,
                                  @Param("yoId") Integer yoId, Pageable pageable);

    /** Vista previa para el feed: los ultimos comentarios visibles (se pide con un Pageable de tamano 3). */
    @Query("""
            SELECT c FROM Comentario c
            WHERE c.publicacion.id = :publicacionId
              AND NOT EXISTS (SELECT b FROM Bloqueo b
                              WHERE (b.bloqueador.id = :yoId AND b.bloqueado = c.usuario)
                                 OR (b.bloqueado.id = :yoId AND b.bloqueador = c.usuario))
            ORDER BY c.fecha DESC
            """)
    List<Comentario> findUltimosVisibles(@Param("publicacionId") Integer publicacionId,
                                         @Param("yoId") Integer yoId, Pageable pageable);

    long countByPublicacionId(Integer publicacionId);
}
