package dev.deveps.moteros.repositories;

import dev.deveps.moteros.entities.Denuncia;
import dev.deveps.moteros.entities.enums.EstadoDenuncia;
import dev.deveps.moteros.entities.enums.TipoDenuncia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DenunciaRepository extends JpaRepository<Denuncia, Integer> {

    Optional<Denuncia> findByUuid(String uuid);

    Page<Denuncia> findAllByOrderByFechaCreacionDesc(Pageable pageable);

    /** Las pendientes se revisan por orden de llegada. */
    Page<Denuncia> findByEstadoOrderByFechaCreacionAsc(EstadoDenuncia estado, Pageable pageable);

    /** Las cerradas, la ultima resuelta primero. */
    Page<Denuncia> findByEstadoOrderByFechaResolucionDesc(EstadoDenuncia estado, Pageable pageable);

    long countByEstado(EstadoDenuncia estado);

    boolean existsByDenuncianteIdAndTipoAndReferenciaUuidAndEstado(
            Integer denuncianteId, TipoDenuncia tipo, String referenciaUuid, EstadoDenuncia estado);

    boolean existsByTipoAndReferenciaUuidAndEstado(TipoDenuncia tipo, String referenciaUuid, EstadoDenuncia estado);

    List<Denuncia> findByTipoAndReferenciaUuidAndEstado(TipoDenuncia tipo, String referenciaUuid, EstadoDenuncia estado);

    long countByDenuncianteIdAndFechaCreacionAfter(Integer denuncianteId, LocalDateTime desde);

    /** Cuantas denuncias ha recibido un usuario, para ver si es reincidente. */
    long countByDenunciadoId(Integer denunciadoId);

    @Modifying
    @Query("""
            DELETE FROM Denuncia d
            WHERE d.estado <> dev.deveps.moteros.entities.enums.EstadoDenuncia.pendiente
              AND d.fechaResolucion < :limite
            """)
    int borrarCerradasAntesDe(@Param("limite") LocalDateTime limite);
}
