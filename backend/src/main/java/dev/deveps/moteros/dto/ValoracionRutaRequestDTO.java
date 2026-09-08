package dev.deveps.moteros.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Valoracion de una ruta. La ruta llega por la URL y el autor del usuario
 * autenticado; aqui solo viajan la puntuacion y el comentario.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValoracionRutaRequestDTO {

    @NotNull(message = "La puntuacion es obligatoria")
    @Min(value = 1, message = "La puntuacion minima es 1")
    @Max(value = 5, message = "La puntuacion maxima es 5")
    private Integer puntuacion;

    @Size(max = 280, message = "El comentario no puede superar los 280 caracteres")
    private String comentario;
}
