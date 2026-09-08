package dev.deveps.moteros.repositories;

import dev.deveps.moteros.entities.Comentario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComentarioRepository extends JpaRepository<Comentario, Integer> {

    Optional<Comentario> findByUuid(String uuid);

    Page<Comentario> findByPublicacionUuidOrderByFechaAsc(String publicacionUuid, Pageable pageable);

    /** Vista previa: ultimos comentarios de una publicacion para el feed. */
    List<Comentario> findTop3ByPublicacionIdOrderByFechaDesc(Integer publicacionId);

    long countByPublicacionId(Integer publicacionId);
}
