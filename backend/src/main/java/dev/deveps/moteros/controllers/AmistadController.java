package dev.deveps.moteros.controllers;

import dev.deveps.moteros.controllers.support.PageableFactory;
import dev.deveps.moteros.dto.AmistadResponseDTO;
import dev.deveps.moteros.dto.ApiResponseDTO;
import dev.deveps.moteros.dto.PagedResponseDTO;
import dev.deveps.moteros.dto.UsuarioSummaryDTO;
import dev.deveps.moteros.services.AmistadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/amistades")
@RequiredArgsConstructor
public class AmistadController {

    private final AmistadService amistadService;

    @PostMapping("/{usuarioUuid}")
    public ResponseEntity<ApiResponseDTO<AmistadResponseDTO>> enviarSolicitud(@PathVariable String usuarioUuid) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(amistadService.enviarSolicitud(usuarioUuid),
                        "Solicitud de amistad enviada"));
    }

    @PatchMapping("/{amistadUuid}")
    public ResponseEntity<ApiResponseDTO<AmistadResponseDTO>> responder(
            @PathVariable String amistadUuid, @RequestParam boolean aceptar) {
        return ResponseEntity.ok(ApiResponseDTO.success(
                amistadService.responder(amistadUuid, aceptar),
                aceptar ? "Solicitud aceptada" : "Solicitud rechazada"));
    }

    @DeleteMapping("/{usuarioUuid}")
    public ResponseEntity<ApiResponseDTO<Void>> eliminar(@PathVariable String usuarioUuid) {
        amistadService.eliminar(usuarioUuid);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Amistad eliminada correctamente"));
    }

    @GetMapping("/usuario/{usuarioUuid}/amigos")
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<UsuarioSummaryDTO>>> listarAmigos(
            @PathVariable String usuarioUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageableFactory.of(page, size);
        return ResponseEntity.ok(ApiResponseDTO.success(
                PagedResponseDTO.of(amistadService.listarAmigos(usuarioUuid, pageable)),
                "Amigos obtenidos correctamente"));
    }

    @GetMapping("/solicitudes/recibidas")
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<AmistadResponseDTO>>> recibidas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageableFactory.of(page, size, "fecha", "desc", "fecha");
        return ResponseEntity.ok(ApiResponseDTO.success(
                PagedResponseDTO.of(amistadService.solicitudesRecibidas(pageable)),
                "Solicitudes recibidas obtenidas correctamente"));
    }

    @GetMapping("/solicitudes/enviadas")
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<AmistadResponseDTO>>> enviadas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageableFactory.of(page, size, "fecha", "desc", "fecha");
        return ResponseEntity.ok(ApiResponseDTO.success(
                PagedResponseDTO.of(amistadService.solicitudesEnviadas(pageable)),
                "Solicitudes enviadas obtenidas correctamente"));
    }

    @GetMapping("/relacion/{usuarioUuid}")
    public ResponseEntity<ApiResponseDTO<AmistadResponseDTO>> relacionCon(@PathVariable String usuarioUuid) {
        return ResponseEntity.ok(ApiResponseDTO.success(
                amistadService.relacionCon(usuarioUuid), "Relacion obtenida correctamente"));
    }
}
