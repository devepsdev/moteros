package dev.deveps.moteros.services;

import dev.deveps.moteros.dto.PuntoRutaRequestDTO;
import dev.deveps.moteros.dto.PuntoRutaResponseDTO;
import dev.deveps.moteros.dto.RutaFilterDTO;
import dev.deveps.moteros.dto.RutaRequestDTO;
import dev.deveps.moteros.dto.RutaResponseDTO;
import dev.deveps.moteros.dto.RutaSummaryDTO;
import dev.deveps.moteros.dto.ValoracionRutaRequestDTO;
import dev.deveps.moteros.dto.ValoracionRutaResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RutaService {

    Page<RutaSummaryDTO> buscar(String texto, Pageable pageable);

    Page<RutaSummaryDTO> filtrar(RutaFilterDTO filtro, Pageable pageable);

    Page<RutaSummaryDTO> listarPorCreador(String creadorUuid, Pageable pageable);

    RutaResponseDTO obtenerPorUuid(String uuid);

    RutaResponseDTO crear(RutaRequestDTO dto);

    RutaResponseDTO actualizar(String uuid, RutaRequestDTO dto);

    void eliminar(String uuid);

    // ===== TRACK =====

    List<PuntoRutaResponseDTO> obtenerTrack(String rutaUuid);

    List<PuntoRutaResponseDTO> reemplazarTrack(String rutaUuid, List<PuntoRutaRequestDTO> puntos);

    // ===== VALORACIONES =====

    Page<ValoracionRutaResponseDTO> listarValoraciones(String rutaUuid, Pageable pageable);

    ValoracionRutaResponseDTO valorar(String rutaUuid, ValoracionRutaRequestDTO dto);

    void eliminarMiValoracion(String rutaUuid);
}
