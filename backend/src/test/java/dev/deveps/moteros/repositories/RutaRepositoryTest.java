package dev.deveps.moteros.repositories;

import dev.deveps.moteros.entities.Ruta;
import dev.deveps.moteros.entities.Usuario;
import dev.deveps.moteros.entities.enums.Dificultad;
import dev.deveps.moteros.entities.enums.TipoTerreno;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@org.springframework.test.context.ActiveProfiles("test")
class RutaRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private RutaRepository rutaRepository;

    private Usuario creador;

    @BeforeEach
    void setUp() {
        creador = em.persistAndFlush(Usuario.builder()
                .nombreUsuario("creador").nombreCompleto("El Creador").email("creador@test.com")
                .passwordHash("hash").activo(true).build());

        // Ruta cerca de Madrid, facil, 40 km
        persistir("Sierra de Madrid", Dificultad.facil, TipoTerreno.asfalto,
                new BigDecimal("40.0"), 60, new BigDecimal("40.4168"), new BigDecimal("-3.7038"));
        // Ruta cerca de Barcelona, dificil, 120 km
        persistir("Pirineo", Dificultad.dificil, TipoTerreno.asfalto,
                new BigDecimal("120.0"), 180, new BigDecimal("41.3851"), new BigDecimal("2.1734"));
    }

    @Test
    void filtrar_porDificultad() {
        Page<Ruta> res = rutaRepository.filtrar(null, Dificultad.dificil, null, null, null, null, null,
                PageRequest.of(0, 10));
        assertThat(res.getContent()).extracting(Ruta::getNombre).containsExactly("Pirineo");
    }

    @Test
    void filtrar_porRangoDeDistancia() {
        Page<Ruta> res = rutaRepository.filtrar(null, null, null,
                new BigDecimal("10"), new BigDecimal("50"), null, null, PageRequest.of(0, 10));
        assertThat(res.getContent()).extracting(Ruta::getNombre).containsExactly("Sierra de Madrid");
    }

    @Test
    void filtrar_porCreador() {
        Page<Ruta> res = rutaRepository.filtrar(null, null, null, null, null, null, creador.getUuid(),
                PageRequest.of(0, 10));
        assertThat(res.getTotalElements()).isEqualTo(2);
    }

    @Test
    void buscarCercanas_haversineFiltraPorRadio() {
        // Alrededor de Madrid, 50 km: solo la ruta de la Sierra
        Page<Ruta> cerca = rutaRepository.buscarCercanas(40.4168, -3.7038, 50, PageRequest.of(0, 10));
        assertThat(cerca.getContent()).extracting(Ruta::getNombre).containsExactly("Sierra de Madrid");

        // Radio enorme: las dos
        Page<Ruta> lejos = rutaRepository.buscarCercanas(40.4168, -3.7038, 1000, PageRequest.of(0, 10));
        assertThat(lejos.getTotalElements()).isEqualTo(2);
    }

    @Test
    void countByCreador() {
        assertThat(rutaRepository.countByCreadorId(creador.getId())).isEqualTo(2);
        assertThat(rutaRepository.countByCreadorUuid(creador.getUuid())).isEqualTo(2);
    }

    private void persistir(String nombre, Dificultad dif, TipoTerreno terreno, BigDecimal distanciaKm,
                           Integer duracion, BigDecimal lat, BigDecimal lng) {
        em.persistAndFlush(Ruta.builder()
                .creador(creador)
                .nombre(nombre)
                .puntoInicio("Inicio")
                .puntoFin("Fin")
                .latitudInicio(lat)
                .longitudInicio(lng)
                .distanciaKm(distanciaKm)
                .duracionEstimadaMin(duracion)
                .dificultad(dif)
                .tipoTerreno(terreno)
                .build());
    }
}
