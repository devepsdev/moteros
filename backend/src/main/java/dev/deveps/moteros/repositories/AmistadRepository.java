package dev.deveps.moteros.repositories;

import dev.deveps.moteros.entities.Amistad;
import dev.deveps.moteros.entities.Usuario;
import dev.deveps.moteros.entities.enums.EstadoAmistad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AmistadRepository extends JpaRepository<Amistad, Integer> {

    Optional<Amistad> findByUuid(String uuid);

    Optional<Amistad> findByUsuarioIdAndAmigoId(Integer usuarioId, Integer amigoId);

    long countByEstado(EstadoAmistad estado);

    /** Relacion entre dos usuarios en cualquier direccion. */
    @Query("""
            SELECT a FROM Amistad a
            WHERE (a.usuario.id = :aId AND a.amigo.id = :bId)
               OR (a.usuario.id = :bId AND a.amigo.id = :aId)
            """)
    Optional<Amistad> findRelacion(@Param("aId") Integer aId, @Param("bId") Integer bId);

    /** Solicitudes recibidas por un usuario en un estado dado (p.ej. pendiente). */
    Page<Amistad> findByAmigoUuidAndEstado(String amigoUuid, EstadoAmistad estado, Pageable pageable);

    /** Solicitudes enviadas por un usuario en un estado dado. */
    Page<Amistad> findByUsuarioUuidAndEstado(String usuarioUuid, EstadoAmistad estado, Pageable pageable);

    /** Amigos aceptados de un usuario (en cualquiera de los dos lados de la relacion). */
    @Query("""
            SELECT u FROM Usuario u WHERE
            u.id IN (SELECT a.amigo.id FROM Amistad a
                     WHERE a.usuario.id = :usuarioId
                       AND a.estado = dev.deveps.moteros.entities.enums.EstadoAmistad.aceptada)
            OR u.id IN (SELECT a.usuario.id FROM Amistad a
                        WHERE a.amigo.id = :usuarioId
                          AND a.estado = dev.deveps.moteros.entities.enums.EstadoAmistad.aceptada)
            """)
    Page<Usuario> findAmigosAceptados(@Param("usuarioId") Integer usuarioId, Pageable pageable);

    /** Misma consulta sin paginar, para fan-out de notificaciones. */
    @Query("""
            SELECT u FROM Usuario u WHERE
            u.id IN (SELECT a.amigo.id FROM Amistad a
                     WHERE a.usuario.id = :usuarioId
                       AND a.estado = dev.deveps.moteros.entities.enums.EstadoAmistad.aceptada)
            OR u.id IN (SELECT a.usuario.id FROM Amistad a
                        WHERE a.amigo.id = :usuarioId
                          AND a.estado = dev.deveps.moteros.entities.enums.EstadoAmistad.aceptada)
            """)
    java.util.List<Usuario> findAmigosAceptadosLista(@Param("usuarioId") Integer usuarioId);

    @Query("""
            SELECT COUNT(a) FROM Amistad a
            WHERE a.estado = dev.deveps.moteros.entities.enums.EstadoAmistad.aceptada
              AND (a.usuario.id = :usuarioId OR a.amigo.id = :usuarioId)
            """)
    long countAmigosAceptados(@Param("usuarioId") Integer usuarioId);
}
