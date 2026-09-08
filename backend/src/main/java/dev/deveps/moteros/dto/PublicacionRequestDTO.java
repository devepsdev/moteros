package dev.deveps.moteros.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Alta o edicion de una publicacion del muro. El autor se toma del usuario autenticado. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicacionRequestDTO {

    @NotBlank(message = "El contenido es obligatorio")
    @Size(max = 5000, message = "El contenido no puede superar los 5000 caracteres")
    private String contenido;

    @Size(max = 255, message = "La URL de la imagen no puede superar los 255 caracteres")
    private String imagenUrl;

    /** UUID de la ruta que se comparte en la publicacion. Opcional. */
    private String rutaUuid;
}
