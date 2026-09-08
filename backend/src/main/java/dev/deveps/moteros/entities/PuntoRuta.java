package dev.deveps.moteros.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/** Tabla `puntos_ruta`: waypoints ordenados de una ruta para dibujar el recorrido. */
@Entity
@Table(name = "puntos_ruta",
        uniqueConstraints = @UniqueConstraint(name = "uq_punto_orden", columnNames = {"ruta_id", "orden"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PuntoRuta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 36, updatable = false)
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ruta_id", nullable = false)
    private Ruta ruta;

    @Column(nullable = false)
    private Integer orden;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitud;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitud;

    @Column(name = "altitud_m")
    private Integer altitudM;

    @Column(name = "nombre_punto", length = 100)
    private String nombrePunto;

    @PrePersist
    void prePersist() {
        if (uuid == null) uuid = UUID.randomUUID().toString();
    }
}
