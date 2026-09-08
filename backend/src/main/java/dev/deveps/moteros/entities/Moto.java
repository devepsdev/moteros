package dev.deveps.moteros.entities;

import dev.deveps.moteros.entities.enums.TipoMoto;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/** Tabla `motos`: motos que posee cada usuario. */
@Entity
@Table(name = "motos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Moto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 36, updatable = false)
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 50)
    private String marca;

    @Column(nullable = false, length = 60)
    private String modelo;

    @Column(name = "anio")
    private Short anio;

    @Column(name = "cilindrada_cc")
    private Integer cilindradaCc;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TipoMoto tipo;

    @Column(name = "foto_url", length = 255)
    private String fotoUrl;

    @PrePersist
    void prePersist() {
        if (uuid == null) uuid = UUID.randomUUID().toString();
        if (tipo == null) tipo = TipoMoto.naked;
    }
}
