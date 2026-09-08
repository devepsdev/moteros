package dev.deveps.moteros.controllers;

import dev.deveps.moteros.controllers.support.PageableFactory;
import dev.deveps.moteros.dto.ApiResponseDTO;
import dev.deveps.moteros.dto.PagedResponseDTO;
import dev.deveps.moteros.dto.PuntoRutaRequestDTO;
import dev.deveps.moteros.dto.PuntoRutaResponseDTO;
import dev.deveps.moteros.dto.RutaFilterDTO;
import dev.deveps.moteros.dto.RutaRequestDTO;
import dev.deveps.moteros.dto.RutaResponseDTO;
import dev.deveps.moteros.dto.RutaSearchDTO;
import dev.deveps.moteros.dto.RutaSummaryDTO;
import dev.deveps.moteros.dto.ValoracionRutaRequestDTO;
import dev.deveps.moteros.dto.ValoracionRutaResponseDTO;
import dev.deveps.moteros.services.RutaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rutas")
@RequiredArgsConstructor
public class RutaController {

    private final RutaService rutaService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<RutaSummaryDTO>>> buscar(
            @Valid RutaSearchDTO searchDTO) {
        Pageable pageable = PageableFactory.of(searchDTO.getPage(), searchDTO.getSize(),
                searchDTO.getSortBy(), searchDTO.getSortDir(), "fechaCreacion");
        return ResponseEntity.ok(ApiResponseDTO.success(
                PagedResponseDTO.of(rutaService.buscar(searchDTO.getSearchText(), pageable)),
                "Rutas obtenidas correctamente"));
    }

    @GetMapping("/filtrar")
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<RutaSummaryDTO>>> filtrar(
            @Valid RutaFilterDTO filtro) {
        Pageable pageable = PageableFactory.of(filtro.getPage(), filtro.getSize(),
                filtro.getSortBy(), filtro.getSortDir(), "fechaCreacion");
        return ResponseEntity.ok(ApiResponseDTO.success(
                PagedResponseDTO.of(rutaService.filtrar(filtro, pageable)),
                "Rutas filtradas correctamente"));
    }

    @GetMapping("/usuario/{creadorUuid}")
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<RutaSummaryDTO>>> porCreador(
            @PathVariable String creadorUuid, @Valid RutaSearchDTO searchDTO) {
        Pageable pageable = PageableFactory.of(searchDTO.getPage(), searchDTO.getSize(),
                searchDTO.getSortBy(), searchDTO.getSortDir(), "fechaCreacion");
        return ResponseEntity.ok(ApiResponseDTO.success(
                PagedResponseDTO.of(rutaService.listarPorCreador(creadorUuid, pageable)),
                "Rutas del usuario obtenidas correctamente"));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponseDTO<RutaResponseDTO>> obtener(@PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponseDTO.success(
                rutaService.obtenerPorUuid(uuid), "Ruta obtenida correctamente"));
    }

    @PostMapping
    public ResponseEntity<ApiResponseDTO<RutaResponseDTO>> crear(@Valid @RequestBody RutaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(rutaService.crear(dto), "Ruta creada correctamente"));
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<ApiResponseDTO<RutaResponseDTO>> actualizar(
            @PathVariable String uuid, @Valid @RequestBody RutaRequestDTO dto) {
        return ResponseEntity.ok(ApiResponseDTO.success(
                rutaService.actualizar(uuid, dto), "Ruta actualizada correctamente"));
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<ApiResponseDTO<Void>> eliminar(@PathVariable String uuid) {
        rutaService.eliminar(uuid);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Ruta eliminada correctamente"));
    }

    // ===================== TRACK =====================

    @GetMapping("/{uuid}/puntos")
    public ResponseEntity<ApiResponseDTO<List<PuntoRutaResponseDTO>>> obtenerTrack(@PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponseDTO.success(
                rutaService.obtenerTrack(uuid), "Track obtenido correctamente"));
    }

    @PutMapping("/{uuid}/puntos")
    public ResponseEntity<ApiResponseDTO<List<PuntoRutaResponseDTO>>> reemplazarTrack(
            @PathVariable String uuid, @Valid @RequestBody List<PuntoRutaRequestDTO> puntos) {
        return ResponseEntity.ok(ApiResponseDTO.success(
                rutaService.reemplazarTrack(uuid, puntos), "Track actualizado correctamente"));
    }

    // ===================== VALORACIONES =====================

    @GetMapping("/{uuid}/valoraciones")
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<ValoracionRutaResponseDTO>>> listarValoraciones(
            @PathVariable String uuid, @Valid RutaSearchDTO searchDTO) {
        Pageable pageable = PageableFactory.of(searchDTO.getPage(), searchDTO.getSize(),
                searchDTO.getSortBy(), searchDTO.getSortDir(), "fecha");
        return ResponseEntity.ok(ApiResponseDTO.success(
                PagedResponseDTO.of(rutaService.listarValoraciones(uuid, pageable)),
                "Valoraciones obtenidas correctamente"));
    }

    @PutMapping("/{uuid}/valoraciones")
    public ResponseEntity<ApiResponseDTO<ValoracionRutaResponseDTO>> valorar(
            @PathVariable String uuid, @Valid @RequestBody ValoracionRutaRequestDTO dto) {
        return ResponseEntity.ok(ApiResponseDTO.success(
                rutaService.valorar(uuid, dto), "Valoracion registrada correctamente"));
    }

    @DeleteMapping("/{uuid}/valoraciones")
    public ResponseEntity<ApiResponseDTO<Void>> eliminarMiValoracion(@PathVariable String uuid) {
        rutaService.eliminarMiValoracion(uuid);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Valoracion eliminada correctamente"));
    }
}
