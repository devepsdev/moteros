package dev.deveps.moteros.repositories;

import dev.deveps.moteros.entities.PuntoRuta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PuntoRutaRepository extends JpaRepository<PuntoRuta, Integer> {

    Optional<PuntoRuta> findByUuid(String uuid);

    List<PuntoRuta> findByRutaIdOrderByOrdenAsc(Integer rutaId);

    List<PuntoRuta> findByRutaUuidOrderByOrdenAsc(String rutaUuid);

    boolean existsByRutaIdAndOrden(Integer rutaId, Integer orden);

    /** Borra el track completo de una ruta (para reemplazarlo al editar). */
    @Modifying
    void deleteByRutaId(Integer rutaId);
}
