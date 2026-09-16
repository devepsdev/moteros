package dev.deveps.moteros.repositories;

import dev.deveps.moteros.entities.Bloqueo;
import dev.deveps.moteros.entities.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BloqueoRepository extends JpaRepository<Bloqueo, Integer> {

    Optional<Bloqueo> findByBloqueadorIdAndBloqueadoId(Integer bloqueadorId, Integer bloqueadoId);

    boolean existsByBloqueadorIdAndBloqueadoId(Integer bloqueadorId, Integer bloqueadoId);

    /** Hay un bloqueo entre los dos usuarios, lo haya puesto cualquiera de ellos. */
    @Query("""
            SELECT COUNT(b) > 0 FROM Bloqueo b
            WHERE (b.bloqueador.id = :aId AND b.bloqueado.id = :bId)
               OR (b.bloqueador.id = :bId AND b.bloqueado.id = :aId)
            """)
    boolean existeEntre(@Param("aId") Integer aId, @Param("bId") Integer bId);

    /** Como {@link #existeEntre}, con el otro usuario identificado por su uuid. */
    @Query("""
            SELECT COUNT(b) > 0 FROM Bloqueo b
            WHERE (b.bloqueador.id = :aId AND b.bloqueado.uuid = :bUuid)
               OR (b.bloqueador.uuid = :bUuid AND b.bloqueado.id = :aId)
            """)
    boolean existeEntreUuid(@Param("aId") Integer aId, @Param("bUuid") String bUuid);

    /** Usuarios bloqueados por {@code bloqueadorId}, el ultimo bloqueo primero. El Pageable ha de llegar sin sort. */
    @Query(value = """
            SELECT b.bloqueado FROM Bloqueo b
            WHERE b.bloqueador.id = :bloqueadorId
            ORDER BY b.fechaCreacion DESC, b.id DESC
            """,
            countQuery = "SELECT COUNT(b) FROM Bloqueo b WHERE b.bloqueador.id = :bloqueadorId")
    Page<Usuario> findBloqueados(@Param("bloqueadorId") Integer bloqueadorId, Pageable pageable);
}
