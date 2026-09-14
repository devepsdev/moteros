package dev.deveps.moteros.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Aprobacion de una sugerencia: el panel crea antes la ruta y aqui indica cual es. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AprobarSugerenciaDTO {

    @NotBlank(message = "El UUID de la ruta creada es obligatorio")
    private String rutaUuid;
}
