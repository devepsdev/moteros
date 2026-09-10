package dev.deveps.moteros.controllers;

import dev.deveps.moteros.dto.ApiResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Endpoint publico de salud para el reverse proxy y la monitorizacion. */
@RestController
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<ApiResponseDTO<Map<String, String>>> health() {
        return ResponseEntity.ok(ApiResponseDTO.success(Map.of("status", "UP"), "OK"));
    }
}
