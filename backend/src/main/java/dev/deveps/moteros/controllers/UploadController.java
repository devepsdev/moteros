package dev.deveps.moteros.controllers;

import dev.deveps.moteros.dto.ApiResponseDTO;
import dev.deveps.moteros.dto.ArchivoSubidoDTO;
import dev.deveps.moteros.services.AlmacenamientoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Subida de imagenes. Requiere autenticacion. Devuelve la URL publica que el cliente
 * guardara luego en {@code fotoPerfilUrl} / {@code fotoUrl} / {@code imagenUrl}.
 */
@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final AlmacenamientoService almacenamientoService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseDTO<ArchivoSubidoDTO>> subirImagen(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(almacenamientoService.guardarImagen(file),
                        "Imagen subida correctamente"));
    }
}
