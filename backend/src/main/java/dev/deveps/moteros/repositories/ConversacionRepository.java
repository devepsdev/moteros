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

    @Query("""
            SELECT c FROM Conversacion c
            WHERE c.usuario1.id = :usuarioId OR c.usuario2.id = :usuarioId
            """)
    Page<Conversacion> findByParticipante(@Param("usuarioId") Integer usuarioId, Pageable pageable);
}
