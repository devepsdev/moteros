package dev.deveps.moteros.repositories;

import dev.deveps.moteros.entities.Ruta;
import dev.deveps.moteros.entities.enums.Dificultad;
import dev.deveps.moteros.entities.enums.TipoTerreno;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface RutaRepository extends JpaRepository<Ruta, Integer> {

    Optional<Ruta> findByUuid(String uuid);

    /** [Dificultad, Long] con el numero de rutas de cada dificultad. */
    @Query("SELECT r.dificultad, COUNT(r) FROM Ruta r GROUP BY r.dificultad")
    List<Object[]> contarPorDificultad();

    /** [TipoTerreno, Long] con el numero de rutas de cada tipo de terreno. */
    @Query("SELECT r.tipoTerreno, COUNT(r) FROM Ruta r GROUP BY r.tipoTerreno")
    List<Object[]> contarPorTerreno();

    Page<Ruta> findByCreadorUuid(String creadorUuid, Pageable pageable);

    long countByCreadorId(Integer creadorId);

    long countByCreadorUuid(String creadorUuid);

    @Query("""
            SELECT r FROM Ruta r
            WHERE :texto IS NULL OR :texto = '' OR
                  LOWER(r.nombre) LIKE LOWER(CONCAT('%', :texto, '%')) OR
                  LOWER(r.descripcion) LIKE LOWER(CONCAT('%', :texto, '%')) OR
                  LOWER(r.puntoInicio) LIKE LOWER(CONCAT('%', :texto, '%')) OR
                  LOWER(r.puntoFin) LIKE LOWER(CONCAT('%', :texto, '%'))
            """)
    Page<Ruta> buscarPorTexto(@Param("texto") String texto, Pageable pageable);

    @Query("""
            SELECT r FROM Ruta r WHERE
            (:nombre IS NULL OR LOWER(r.nombre) LIKE LOWER(CONCAT('%', :nombre, '%'))) AND
            (:dificultad IS NULL OR r.dificultad = :dificultad) AND
            (:tipoTerreno IS NULL OR r.tipoTerreno = :tipoTerreno) AND
            (:distanciaMin IS NULL OR r.distanciaKm >= :distanciaMin) AND
            (:distanciaMax IS NULL OR r.distanciaKm <= :distanciaMax) AND
            (:duracionMax IS NULL OR r.duracionEstimadaMin <= :duracionMax) AND
            (:creadorUuid IS NULL OR r.creador.uuid = :creadorUuid)
            """)
    Page<Ruta> filtrar(@Param("nombre") String nombre,
                       @Param("dificultad") Dificultad dificultad,
                       @Param("tipoTerreno") TipoTerreno tipoTerreno,
                       @Param("distanciaMin") BigDecimal distanciaMin,
                       @Param("distanciaMax") BigDecimal distanciaMax,
                       @Param("duracionMax") Integer duracionMax,
                       @Param("creadorUuid") String creadorUuid,
                       Pageable pageable);

    /**
     * Rutas cuyo punto de inicio esta a menos de {@code radioKm} del punto dado (Haversine, radio Tierra 6371 km).
     * Ordenar solo por columnas de la tabla {@code rutas} (consulta nativa).
     */
    @Query(value = """
            SELECT r.* FROM rutas r
            WHERE r.latitud_inicio IS NOT NULL AND r.longitud_inicio IS NOT NULL
              AND (6371 * acos(
                    cos(radians(:lat)) * cos(radians(r.latitud_inicio)) *
                    cos(radians(r.longitud_inicio) - radians(:lng)) +
                    sin(radians(:lat)) * sin(radians(r.latitud_inicio))
              )) <= :radioKm
            """,
            countQuery = """
            SELECT count(*) FROM rutas r
            WHERE r.latitud_inicio IS NOT NULL AND r.longitud_inicio IS NOT NULL
              AND (6371 * acos(
                    cos(radians(:lat)) * cos(radians(r.latitud_inicio)) *
                    cos(radians(r.longitud_inicio) - radians(:lng)) +
                    sin(radians(:lat)) * sin(radians(r.latitud_inicio))
              )) <= :radioKm
            """,
            nativeQuery = true)
    Page<Ruta> buscarCercanas(@Param("lat") double lat,
                              @Param("lng") double lng,
                              @Param("radioKm") double radioKm,
                              Pageable pageable);
}
