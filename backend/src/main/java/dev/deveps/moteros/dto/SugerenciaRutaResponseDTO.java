package dev.deveps.moteros.dto;

import dev.deveps.moteros.entities.enums.Dificultad;
import dev.deveps.moteros.entities.enums.EstadoSugerencia;
import dev.deveps.moteros.entities.enums.TipoTerreno;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Sugerencia de ruta para la bandeja de revision del panel. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SugerenciaRutaResponseDTO {

    private String uuid;
    private UsuarioSummaryDTO autor;
    private String urlFuente;
    private String nombre;
    private String descripcion;
    private String puntoInicio;
    private String puntoFin;
    private BigDecimal distanciaKm;
    private Integer duracionEstimadaMin;
    private Dificultad dificultad;
    private TipoTerreno tipoTerreno;
    private List<PuntoSugeridoDTO> puntos;
    /** Enlaces de la fuente al recorrido exacto (GPX, KML) para descargarlo e importarlo en el panel. */
    private List<EnlaceTrackDTO> enlacesTrack;
    private EstadoSugerencia estado;
    /** Ruta creada a partir de la sugerencia (solo si esta aprobada y la ruta sigue existiendo). */
    private String rutaUuid;
    private String motivoRechazo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}
