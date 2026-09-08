package dev.deveps.moteros.repositories;

import dev.deveps.moteros.entities.LikePublicacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LikePublicacionRepository extends JpaRepository<LikePublicacion, Integer> {

    Optional<LikePublicacion> findByUuid(String uuid);

    Optional<LikePublicacion> findByPublicacionIdAndUsuarioId(Integer publicacionId, Integer usuarioId);

    boolean existsByPublicacionIdAndUsuarioId(Integer publicacionId, Integer usuarioId);

    boolean existsByPublicacionUuidAndUsuarioUuid(String publicacionUuid, String usuarioUuid);

    long countByPublicacionId(Integer publicacionId);

    Page<LikePublicacion> findByPublicacionUuidOrderByFechaDesc(String publicacionUuid, Pageable pageable);

    void deleteByPublicacionIdAndUsuarioId(Integer publicacionId, Integer usuarioId);
}
