package dev.deveps.moteros.repositories;

import dev.deveps.moteros.entities.Publicacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PublicacionRepository extends JpaRepository<Publicacion, Integer> {

    Optional<Publicacion> findByUuid(String uuid);

    Page<Publicacion> findByUsuarioUuidOrderByFechaPublicacionDesc(String usuarioUuid, Pageable pageable);

    long countByUsuarioId(Integer usuarioId);

    long countByUsuarioUuid(String usuarioUuid);

    @Query("""
            SELECT p FROM Publicacion p
            WHERE :texto IS NULL OR :texto = '' OR
                  LOWER(p.contenido) LIKE LOWER(CONCAT('%', :texto, '%'))
            """)
    Page<Publicacion> buscarPorTexto(@Param("texto") String texto, Pageable pageable);

    /**
     * Feed del usuario {@code usuarioId}: sus publicaciones y las de sus amigos aceptados.
     */
    @Query("""
            SELECT p FROM Publicacion p
            WHERE p.usuario.id = :usuarioId
               OR p.usuario.id IN (
                    SELECT CASE WHEN a.usuario.id = :usuarioId THEN a.amigo.id ELSE a.usuario.id END
                    FROM Amistad a
                    WHERE a.estado = dev.deveps.moteros.entities.enums.EstadoAmistad.aceptada
                      AND (a.usuario.id = :usuarioId OR a.amigo.id = :usuarioId)
               )
            """)
    Page<Publicacion> feed(@Param("usuarioId") Integer usuarioId, Pageable pageable);
}
