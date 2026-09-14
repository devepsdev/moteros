package dev.deveps.moteros.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Rechazo de una sugerencia, con motivo opcional. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RechazarSugerenciaDTO {

    @Size(max = 255, message = "El motivo no puede superar los 255 caracteres")
    private String motivo;
}
