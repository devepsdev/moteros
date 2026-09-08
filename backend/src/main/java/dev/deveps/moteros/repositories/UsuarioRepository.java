package dev.deveps.moteros.repositories;

import dev.deveps.moteros.entities.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByUuid(String uuid);

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByNombreUsuario(String nombreUsuario);

    /** Login por email o por nombre de usuario (mismo valor en ambos parametros). */
    Optional<Usuario> findByEmailOrNombreUsuario(String email, String nombreUsuario);

    boolean existsByEmail(String email);

    boolean existsByNombreUsuario(String nombreUsuario);

    @Query("""
            SELECT u FROM Usuario u
            WHERE u.activo = true AND (
                :texto IS NULL OR :texto = '' OR
                LOWER(u.nombreUsuario) LIKE LOWER(CONCAT('%', :texto, '%')) OR
                LOWER(u.nombreCompleto) LIKE LOWER(CONCAT('%', :texto, '%')) OR
                LOWER(u.ciudad) LIKE LOWER(CONCAT('%', :texto, '%'))
            )
            """)
    Page<Usuario> buscarPorTexto(@Param("texto") String texto, Pageable pageable);
}
