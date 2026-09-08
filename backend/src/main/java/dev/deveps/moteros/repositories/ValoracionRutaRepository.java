package dev.deveps.moteros.repositories;

import dev.deveps.moteros.entities.ValoracionRuta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ValoracionRutaRepository extends JpaRepository<ValoracionRuta, Integer> {

    Optional<ValoracionRuta> findByUuid(String uuid);

    Optional<ValoracionRuta> findByRutaIdAndUsuarioId(Integer rutaId, Integer usuarioId);

    Optional<ValoracionRuta> findByRutaUuidAndUsuarioUuid(String rutaUuid, String usuarioUuid);

    boolean existsByRutaIdAndUsuarioId(Integer rutaId, Integer usuarioId);

    Page<ValoracionRuta> findByRutaUuidOrderByFechaDesc(String rutaUuid, Pageable pageable);

    long countByRutaId(Integer rutaId);

    @Query("SELECT AVG(v.puntuacion) FROM ValoracionRuta v WHERE v.ruta.id = :rutaId")
    Double mediaPuntuacion(@Param("rutaId") Integer rutaId);
}
