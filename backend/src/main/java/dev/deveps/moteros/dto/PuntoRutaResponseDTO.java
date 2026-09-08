package dev.deveps.moteros.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Waypoint de una ruta tal y como se devuelve al cliente para dibujar el track. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PuntoRutaResponseDTO {

    private String uuid;

    private Integer orden;

    private BigDecimal latitud;

    private BigDecimal longitud;

    private Integer altitudM;

    private String nombrePunto;
}
