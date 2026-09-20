package dev.deveps.moteros.repositories;

import dev.deveps.moteros.entities.DispositivoPush;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DispositivoPushRepository extends JpaRepository<DispositivoPush, Integer> {

    Optional<DispositivoPush> findByToken(String token);

    List<DispositivoPush> findByUsuarioId(Integer usuarioId);

    void deleteByToken(String token);

    void deleteByTokenIn(List<String> tokens);
}
