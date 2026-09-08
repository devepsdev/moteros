package dev.deveps.moteros.controllers;

import dev.deveps.moteros.dto.ApiResponseDTO;
import dev.deveps.moteros.dto.MotoRequestDTO;
import dev.deveps.moteros.dto.MotoResponseDTO;
import dev.deveps.moteros.services.MotoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/motos")
@RequiredArgsConstructor
public class MotoController {

    private final MotoService motoService;

    @GetMapping("/mias")
    public ResponseEntity<ApiResponseDTO<List<MotoResponseDTO>>> misMotos() {
        return ResponseEntity.ok(ApiResponseDTO.success(
                motoService.listarMisMotos(), "Motos obtenidas correctamente"));
    }

    @GetMapping("/usuario/{usuarioUuid}")
    public ResponseEntity<ApiResponseDTO<List<MotoResponseDTO>>> porUsuario(@PathVariable String usuarioUuid) {
        return ResponseEntity.ok(ApiResponseDTO.success(
                motoService.listarPorUsuario(usuarioUuid), "Motos obtenidas correctamente"));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponseDTO<MotoResponseDTO>> obtener(@PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponseDTO.success(
                motoService.obtenerPorUuid(uuid), "Moto obtenida correctamente"));
    }

    @PostMapping
    public ResponseEntity<ApiResponseDTO<MotoResponseDTO>> crear(@Valid @RequestBody MotoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(motoService.crear(dto), "Moto creada correctamente"));
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<ApiResponseDTO<MotoResponseDTO>> actualizar(
            @PathVariable String uuid, @Valid @RequestBody MotoRequestDTO dto) {
        return ResponseEntity.ok(ApiResponseDTO.success(
                motoService.actualizar(uuid, dto), "Moto actualizada correctamente"));
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<ApiResponseDTO<Void>> eliminar(@PathVariable String uuid) {
        motoService.eliminar(uuid);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Moto eliminada correctamente"));
    }
}
