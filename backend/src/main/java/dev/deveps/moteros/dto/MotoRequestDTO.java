package dev.deveps.moteros.dto;

import dev.deveps.moteros.entities.enums.TipoMoto;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Alta o edicion de una moto. El propietario se toma del usuario autenticado,
 * no viaja en el cuerpo.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MotoRequestDTO {

    @NotBlank(message = "La marca es obligatoria")
    @Size(max = 50, message = "La marca no puede superar los 50 caracteres")
    private String marca;

    @NotBlank(message = "El modelo es obligatorio")
    @Size(max = 60, message = "El modelo no puede superar los 60 caracteres")
    private String modelo;

    @Min(value = 1885, message = "El anio no es valido")
    @Max(value = 2100, message = "El anio no es valido")
    private Short anio;

    @Positive(message = "La cilindrada ha de ser mayor que 0")
    private Integer cilindradaCc;

    /** Si es null, el servicio aplica el valor por defecto {@code naked}. */
    private TipoMoto tipo;

    @Size(max = 255, message = "La URL de la foto no puede superar los 255 caracteres")
    private String fotoUrl;
}
