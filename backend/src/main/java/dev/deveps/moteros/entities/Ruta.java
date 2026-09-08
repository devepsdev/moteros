package dev.deveps.moteros.entities;

import dev.deveps.moteros.entities.enums.Dificultad;
import dev.deveps.moteros.entities.enums.TipoTerreno;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Tabla `rutas`: rutas moteras con geolocalizacion de inicio y fin. */
@Entity
@Table(name = "rutas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ruta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 36, updatable = false)
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creador_id", nullable = false)
    private Usuario creador;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "punto_inicio", nullable = false, length = 120)
    private String puntoInicio;

    @Column(name = "latitud_inicio", precision = 10, scale = 7)
    private BigDecimal latitudInicio;

    @Column(name = "longitud_inicio", precision = 10, scale = 7)
    private BigDecimal longitudInicio;

    @Column(name = "punto_fin", nullable = false, length = 120)
    private String puntoFin;

    @Column(name = "latitud_fin", precision = 10, scale = 7)
    private BigDecimal latitudFin;

    @Column(name = "longitud_fin", precision = 10, scale = 7)
    private BigDecimal longitudFin;

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

    @CreationTimestamp
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    void prePersist() {
        if (uuid == null) uuid = UUID.randomUUID().toString();
        if (dificultad == null) dificultad = Dificultad.moderada;
        if (tipoTerreno == null) tipoTerreno = TipoTerreno.asfalto;
    }
}
