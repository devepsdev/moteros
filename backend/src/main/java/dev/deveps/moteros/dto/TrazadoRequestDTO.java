package dev.deveps.moteros.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Puntos de paso (en orden) para ver el recorrido por carretera mientras se dibuja una ruta. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrazadoRequestDTO {

    @NotNull(message = "Faltan los puntos")
    @Size(min = 2, max = 100, message = "El recorrido ha de tener entre 2 y 100 puntos")
    private List<@Valid @NotNull CoordenadaDTO> puntos;
}
