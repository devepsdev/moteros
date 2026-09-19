package dev.deveps.moteros.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Recorrido por carretera calculado para la vista previa. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrazadoResponseDTO {

    /** Polilinea codificada (precision 5); null si no hay recorrido o no se ha podido calcular. */
    private String trazado;
    private Double distanciaKm;
    private Integer duracionMin;
}
