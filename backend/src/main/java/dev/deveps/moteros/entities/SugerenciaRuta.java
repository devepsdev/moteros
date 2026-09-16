package dev.deveps.moteros.entities;

import dev.deveps.moteros.entities.enums.Dificultad;
import dev.deveps.moteros.entities.enums.EstadoSugerencia;
import dev.deveps.moteros.entities.enums.TipoTerreno;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tabla `sugerencias_ruta`: rutas que el scraper encuentra en webs y deja pendientes de
 * revision. No forman parte del catalogo hasta que un administrador crea la ruta.
 */
@Entity
@Table(name = "sugerencias_ruta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SugerenciaRuta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 36, updatable = false)
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "url_fuente", nullable = false, length = 500)
    private String urlFuente;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "punto_inicio", nullable = false, length = 120)
    private String puntoInicio;

    @Column(name = "punto_fin", nullable = false, length = 120)
    private String puntoFin;

    @Column(name = "distancia_km", precision = 6, scale = 1)
    private BigDecimal distanciaKm;

    @Column(name = "duracion_estimada_min")
    private Integer duracionEstimadaMin;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Dificultad dificultad;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_terreno", length = 20)
    private TipoTerreno tipoTerreno;

    /** Lugares de paso en orden, serializados como JSON (ver SugerenciaRutaServiceImpl). */
    @Column(name = "puntos_json", columnDefinition = "TEXT")
    private String puntosJson;

    /** Enlaces de la página de origen al recorrido exacto, serializados como JSON. */
    @Column(name = "enlaces_track_json", columnDefinition = "TEXT")
    private String enlacesTrackJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSugerencia estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ruta_id")
    private Ruta ruta;

    @Column(name = "motivo_rechazo", length = 255)
    private String motivoRechazo;

    @CreationTimestamp
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @PrePersist
    void prePersist() {
        if (uuid == null) uuid = UUID.randomUUID().toString();
        if (estado == null) estado = EstadoSugerencia.pendiente;
    }
}
