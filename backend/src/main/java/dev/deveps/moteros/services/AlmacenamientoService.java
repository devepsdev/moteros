package dev.deveps.moteros.services;

import dev.deveps.moteros.dto.ArchivoSubidoDTO;
import org.springframework.web.multipart.MultipartFile;

public interface AlmacenamientoService {

    /** Guarda una imagen (jpg, png, webp o gif) y devuelve su URL publica. */
    ArchivoSubidoDTO guardarImagen(MultipartFile archivo);
}
