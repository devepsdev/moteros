package dev.deveps.moteros.services;

import dev.deveps.moteros.dto.SugerenciaRutaRequestDTO;
import dev.deveps.moteros.dto.SugerenciaRutaResponseDTO;
import dev.deveps.moteros.entities.enums.EstadoSugerencia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Bandeja de rutas sugeridas por el scraper, revisada por los administradores. */
public interface SugerenciaRutaService {

    /**
     * Registra una ruta encontrada por el scraper. Se descarta (409) si ya hay en el catalogo
     * una ruta con el mismo nombre y salida, o si ya se sugirio antes aunque se rechazara.
     */
    SugerenciaRutaResponseDTO crear(SugerenciaRutaRequestDTO dto);

    /** Listado para el panel; con {@code estado} null devuelve todas. */
    Page<SugerenciaRutaResponseDTO> listar(EstadoSugerencia estado, Pageable pageable);

    SugerenciaRutaResponseDTO obtener(String uuid);

    /** Marca como aprobada una sugerencia pendiente y la vincula con la ruta creada a partir de ella. */
    SugerenciaRutaResponseDTO aprobar(String uuid, String rutaUuid);

    SugerenciaRutaResponseDTO rechazar(String uuid, String motivo);
}
