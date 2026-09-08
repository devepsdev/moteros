package dev.deveps.moteros.repositories;

import dev.deveps.moteros.entities.Usuario;
import dev.deveps.moteros.entities.enums.RolUsuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
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

    long countByRol(RolUsuario rol);

    long countByActivoTrue();

    /** Fechas de alta de todos los usuarios; el agrupado por mes se hace en el servicio (portable MySQL/H2). */
    @Query("SELECT u.fechaRegistro FROM Usuario u WHERE u.fechaRegistro IS NOT NULL")
    List<LocalDateTime> fechasRegistro();

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

    /** Como {@link #buscarPorTexto} pero sin filtrar por activo ni ciudad, e incluye email (uso admin). */
    @Query("""
            SELECT u FROM Usuario u
            WHERE :texto IS NULL OR :texto = '' OR
                  LOWER(u.nombreUsuario) LIKE LOWER(CONCAT('%', :texto, '%')) OR
                  LOWER(u.nombreCompleto) LIKE LOWER(CONCAT('%', :texto, '%')) OR
                  LOWER(u.email) LIKE LOWER(CONCAT('%', :texto, '%'))
            """)
    Page<Usuario> buscarTodosPorTexto(@Param("texto") String texto, Pageable pageable);
}
