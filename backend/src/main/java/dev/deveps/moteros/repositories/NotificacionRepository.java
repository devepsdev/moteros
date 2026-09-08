package dev.deveps.moteros.repositories;

import dev.deveps.moteros.entities.Notificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Integer> {

    Optional<Notificacion> findByUuid(String uuid);

    Page<Notificacion> findByUsuarioUuidOrderByFechaCreacionDesc(String usuarioUuid, Pageable pageable);

    Page<Notificacion> findByUsuarioUuidAndLeidoFalseOrderByFechaCreacionDesc(String usuarioUuid, Pageable pageable);

    long countByUsuarioIdAndLeidoFalse(Integer usuarioId);

    long countByUsuarioUuidAndLeidoFalse(String usuarioUuid);

    @Modifying
    @Query("UPDATE Notificacion n SET n.leido = true WHERE n.usuario.id = :usuarioId AND n.leido = false")
    int marcarTodasLeidas(@Param("usuarioId") Integer usuarioId);
}
