package dev.deveps.moteros.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Lugar de paso de una ruta sugerida. Las coordenadas faltan si no se pudo geolocalizar. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PuntoSugeridoDTO {

    @NotBlank(message = "El nombre del punto es obligatorio")
    @Size(max = 100, message = "El nombre del punto no puede superar los 100 caracteres")
    private String nombre;

    @DecimalMin(value = "-90.0", message = "La latitud ha de estar entre -90 y 90")
    @DecimalMax(value = "90.0", message = "La latitud ha de estar entre -90 y 90")
    private Double latitud;

    @DecimalMin(value = "-180.0", message = "La longitud ha de estar entre -180 y 180")
    @DecimalMax(value = "180.0", message = "La longitud ha de estar entre -180 y 180")
    private Double longitud;
}
