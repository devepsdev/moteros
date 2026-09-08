package dev.deveps.moteros.entities;

import dev.deveps.moteros.entities.enums.EstadoQuedada;
import dev.deveps.moteros.entities.enums.NivelRecomendado;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Tabla `quedadas`: eventos/encuentros moteros con geolocalizacion del punto de encuentro. */
@Entity
@Table(name = "quedadas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quedada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 36, updatable = false)
    private String uuid;

    /** Opcional: la quedada puede no estar asociada a una ruta (ON DELETE SET NULL). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ruta_id")
    private Ruta ruta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizador_id", nullable = false)
    private Usuario organizador;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "punto_encuentro", nullable = false, length = 150)
    private String puntoEncuentro;

    @Column(name = "latitud_encuentro", precision = 10, scale = 7)
    private BigDecimal latitudEncuentro;

    @Column(name = "longitud_encuentro", precision = 10, scale = 7)
    private BigDecimal longitudEncuentro;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "max_participantes")
    private Integer maxParticipantes;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_recomendado", length = 20)
    private NivelRecomendado nivelRecomendado;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EstadoQuedada estado;

    @CreationTimestamp
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    void prePersist() {
        if (uuid == null) uuid = UUID.randomUUID().toString();
        if (maxParticipantes == null) maxParticipantes = 20;
        if (nivelRecomendado == null) nivelRecomendado = NivelRecomendado.cualquiera;
        if (estado == null) estado = EstadoQuedada.programada;
    }
}
