package dev.deveps.moteros.repositories;

import dev.deveps.moteros.entities.Moto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MotoRepository extends JpaRepository<Moto, Integer> {

    Optional<Moto> findByUuid(String uuid);

    List<Moto> findByUsuarioUuidOrderByIdDesc(String usuarioUuid);

    long countByUsuarioId(Integer usuarioId);

    long countByUsuarioUuid(String usuarioUuid);

    /** URLs de las fotos de las motos de un usuario (para borrarlas del disco al eliminar la cuenta). */
    @Query("SELECT m.fotoUrl FROM Moto m WHERE m.usuario.id = :usuarioId AND m.fotoUrl IS NOT NULL")
    List<String> fotosDeUsuario(@org.springframework.data.repository.query.Param("usuarioId") Integer usuarioId);

    /** [TipoMoto, Long] con el numero de motos de cada tipo. */
    @Query("SELECT m.tipo, COUNT(m) FROM Moto m GROUP BY m.tipo")
    List<Object[]> contarPorTipo();
}
