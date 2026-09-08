package dev.deveps.moteros.dto;

import dev.deveps.moteros.entities.enums.Dificultad;
import dev.deveps.moteros.entities.enums.TipoTerreno;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Representacion ligera de una ruta para listados, feed y para anidar en quedadas/publicaciones. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RutaSummaryDTO {

    private String uuid;

    private String nombre;

    private UsuarioSummaryDTO creador;

    private String puntoInicio;

    private String puntoFin;

    private BigDecimal distanciaKm;

    private Integer duracionEstimadaMin;

    private Dificultad dificultad;

    private TipoTerreno tipoTerreno;

    private Double valoracionMedia;

    private Long numValoraciones;
}
