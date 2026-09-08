package dev.deveps.moteros.repositories;

import dev.deveps.moteros.entities.Moto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MotoRepository extends JpaRepository<Moto, Integer> {

    Optional<Moto> findByUuid(String uuid);

    List<Moto> findByUsuarioUuidOrderByIdDesc(String usuarioUuid);

    long countByUsuarioId(Integer usuarioId);

    long countByUsuarioUuid(String usuarioUuid);
}
