package dev.deveps.moteros.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Waypoint de una ruta. Se envia dentro de {@code RutaRequestDTO.puntos}. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PuntoRutaRequestDTO {

    @NotNull(message = "El orden del punto es obligatorio")
    @PositiveOrZero(message = "El orden ha de ser 0 o superior")
    private Integer orden;

    @NotNull(message = "La latitud es obligatoria")
    @DecimalMin(value = "-90.0", message = "La latitud ha de estar entre -90 y 90")
    @DecimalMax(value = "90.0", message = "La latitud ha de estar entre -90 y 90")
    @Digits(integer = 3, fraction = 7, message = "Latitud con formato invalido")
    private BigDecimal latitud;

    @NotNull(message = "La longitud es obligatoria")
    @DecimalMin(value = "-180.0", message = "La longitud ha de estar entre -180 y 180")
    @DecimalMax(value = "180.0", message = "La longitud ha de estar entre -180 y 180")
    @Digits(integer = 3, fraction = 7, message = "Longitud con formato invalido")
    private BigDecimal longitud;

    private Integer altitudM;

    @Size(max = 100, message = "El nombre del punto no puede superar los 100 caracteres")
    private String nombrePunto;
}
