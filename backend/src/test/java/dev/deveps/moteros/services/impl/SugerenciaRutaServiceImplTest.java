package dev.deveps.moteros.services.impl;

import dev.deveps.moteros.dto.PuntoSugeridoDTO;
import dev.deveps.moteros.dto.SugerenciaRutaRequestDTO;
import dev.deveps.moteros.dto.SugerenciaRutaResponseDTO;
import dev.deveps.moteros.entities.Ruta;
import dev.deveps.moteros.entities.SugerenciaRuta;
import dev.deveps.moteros.entities.Usuario;
import dev.deveps.moteros.entities.enums.EstadoSugerencia;
import dev.deveps.moteros.entities.enums.RolUsuario;
import dev.deveps.moteros.exceptions.BadRequestException;
import dev.deveps.moteros.exceptions.DuplicateResourceException;
import dev.deveps.moteros.mapper.EntityDtoMapper;
import dev.deveps.moteros.repositories.RutaRepository;
import dev.deveps.moteros.repositories.SugerenciaRutaRepository;
import dev.deveps.moteros.security.UsuarioAutenticadoProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SugerenciaRutaServiceImplTest {

    @Mock private SugerenciaRutaRepository sugerenciaRepository;
    @Mock private RutaRepository rutaRepository;
    @Mock private UsuarioAutenticadoProvider usuarioAutenticado;

    private SugerenciaRutaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SugerenciaRutaServiceImpl(sugerenciaRepository, rutaRepository, usuarioAutenticado, new EntityDtoMapper());
    }

    private Usuario usuario(RolUsuario rol) {
        return Usuario.builder().id(5).uuid("u-5").nombreUsuario("bot").nombreCompleto("Scraper").email("bot@test.com").rol(rol).build();
    }

    private SugerenciaRutaRequestDTO request() {
        return SugerenciaRutaRequestDTO.builder()
                .urlFuente(" https://rutas.example/montseny ")
                .nombre("  Curvas   del Montseny ")
                .puntoInicio(" Sant Celoni")
                .puntoFin("Viladrau")
                .distanciaKm(new BigDecimal("48.5"))
                .puntos(List.of(
                        new PuntoSugeridoDTO("Sant Celoni", 41.689, 2.489),
                        new PuntoSugeridoDTO("Collformic", null, null)))
                .build();
    }

    @Test
    void crear_scraper_normalizaTextosYGuardaLosPuntosComoJson() {
        when(usuarioAutenticado.obtenerUsuarioActual()).thenReturn(usuario(RolUsuario.scraper));
        when(sugerenciaRepository.save(any(SugerenciaRuta.class))).thenAnswer(inv -> inv.getArgument(0));

        SugerenciaRutaResponseDTO resultado = service.crear(request());

        ArgumentCaptor<SugerenciaRuta> guardada = ArgumentCaptor.forClass(SugerenciaRuta.class);
        verify(sugerenciaRepository).save(guardada.capture());
        assertThat(guardada.getValue().getNombre()).isEqualTo("Curvas del Montseny");
        assertThat(guardada.getValue().getPuntoInicio()).isEqualTo("Sant Celoni");
        assertThat(guardada.getValue().getUrlFuente()).isEqualTo("https://rutas.example/montseny");
        assertThat(guardada.getValue().getEstado()).isEqualTo(EstadoSugerencia.pendiente);
        // Los puntos se leen de vuelta del JSON guardado, con las coordenadas que faltaban a null.
        assertThat(resultado.getPuntos()).extracting(PuntoSugeridoDTO::getNombre).containsExactly("Sant Celoni", "Collformic");
        assertThat(resultado.getPuntos().get(0).getLatitud()).isEqualTo(41.689);
        assertThat(resultado.getPuntos().get(1).getLatitud()).isNull();
    }

    @Test
    void crear_usuarioNormal_seRechaza() {
        when(usuarioAutenticado.obtenerUsuarioActual()).thenReturn(usuario(RolUsuario.user));

        assertThatThrownBy(() -> service.crear(request())).isInstanceOf(BadRequestException.class);
        verify(sugerenciaRepository, never()).save(any());
    }

    @Test
    void crear_rutaYaEnCatalogo_409() {
        when(usuarioAutenticado.obtenerUsuarioActual()).thenReturn(usuario(RolUsuario.scraper));
        when(rutaRepository.existeEnCatalogo("Curvas del Montseny", "Sant Celoni")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(request())).isInstanceOf(DuplicateResourceException.class);
        verify(sugerenciaRepository, never()).save(any());
    }

    @Test
    void crear_yaSugeridaAntes_409() {
        when(usuarioAutenticado.obtenerUsuarioActual()).thenReturn(usuario(RolUsuario.scraper));
        when(sugerenciaRepository.existeSugerida("Curvas del Montseny", "Sant Celoni")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(request())).isInstanceOf(DuplicateResourceException.class);
        verify(sugerenciaRepository, never()).save(any());
    }

    @Test
    void crear_topeDePendientes_seRechaza() {
        when(usuarioAutenticado.obtenerUsuarioActual()).thenReturn(usuario(RolUsuario.scraper));
        when(sugerenciaRepository.countByUsuarioIdAndEstado(5, EstadoSugerencia.pendiente))
                .thenReturn((long) SugerenciaRutaServiceImpl.MAX_PENDIENTES);

        assertThatThrownBy(() -> service.crear(request())).isInstanceOf(BadRequestException.class);
        verify(sugerenciaRepository, never()).save(any());
    }

    @Test
    void aprobar_vinculaLaRutaCreada() {
        SugerenciaRuta pendiente = SugerenciaRuta.builder().uuid("s-1").usuario(usuario(RolUsuario.scraper))
                .nombre("X").puntoInicio("A").puntoFin("B").urlFuente("https://x").estado(EstadoSugerencia.pendiente).build();
        when(sugerenciaRepository.findByUuid("s-1")).thenReturn(Optional.of(pendiente));
        when(rutaRepository.findByUuid("r-9")).thenReturn(Optional.of(Ruta.builder().id(9).uuid("r-9").build()));
        when(sugerenciaRepository.save(any(SugerenciaRuta.class))).thenAnswer(inv -> inv.getArgument(0));

        SugerenciaRutaResponseDTO resultado = service.aprobar("s-1", "r-9");

        assertThat(resultado.getEstado()).isEqualTo(EstadoSugerencia.aprobada);
        assertThat(resultado.getRutaUuid()).isEqualTo("r-9");
    }

    @Test
    void rechazar_unaYaResuelta_noSeReabre() {
        SugerenciaRuta aprobada = SugerenciaRuta.builder().uuid("s-2").usuario(usuario(RolUsuario.scraper))
                .estado(EstadoSugerencia.aprobada).build();
        when(sugerenciaRepository.findByUuid("s-2")).thenReturn(Optional.of(aprobada));

        assertThatThrownBy(() -> service.rechazar("s-2", "no")).isInstanceOf(BadRequestException.class);
        verify(sugerenciaRepository, never()).save(any());
    }
}
