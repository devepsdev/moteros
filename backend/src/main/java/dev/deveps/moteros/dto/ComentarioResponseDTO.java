package dev.deveps.moteros.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Comentario de una publicacion con el autor anidado. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComentarioResponseDTO {

    private String uuid;

    private String publicacionUuid;

    private UsuarioSummaryDTO autor;

    private String contenido;

    private LocalDateTime fecha;
}
