package dev.deveps.moteros.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Valoracion de una ruta con el autor anidado. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValoracionRutaResponseDTO {

    private String uuid;

    private String rutaUuid;

    private UsuarioSummaryDTO autor;

    private Integer puntuacion;

    private String comentario;

    private LocalDateTime fecha;
}
