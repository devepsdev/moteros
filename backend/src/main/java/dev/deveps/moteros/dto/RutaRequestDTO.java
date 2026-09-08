package dev.deveps.moteros.dto;

import dev.deveps.moteros.entities.enums.Dificultad;
import dev.deveps.moteros.entities.enums.TipoTerreno;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Alta o edicion de una ruta. El creador se toma del usuario autenticado.
 * Opcionalmente incluye el track completo en {@code puntos}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RutaRequestDTO {

    @NotBlank(message = "El nombre de la ruta es obligatorio")
    @Size(max = 120, message = "El nombre no puede superar los 120 caracteres")
    private String nombre;

    @Size(max = 5000, message = "La descripcion no puede superar los 5000 caracteres")
    private String descripcion;

    @NotBlank(message = "El punto de inicio es obligatorio")
    @Size(max = 120, message = "El punto de inicio no puede superar los 120 caracteres")
    private String puntoInicio;

    @DecimalMin(value = "-90.0", message = "La latitud de inicio ha de estar entre -90 y 90")
    @DecimalMax(value = "90.0", message = "La latitud de inicio ha de estar entre -90 y 90")
    @Digits(integer = 3, fraction = 7, message = "Latitud de inicio con formato invalido")
    private BigDecimal latitudInicio;

    @DecimalMin(value = "-180.0", message = "La longitud de inicio ha de estar entre -180 y 180")
    @DecimalMax(value = "180.0", message = "La longitud de inicio ha de estar entre -180 y 180")
    @Digits(integer = 3, fraction = 7, message = "Longitud de inicio con formato invalido")
    private BigDecimal longitudInicio;

    @NotBlank(message = "El punto de fin es obligatorio")
    @Size(max = 120, message = "El punto de fin no puede superar los 120 caracteres")
    private String puntoFin;

    @DecimalMin(value = "-90.0", message = "La latitud de fin ha de estar entre -90 y 90")
    @DecimalMax(value = "90.0", message = "La latitud de fin ha de estar entre -90 y 90")
    @Digits(integer = 3, fraction = 7, message = "Latitud de fin con formato invalido")
    private BigDecimal latitudFin;

    @DecimalMin(value = "-180.0", message = "La longitud de fin ha de estar entre -180 y 180")
    @DecimalMax(value = "180.0", message = "La longitud de fin ha de estar entre -180 y 180")
    @Digits(integer = 3, fraction = 7, message = "Longitud de fin con formato invalido")
    private BigDecimal longitudFin;

    @PositiveOrZero(message = "La distancia no puede ser negativa")
    @Digits(integer = 5, fraction = 1, message = "La distancia ha de tener como maximo 5 digitos enteros y 1 decimal")
    private BigDecimal distanciaKm;

    @Positive(message = "La duracion estimada ha de ser mayor que 0")
    private Integer duracionEstimadaMin;

    /** Si es null, el servicio aplica el valor por defecto {@code moderada}. */
    private Dificultad dificultad;

    /** Si es null, el servicio aplica el valor por defecto {@code asfalto}. */
    private TipoTerreno tipoTerreno;

    @Valid
    private List<PuntoRutaRequestDTO> puntos;
}
