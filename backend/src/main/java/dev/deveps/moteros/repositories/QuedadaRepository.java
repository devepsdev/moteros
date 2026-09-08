package dev.deveps.moteros.repositories;

import dev.deveps.moteros.entities.Quedada;
import dev.deveps.moteros.entities.enums.EstadoQuedada;
import dev.deveps.moteros.entities.enums.NivelRecomendado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface QuedadaRepository extends JpaRepository<Quedada, Integer> {

    Optional<Quedada> findByUuid(String uuid);

    Page<Quedada> findByOrganizadorUuid(String organizadorUuid, Pageable pageable);

    @Query("""
            SELECT q FROM Quedada q
            WHERE :texto IS NULL OR :texto = '' OR
                  LOWER(q.titulo) LIKE LOWER(CONCAT('%', :texto, '%')) OR
                  LOWER(q.descripcion) LIKE LOWER(CONCAT('%', :texto, '%')) OR
                  LOWER(q.puntoEncuentro) LIKE LOWER(CONCAT('%', :texto, '%'))
            """)
    Page<Quedada> buscarPorTexto(@Param("texto") String texto, Pageable pageable);

    @Query("""
            SELECT q FROM Quedada q WHERE
            (:titulo IS NULL OR LOWER(q.titulo) LIKE LOWER(CONCAT('%', :titulo, '%'))) AND
            (:nivel IS NULL OR q.nivelRecomendado = :nivel) AND
            (:estado IS NULL OR q.estado = :estado) AND
            (:rutaUuid IS NULL OR q.ruta.uuid = :rutaUuid) AND
            (:organizadorUuid IS NULL OR q.organizador.uuid = :organizadorUuid) AND
            (:fechaDesde IS NULL OR q.fechaHora >= :fechaDesde) AND
            (:fechaHasta IS NULL OR q.fechaHora <= :fechaHasta) AND
            (:ahora IS NULL OR q.fechaHora >= :ahora)
            """)
    Page<Quedada> filtrar(@Param("titulo") String titulo,
                          @Param("nivel") NivelRecomendado nivel,
                          @Param("estado") EstadoQuedada estado,
                          @Param("rutaUuid") String rutaUuid,
                          @Param("organizadorUuid") String organizadorUuid,
                          @Param("fechaDesde") LocalDateTime fechaDesde,
                          @Param("fechaHasta") LocalDateTime fechaHasta,
                          @Param("ahora") LocalDateTime ahora,
                          Pageable pageable);

    /**
     * Quedadas cuyo punto de encuentro esta a menos de {@code radioKm} del punto dado (Haversine).
     * Ordenar solo por columnas de la tabla {@code quedadas} (consulta nativa).
     */
    @Query(value = """
            SELECT q.* FROM quedadas q
            WHERE q.latitud_encuentro IS NOT NULL AND q.longitud_encuentro IS NOT NULL
              AND (6371 * acos(
                    cos(radians(:lat)) * cos(radians(q.latitud_encuentro)) *
                    cos(radians(q.longitud_encuentro) - radians(:lng)) +
                    sin(radians(:lat)) * sin(radians(q.latitud_encuentro))
              )) <= :radioKm
            """,
            countQuery = """
            SELECT count(*) FROM quedadas q
            WHERE q.latitud_encuentro IS NOT NULL AND q.longitud_encuentro IS NOT NULL
              AND (6371 * acos(
                    cos(radians(:lat)) * cos(radians(q.latitud_encuentro)) *
                    cos(radians(q.longitud_encuentro) - radians(:lng)) +
                    sin(radians(:lat)) * sin(radians(q.latitud_encuentro))
              )) <= :radioKm
            """,
            nativeQuery = true)
    Page<Quedada> buscarCercanas(@Param("lat") double lat,
                                 @Param("lng") double lng,
                                 @Param("radioKm") double radioKm,
                                 Pageable pageable);
}
