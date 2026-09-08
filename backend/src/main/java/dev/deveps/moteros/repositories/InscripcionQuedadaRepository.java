package dev.deveps.moteros.repositories;

import dev.deveps.moteros.entities.InscripcionQuedada;
import dev.deveps.moteros.entities.enums.EstadoInscripcion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InscripcionQuedadaRepository extends JpaRepository<InscripcionQuedada, Integer> {

    Optional<InscripcionQuedada> findByUuid(String uuid);

    Optional<InscripcionQuedada> findByQuedadaIdAndUsuarioId(Integer quedadaId, Integer usuarioId);

    Optional<InscripcionQuedada> findByQuedadaUuidAndUsuarioUuid(String quedadaUuid, String usuarioUuid);

    boolean existsByQuedadaIdAndUsuarioId(Integer quedadaId, Integer usuarioId);

    List<InscripcionQuedada> findByQuedadaId(Integer quedadaId);

    List<InscripcionQuedada> findByQuedadaUuid(String quedadaUuid);

    long countByQuedadaId(Integer quedadaId);

    long countByQuedadaIdAndEstado(Integer quedadaId, EstadoInscripcion estado);

    /** Quedadas a las que se ha apuntado un usuario. */
    Page<InscripcionQuedada> findByUsuarioUuid(String usuarioUuid, Pageable pageable);
}
