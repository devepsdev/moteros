package dev.deveps.moteros.controllers;

import dev.deveps.moteros.dto.ApiResponseDTO;
import dev.deveps.moteros.dto.DispositivoPushRequestDTO;
import dev.deveps.moteros.services.PushService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Alta y baja del movil donde el usuario quiere recibir avisos con la app cerrada. */
@RestController
@RequestMapping("/api/dispositivos")
@RequiredArgsConstructor
public class DispositivoPushController {

    private final PushService pushService;

    @PostMapping
    public ResponseEntity<ApiResponseDTO<Void>> registrar(@Valid @RequestBody DispositivoPushRequestDTO dto) {
        pushService.registrar(dto.getToken(), dto.getPlataforma());
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Dispositivo registrado correctamente"));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponseDTO<Void>> eliminar(@Valid @RequestBody DispositivoPushRequestDTO dto) {
        pushService.eliminar(dto.getToken());
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Dispositivo eliminado correctamente"));
    }
}
