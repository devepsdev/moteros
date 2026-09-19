package dev.deveps.moteros.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Dos puntos seguidos de una ruta entre los que buscar carreteras alternativas. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlternativasRequestDTO {

    @NotNull(message = "Falta el punto de salida del tramo")
    @Valid
    private CoordenadaDTO origen;

    @NotNull(message = "Falta el punto de llegada del tramo")
    @Valid
    private CoordenadaDTO destino;
}
