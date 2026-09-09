package dev.deveps.moteros.services.impl;

import dev.deveps.moteros.entities.RefreshToken;
import dev.deveps.moteros.entities.Usuario;
import dev.deveps.moteros.exceptions.BadRequestException;
import dev.deveps.moteros.repositories.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class RefreshTokenServiceImplTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenServiceImpl servicio;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        servicio = new RefreshTokenServiceImpl(refreshTokenRepository, 60_000L);
        usuario = em.persistAndFlush(Usuario.builder()
                .nombreUsuario("u").nombreCompleto("U").email("u@test.com")
                .passwordHash("h").activo(true).build());
    }

    @Test
    void crear_persisteTokenValido() {
        RefreshToken rt = servicio.crear(usuario);
        assertThat(rt.getId()).isNotNull();
        assertThat(rt.getToken()).isNotBlank();
        assertThat(rt.esValido()).isTrue();
        assertThat(refreshTokenRepository.findByToken(rt.getToken())).isPresent();
    }

    @Test
    void validarYRotar_revocaElViejoYDevuelveUnoNuevo() {
        RefreshToken viejo = servicio.crear(usuario);

        RefreshToken nuevo = servicio.validarYRotar(viejo.getToken());

        assertThat(nuevo.getToken()).isNotEqualTo(viejo.getToken());
        assertThat(refreshTokenRepository.findByToken(viejo.getToken()).orElseThrow().getRevocado()).isTrue();
        assertThat(nuevo.esValido()).isTrue();
    }

    @Test
    void validarYRotar_tokenRevocado_lanzaBadRequest() {
        RefreshToken rt = servicio.crear(usuario);
        servicio.revocar(rt.getToken());

        assertThatThrownBy(() -> servicio.validarYRotar(rt.getToken()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void validarYRotar_tokenCaducado_lanzaBadRequest() {
        RefreshToken caducado = em.persistAndFlush(RefreshToken.builder()
                .token("caducado-xyz").usuario(usuario)
                .expiraEn(LocalDateTime.now().minusDays(1)).revocado(false).build());

        assertThatThrownBy(() -> servicio.validarYRotar(caducado.getToken()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void validarYRotar_tokenInexistente_lanzaBadRequest() {
        assertThatThrownBy(() -> servicio.validarYRotar("no-existe"))
                .isInstanceOf(BadRequestException.class);
    }
}
