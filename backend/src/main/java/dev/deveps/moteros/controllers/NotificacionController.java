package dev.deveps.moteros.controllers;

import dev.deveps.moteros.controllers.support.PageableFactory;
import dev.deveps.moteros.dto.ApiResponseDTO;
import dev.deveps.moteros.dto.NotificacionResponseDTO;
import dev.deveps.moteros.dto.PagedResponseDTO;
import dev.deveps.moteros.services.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<NotificacionResponseDTO>>> listar(
            @RequestParam(defaultValue = "false") boolean soloNoLeidas,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageableFactory.of(page, size, "fechaCreacion", "desc", "fechaCreacion");
        return ResponseEntity.ok(ApiResponseDTO.success(
                PagedResponseDTO.of(notificacionService.listar(soloNoLeidas, pageable)),
                "Notificaciones obtenidas correctamente"));
    }

    @GetMapping("/no-leidas/contador")
    public ResponseEntity<ApiResponseDTO<Map<String, Long>>> contador() {
        return ResponseEntity.ok(ApiResponseDTO.success(
                Map.of("noLeidas", notificacionService.contarNoLeidas()), "Contador obtenido correctamente"));
    }

    @PatchMapping("/{uuid}/leida")
    public ResponseEntity<ApiResponseDTO<Void>> marcarLeida(@PathVariable String uuid) {
        notificacionService.marcarLeida(uuid);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Notificacion marcada como leida"));
    }

    @PatchMapping("/leidas")
    public ResponseEntity<ApiResponseDTO<Void>> marcarTodasLeidas() {
        notificacionService.marcarTodasLeidas();
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Todas las notificaciones marcadas como leidas"));
    }
}
