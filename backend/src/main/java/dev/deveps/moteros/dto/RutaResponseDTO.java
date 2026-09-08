package dev.deveps.moteros.dto;

import dev.deveps.moteros.entities.enums.Dificultad;
import dev.deveps.moteros.entities.enums.TipoTerreno;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Ruta completa: cabecera, geolocalizacion de inicio/fin, track de puntos y resumen de valoraciones. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RutaResponseDTO {

    private String uuid;

    private UsuarioSummaryDTO creador;

    private String nombre;

    private String descripcion;

    private String puntoInicio;

    private BigDecimal latitudInicio;

    private BigDecimal longitudInicio;

    private String puntoFin;

    private BigDecimal latitudFin;

    private BigDecimal longitudFin;

    private BigDecimal distanciaKm;

    private Integer duracionEstimadaMin;

    private Dificultad dificultad;

    private TipoTerreno tipoTerreno;

    private LocalDateTime fechaCreacion;

    private Double valoracionMedia;

    private Long numValoraciones;

    /** Waypoints ordenados por {@code orden}. Puede venir vacio en listados. */
    private List<PuntoRutaResponseDTO> puntos;
}
