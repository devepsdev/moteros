package dev.deveps.moteros.entities;

import dev.deveps.moteros.entities.enums.EstadoAmistad;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/** Tabla `amistades`: solicitudes y relaciones de amistad entre usuarios. */
@Entity
@Table(name = "amistades",
        uniqueConstraints = @UniqueConstraint(name = "uq_amistad", columnNames = {"usuario_id", "amigo_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Amistad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 36, updatable = false)
    private String uuid;

    /** Usuario que envia/posee la solicitud. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /** Usuario destinatario de la solicitud. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "amigo_id", nullable = false)
    private Usuario amigo;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EstadoAmistad estado;

    @CreationTimestamp
    @Column(name = "fecha", updatable = false)
    private LocalDateTime fecha;

    @PrePersist
    void prePersist() {
        if (uuid == null) uuid = UUID.randomUUID().toString();
        if (estado == null) estado = EstadoAmistad.pendiente;
    }
}
