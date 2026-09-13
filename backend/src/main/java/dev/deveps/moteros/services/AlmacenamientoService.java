package dev.deveps.moteros.services;

import dev.deveps.moteros.dto.ArchivoSubidoDTO;
import org.springframework.web.multipart.MultipartFile;

public interface AlmacenamientoService {

    /** Guarda una imagen (jpg, png, webp o gif) y devuelve su URL publica. */
    ArchivoSubidoDTO guardarImagen(MultipartFile archivo);

    /**
     * Borra del disco la imagen a la que apunta una URL publica devuelta por {@link #guardarImagen}.
     * Ignora URLs vacias o externas y no falla si el archivo ya no existe.
     */
    void eliminarPorUrl(String url);
}
