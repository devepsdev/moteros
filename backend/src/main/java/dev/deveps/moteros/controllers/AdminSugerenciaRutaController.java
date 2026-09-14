package dev.deveps.moteros.controllers;

import dev.deveps.moteros.controllers.support.PageableFactory;
import dev.deveps.moteros.dto.ApiResponseDTO;
import dev.deveps.moteros.dto.AprobarSugerenciaDTO;
import dev.deveps.moteros.dto.PagedResponseDTO;
import dev.deveps.moteros.dto.RechazarSugerenciaDTO;
import dev.deveps.moteros.dto.SugerenciaRutaResponseDTO;
import dev.deveps.moteros.entities.enums.EstadoSugerencia;
import dev.deveps.moteros.services.SugerenciaRutaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Bandeja de revision de rutas sugeridas. Requiere rol ADMIN ({@code /api/admin/**}). */
@RestController
@RequestMapping("/api/admin/sugerencias-ruta")
@RequiredArgsConstructor
public class AdminSugerenciaRutaController {

    private final SugerenciaRutaService sugerenciaRutaService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<SugerenciaRutaResponseDTO>>> listar(
            @RequestParam(required = false) EstadoSugerencia estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // El orden va en la consulta (mas recientes primero): el Pageable llega sin sort.
        return ResponseEntity.ok(ApiResponseDTO.success(
                PagedResponseDTO.of(sugerenciaRutaService.listar(estado, PageableFactory.of(page, size))),
                "Sugerencias obtenidas correctamente"));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponseDTO<SugerenciaRutaResponseDTO>> obtener(@PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponseDTO.success(sugerenciaRutaService.obtener(uuid), "Sugerencia obtenida correctamente"));
    }

    @PutMapping("/{uuid}/aprobar")
    public ResponseEntity<ApiResponseDTO<SugerenciaRutaResponseDTO>> aprobar(
            @PathVariable String uuid, @Valid @RequestBody AprobarSugerenciaDTO dto) {
        return ResponseEntity.ok(ApiResponseDTO.success(
                sugerenciaRutaService.aprobar(uuid, dto.getRutaUuid()), "Sugerencia aprobada"));
    }

    @PutMapping("/{uuid}/rechazar")
    public ResponseEntity<ApiResponseDTO<SugerenciaRutaResponseDTO>> rechazar(
            @PathVariable String uuid, @Valid @RequestBody RechazarSugerenciaDTO dto) {
        return ResponseEntity.ok(ApiResponseDTO.success(
                sugerenciaRutaService.rechazar(uuid, dto.getMotivo()), "Sugerencia rechazada"));
    }
}
