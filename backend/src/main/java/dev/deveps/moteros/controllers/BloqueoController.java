package dev.deveps.moteros.controllers;

import dev.deveps.moteros.controllers.support.PageableFactory;
import dev.deveps.moteros.dto.ApiResponseDTO;
import dev.deveps.moteros.dto.PagedResponseDTO;
import dev.deveps.moteros.dto.UsuarioSummaryDTO;
import dev.deveps.moteros.services.BloqueoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/bloqueos")
@RequiredArgsConstructor
public class BloqueoController {

    private final BloqueoService bloqueoService;

    @PostMapping("/{usuarioUuid}")
    public ResponseEntity<ApiResponseDTO<Void>> bloquear(@PathVariable String usuarioUuid) {
        bloqueoService.bloquear(usuarioUuid);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Usuario bloqueado"));
    }

    @DeleteMapping("/{usuarioUuid}")
    public ResponseEntity<ApiResponseDTO<Void>> desbloquear(@PathVariable String usuarioUuid) {
        bloqueoService.desbloquear(usuarioUuid);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Usuario desbloqueado"));
    }

    @GetMapping
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<UsuarioSummaryDTO>>> misBloqueados(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponseDTO.success(
                PagedResponseDTO.of(bloqueoService.misBloqueados(PageableFactory.of(page, size))),
                "Usuarios bloqueados obtenidos correctamente"));
    }

    @GetMapping("/{usuarioUuid}")
    public ResponseEntity<ApiResponseDTO<Map<String, Boolean>>> estado(@PathVariable String usuarioUuid) {
        return ResponseEntity.ok(ApiResponseDTO.success(
                Map.of("bloqueado", bloqueoService.heBloqueado(usuarioUuid)), "Estado del bloqueo"));
    }
}
