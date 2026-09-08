package dev.deveps.moteros.services;

import dev.deveps.moteros.dto.InscripcionQuedadaResponseDTO;
import dev.deveps.moteros.dto.QuedadaFilterDTO;
import dev.deveps.moteros.dto.QuedadaRequestDTO;
import dev.deveps.moteros.dto.QuedadaResponseDTO;
import dev.deveps.moteros.dto.QuedadaSummaryDTO;
import dev.deveps.moteros.entities.enums.EstadoInscripcion;
import dev.deveps.moteros.entities.enums.EstadoQuedada;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface QuedadaService {

    Page<QuedadaSummaryDTO> buscar(String texto, Pageable pageable);

    Page<QuedadaSummaryDTO> filtrar(QuedadaFilterDTO filtro, Pageable pageable);

    Page<QuedadaSummaryDTO> listarPorOrganizador(String organizadorUuid, Pageable pageable);

    Page<QuedadaSummaryDTO> misInscripciones(Pageable pageable);

    QuedadaResponseDTO obtenerPorUuid(String uuid);

    QuedadaResponseDTO crear(QuedadaRequestDTO dto);

    QuedadaResponseDTO actualizar(String uuid, QuedadaRequestDTO dto);

    QuedadaResponseDTO cambiarEstado(String uuid, EstadoQuedada estado);

    void eliminar(String uuid);

    // ===== INSCRIPCIONES =====

    InscripcionQuedadaResponseDTO inscribirse(String quedadaUuid);

    void cancelarInscripcion(String quedadaUuid);

    List<InscripcionQuedadaResponseDTO> listarInscritos(String quedadaUuid);

    InscripcionQuedadaResponseDTO cambiarEstadoInscripcion(String quedadaUuid, String usuarioUuid, EstadoInscripcion estado);
}
