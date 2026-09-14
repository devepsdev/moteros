package dev.deveps.moteros.controllers;

import dev.deveps.moteros.dto.ApiResponseDTO;
import dev.deveps.moteros.dto.SugerenciaRutaRequestDTO;
import dev.deveps.moteros.dto.SugerenciaRutaResponseDTO;
import dev.deveps.moteros.services.SugerenciaRutaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Alta de sugerencias de rutas. Solo la usa la cuenta del scraper (rol scraper, ver SecurityConfig). */
@RestController
@RequestMapping("/api/sugerencias-ruta")
@RequiredArgsConstructor
public class SugerenciaRutaController {

    private final SugerenciaRutaService sugerenciaRutaService;

    @PostMapping
    public ResponseEntity<ApiResponseDTO<SugerenciaRutaResponseDTO>> crear(@Valid @RequestBody SugerenciaRutaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(sugerenciaRutaService.crear(dto), "Sugerencia registrada correctamente"));
    }
}
