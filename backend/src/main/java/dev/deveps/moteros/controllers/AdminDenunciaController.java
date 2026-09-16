package dev.deveps.moteros.controllers;

import dev.deveps.moteros.controllers.support.PageableFactory;
import dev.deveps.moteros.dto.ApiResponseDTO;
import dev.deveps.moteros.dto.DenunciaResponseDTO;
import dev.deveps.moteros.dto.PagedResponseDTO;
import dev.deveps.moteros.dto.ResolverDenunciaDTO;
import dev.deveps.moteros.entities.enums.EstadoDenuncia;
import dev.deveps.moteros.services.DenunciaService;
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

/** Bandeja de moderacion de denuncias. Requiere rol ADMIN ({@code /api/admin/**}). */
@RestController
@RequestMapping("/api/admin/denuncias")
@RequiredArgsConstructor
public class AdminDenunciaController {

    private final DenunciaService denunciaService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<DenunciaResponseDTO>>> listar(
            @RequestParam(required = false) EstadoDenuncia estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // El orden va en la consulta: el Pageable llega sin sort.
        return ResponseEntity.ok(ApiResponseDTO.success(
                PagedResponseDTO.of(denunciaService.listar(estado, PageableFactory.of(page, size))),
                "Denuncias obtenidas correctamente"));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponseDTO<DenunciaResponseDTO>> obtener(@PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponseDTO.success(denunciaService.obtener(uuid), "Denuncia obtenida correctamente"));
    }

    @PutMapping("/{uuid}/resolver")
    public ResponseEntity<ApiResponseDTO<DenunciaResponseDTO>> resolver(
            @PathVariable String uuid, @Valid @RequestBody ResolverDenunciaDTO dto) {
        return ResponseEntity.ok(ApiResponseDTO.success(denunciaService.resolver(uuid, dto), "Denuncia cerrada"));
    }
}
