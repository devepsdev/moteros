package dev.deveps.moteros.repositories;

import dev.deveps.moteros.entities.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UsuarioRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Usuario marc;

    @BeforeEach
    void setUp() {
        marc = persistir("lobo_asfalto", "Marc Puig", "marc@test.com", "Barcelona", true);
        persistir("reina_curvas", "Laia Ferrer", "laia@test.com", "Girona", true);
        persistir("baja_inactivo", "Pepe Baja", "pepe@test.com", "Barcelona", false);
    }

    @Test
    void findByUuid_devuelveUsuario() {
        Optional<Usuario> encontrado = usuarioRepository.findByUuid(marc.getUuid());
        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getEmail()).isEqualTo("marc@test.com");
    }

    @Test
    void findByEmailOrNombreUsuario_funcionaConAmbosCampos() {
        assertThat(usuarioRepository.findByEmailOrNombreUsuario("marc@test.com", "marc@test.com")).isPresent();
        assertThat(usuarioRepository.findByEmailOrNombreUsuario("lobo_asfalto", "lobo_asfalto")).isPresent();
        assertThat(usuarioRepository.findByEmailOrNombreUsuario("nadie", "nadie")).isEmpty();
    }

    @Test
    void existsByEmail_yExistsByNombreUsuario() {
        assertThat(usuarioRepository.existsByEmail("marc@test.com")).isTrue();
        assertThat(usuarioRepository.existsByEmail("otro@test.com")).isFalse();
        assertThat(usuarioRepository.existsByNombreUsuario("reina_curvas")).isTrue();
        assertThat(usuarioRepository.existsByNombreUsuario("desconocido")).isFalse();
    }

    @Test
    void buscarPorTexto_soloActivosYCoincidenciaParcial() {
        Page<Usuario> porCiudad = usuarioRepository.buscarPorTexto("barcel", PageRequest.of(0, 10));
        // Marc (Barcelona, activo) sí; Pepe (Barcelona, inactivo) no
        assertThat(porCiudad.getContent()).extracting(Usuario::getEmail).containsExactly("marc@test.com");

        Page<Usuario> porNombre = usuarioRepository.buscarPorTexto("ferrer", PageRequest.of(0, 10));
        assertThat(porNombre.getContent()).extracting(Usuario::getNombreUsuario).containsExactly("reina_curvas");
    }

    @Test
    void buscarPorTexto_textoVacioDevuelveTodosLosActivos() {
        Page<Usuario> todos = usuarioRepository.buscarPorTexto("", PageRequest.of(0, 10));
        assertThat(todos.getTotalElements()).isEqualTo(2);
    }

    private Usuario persistir(String nombreUsuario, String nombreCompleto, String email, String ciudad, boolean activo) {
        Usuario u = Usuario.builder()
                .nombreUsuario(nombreUsuario)
                .nombreCompleto(nombreCompleto)
                .email(email)
                .passwordHash("hash")
                .ciudad(ciudad)
                .activo(activo)
                .build();
        return em.persistAndFlush(u);
    }
}
