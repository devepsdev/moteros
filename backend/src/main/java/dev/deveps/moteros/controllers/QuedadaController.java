package dev.deveps.moteros.controllers;

import dev.deveps.moteros.controllers.support.PageableFactory;
import dev.deveps.moteros.dto.ApiResponseDTO;
import dev.deveps.moteros.dto.InscripcionQuedadaResponseDTO;
import dev.deveps.moteros.dto.PagedResponseDTO;
import dev.deveps.moteros.dto.QuedadaFilterDTO;
import dev.deveps.moteros.dto.QuedadaRequestDTO;
import dev.deveps.moteros.dto.QuedadaResponseDTO;
import dev.deveps.moteros.dto.QuedadaSearchDTO;
import dev.deveps.moteros.dto.QuedadaSummaryDTO;
import dev.deveps.moteros.entities.enums.EstadoInscripcion;
import dev.deveps.moteros.entities.enums.EstadoQuedada;
import dev.deveps.moteros.services.QuedadaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/quedadas")
@RequiredArgsConstructor
public class QuedadaController {

    private final QuedadaService quedadaService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<QuedadaSummaryDTO>>> buscar(
            @Valid QuedadaSearchDTO searchDTO) {
        Pageable pageable = PageableFactory.of(searchDTO.getPage(), searchDTO.getSize(),
                searchDTO.getSortBy(), searchDTO.getSortDir(), "fechaHora");
        return ResponseEntity.ok(ApiResponseDTO.success(
                PagedResponseDTO.of(quedadaService.buscar(searchDTO.getSearchText(), pageable)),
                "Quedadas obtenidas correctamente"));
    }

    @GetMapping("/filtrar")
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<QuedadaSummaryDTO>>> filtrar(
            @Valid QuedadaFilterDTO filtro) {
        Pageable pageable = PageableFactory.of(filtro.getPage(), filtro.getSize(),
                filtro.getSortBy(), filtro.getSortDir(), "fechaHora");
        return ResponseEntity.ok(ApiResponseDTO.success(
                PagedResponseDTO.of(quedadaService.filtrar(filtro, pageable)),
                "Quedadas filtradas correctamente"));
    }

    @GetMapping("/organizador/{organizadorUuid}")
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<QuedadaSummaryDTO>>> porOrganizador(
            @PathVariable String organizadorUuid, @Valid QuedadaSearchDTO searchDTO) {
        Pageable pageable = PageableFactory.of(searchDTO.getPage(), searchDTO.getSize(),
                searchDTO.getSortBy(), searchDTO.getSortDir(), "fechaHora");
        return ResponseEntity.ok(ApiResponseDTO.success(
                PagedResponseDTO.of(quedadaService.listarPorOrganizador(organizadorUuid, pageable)),
                "Quedadas del organizador obtenidas correctamente"));
    }

    @GetMapping("/mis-inscripciones")
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<QuedadaSummaryDTO>>> misInscripciones(
            @Valid QuedadaSearchDTO searchDTO) {
        Pageable pageable = PageableFactory.of(searchDTO.getPage(), searchDTO.getSize(),
                searchDTO.getSortBy(), searchDTO.getSortDir(), "fechaHora");
        return ResponseEntity.ok(ApiResponseDTO.success(
                PagedResponseDTO.of(quedadaService.misInscripciones(pageable)),
                "Inscripciones obtenidas correctamente"));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponseDTO<QuedadaResponseDTO>> obtener(@PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponseDTO.success(
                quedadaService.obtenerPorUuid(uuid), "Quedada obtenida correctamente"));
    }

    @PostMapping
    public ResponseEntity<ApiResponseDTO<QuedadaResponseDTO>> crear(@Valid @RequestBody QuedadaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(quedadaService.crear(dto), "Quedada creada correctamente"));
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<ApiResponseDTO<QuedadaResponseDTO>> actualizar(
            @PathVariable String uuid, @Valid @RequestBody QuedadaRequestDTO dto) {
        return ResponseEntity.ok(ApiResponseDTO.success(
                quedadaService.actualizar(uuid, dto), "Quedada actualizada correctamente"));
    }

    @PatchMapping("/{uuid}/estado")
    public ResponseEntity<ApiResponseDTO<QuedadaResponseDTO>> cambiarEstado(
            @PathVariable String uuid, @RequestParam EstadoQuedada estado) {
        return ResponseEntity.ok(ApiResponseDTO.success(
                quedadaService.cambiarEstado(uuid, estado), "Estado de la quedada actualizado"));
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<ApiResponseDTO<Void>> eliminar(@PathVariable String uuid) {
        quedadaService.eliminar(uuid);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Quedada eliminada correctamente"));
    }

    // ===================== INSCRIPCIONES =====================

    @PostMapping("/{uuid}/inscripciones")
    public ResponseEntity<ApiResponseDTO<InscripcionQuedadaResponseDTO>> inscribirse(@PathVariable String uuid) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDTO.success(
                quedadaService.inscribirse(uuid), "Inscripcion realizada correctamente"));
    }

    @DeleteMapping("/{uuid}/inscripciones")
    public ResponseEntity<ApiResponseDTO<Void>> cancelarInscripcion(@PathVariable String uuid) {
        quedadaService.cancelarInscripcion(uuid);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Inscripcion cancelada correctamente"));
    }

    @GetMapping("/{uuid}/inscripciones")
    public ResponseEntity<ApiResponseDTO<List<InscripcionQuedadaResponseDTO>>> listarInscritos(
            @PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponseDTO.success(
                quedadaService.listarInscritos(uuid), "Inscritos obtenidos correctamente"));
    }

    @PatchMapping("/{uuid}/inscripciones/{usuarioUuid}")
    public ResponseEntity<ApiResponseDTO<InscripcionQuedadaResponseDTO>> cambiarEstadoInscripcion(
            @PathVariable String uuid, @PathVariable String usuarioUuid,
            @RequestParam EstadoInscripcion estado) {
        return ResponseEntity.ok(ApiResponseDTO.success(
                quedadaService.cambiarEstadoInscripcion(uuid, usuarioUuid, estado),
                "Estado de la inscripcion actualizado"));
    }
}
