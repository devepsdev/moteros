package dev.deveps.moteros.repositories;

import dev.deveps.moteros.entities.SugerenciaRuta;
import dev.deveps.moteros.entities.enums.EstadoSugerencia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SugerenciaRutaRepository extends JpaRepository<SugerenciaRuta, Integer> {

    Optional<SugerenciaRuta> findByUuid(String uuid);

    Page<SugerenciaRuta> findAllByOrderByFechaCreacionDesc(Pageable pageable);

    Page<SugerenciaRuta> findByEstadoOrderByFechaCreacionDesc(EstadoSugerencia estado, Pageable pageable);

    long countByEstado(EstadoSugerencia estado);

    long countByUsuarioIdAndEstado(Integer usuarioId, EstadoSugerencia estado);

    /** Si ya se sugirio una ruta con ese nombre y salida, en cualquier estado. Ignora mayusculas. */
    @Query("""
            SELECT COUNT(s) > 0 FROM SugerenciaRuta s
            WHERE LOWER(s.nombre) = LOWER(:nombre) AND LOWER(s.puntoInicio) = LOWER(:puntoInicio)
            """)
    boolean existeSugerida(@Param("nombre") String nombre, @Param("puntoInicio") String puntoInicio);
}
