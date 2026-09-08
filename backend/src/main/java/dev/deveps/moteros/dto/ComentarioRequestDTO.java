package dev.deveps.moteros.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Alta de un comentario sobre una publicacion. La publicacion llega por la URL. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComentarioRequestDTO {

    @NotBlank(message = "El contenido es obligatorio")
    @Size(max = 280, message = "El comentario no puede superar los 280 caracteres")
    private String contenido;
}
