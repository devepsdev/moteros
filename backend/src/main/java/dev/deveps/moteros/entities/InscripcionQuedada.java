package dev.deveps.moteros.entities;

import dev.deveps.moteros.entities.enums.EstadoInscripcion;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/** Tabla `inscripciones_quedadas`: usuarios apuntados a una quedada. */
@Entity
@Table(name = "inscripciones_quedadas",
        uniqueConstraints = @UniqueConstraint(name = "uq_inscripcion", columnNames = {"quedada_id", "usuario_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InscripcionQuedada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 36, updatable = false)
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quedada_id", nullable = false)
    private Quedada quedada;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EstadoInscripcion estado;

    @CreationTimestamp
    @Column(name = "fecha_inscripcion", updatable = false)
    private LocalDateTime fechaInscripcion;

    @PrePersist
    void prePersist() {
        if (uuid == null) uuid = UUID.randomUUID().toString();
        if (estado == null) estado = EstadoInscripcion.confirmado;
    }
}
