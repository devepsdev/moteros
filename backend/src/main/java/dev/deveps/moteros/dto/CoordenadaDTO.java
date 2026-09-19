package dev.deveps.moteros.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Una posicion en el mapa. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoordenadaDTO {

    @NotNull(message = "Falta la latitud")
    @DecimalMin(value = "-90", message = "Latitud no valida")
    @DecimalMax(value = "90", message = "Latitud no valida")
    private Double latitud;

    @NotNull(message = "Falta la longitud")
    @DecimalMin(value = "-180", message = "Longitud no valida")
    @DecimalMax(value = "180", message = "Longitud no valida")
    private Double longitud;
}
