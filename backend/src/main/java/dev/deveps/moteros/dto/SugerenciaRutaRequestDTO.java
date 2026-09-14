package dev.deveps.moteros.dto;

import dev.deveps.moteros.entities.enums.Dificultad;
import dev.deveps.moteros.entities.enums.TipoTerreno;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/** Ruta encontrada por el scraper en una web. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SugerenciaRutaRequestDTO {

    @NotBlank(message = "La URL de origen es obligatoria")
    @Size(max = 500, message = "La URL de origen no puede superar los 500 caracteres")
    @Pattern(regexp = "https?://.+", message = "La URL de origen ha de empezar por http:// o https://")
    private String urlFuente;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 120, message = "El nombre no puede superar los 120 caracteres")
    private String nombre;

    @Size(max = 5000, message = "La descripcion no puede superar los 5000 caracteres")
    private String descripcion;

    @NotBlank(message = "El punto de inicio es obligatorio")
    @Size(max = 120, message = "El punto de inicio no puede superar los 120 caracteres")
    private String puntoInicio;

    @NotBlank(message = "El punto de fin es obligatorio")
    @Size(max = 120, message = "El punto de fin no puede superar los 120 caracteres")
    private String puntoFin;

    @DecimalMin(value = "0.1", message = "La distancia ha de ser mayor que 0")
    @DecimalMax(value = "99999.9", message = "La distancia no es valida")
    private BigDecimal distanciaKm;

    @Positive(message = "La duracion estimada ha de ser mayor que 0")
    private Integer duracionEstimadaMin;

    private Dificultad dificultad;

    private TipoTerreno tipoTerreno;

    @Valid
    @Size(max = 60, message = "Como maximo 60 puntos de paso")
    private List<PuntoSugeridoDTO> puntos;
}
