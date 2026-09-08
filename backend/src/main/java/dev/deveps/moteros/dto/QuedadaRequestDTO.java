package dev.deveps.moteros.dto;

import dev.deveps.moteros.entities.enums.NivelRecomendado;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Alta o edicion de una quedada. El organizador se toma del usuario autenticado.
 * El {@code estado} se gestiona con endpoints dedicados (cancelar / finalizar).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuedadaRequestDTO {

    /** UUID de la ruta asociada. Opcional. */
    private String rutaUuid;

    @NotBlank(message = "El titulo es obligatorio")
    @Size(max = 120, message = "El titulo no puede superar los 120 caracteres")
    private String titulo;

    @Size(max = 5000, message = "La descripcion no puede superar los 5000 caracteres")
    private String descripcion;

    @NotBlank(message = "El punto de encuentro es obligatorio")
    @Size(max = 150, message = "El punto de encuentro no puede superar los 150 caracteres")
    private String puntoEncuentro;

    @DecimalMin(value = "-90.0", message = "La latitud ha de estar entre -90 y 90")
    @DecimalMax(value = "90.0", message = "La latitud ha de estar entre -90 y 90")
    @Digits(integer = 3, fraction = 7, message = "Latitud con formato invalido")
    private BigDecimal latitudEncuentro;

    @DecimalMin(value = "-180.0", message = "La longitud ha de estar entre -180 y 180")
    @DecimalMax(value = "180.0", message = "La longitud ha de estar entre -180 y 180")
    @Digits(integer = 3, fraction = 7, message = "Longitud con formato invalido")
    private BigDecimal longitudEncuentro;

    @NotNull(message = "La fecha y hora son obligatorias")
    @Future(message = "La fecha de la quedada ha de ser futura")
    private LocalDateTime fechaHora;

    @Positive(message = "El maximo de participantes ha de ser mayor que 0")
    private Integer maxParticipantes;

    /** Si es null, el servicio aplica el valor por defecto {@code cualquiera}. */
    private NivelRecomendado nivelRecomendado;
}
