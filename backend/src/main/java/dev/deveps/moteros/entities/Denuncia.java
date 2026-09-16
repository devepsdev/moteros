package dev.deveps.moteros.entities;

import dev.deveps.moteros.entities.enums.EstadoDenuncia;
import dev.deveps.moteros.entities.enums.MotivoDenuncia;
import dev.deveps.moteros.entities.enums.TipoDenuncia;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tabla `denuncias`: contenido denunciado por un usuario, con una copia de como estaba en el
 * momento de la denuncia. Se revisa desde el panel de administracion.
 */
@Entity
@Table(name = "denuncias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Denuncia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 36, updatable = false)
    private String uuid;

    /** Null si quien denuncio ha borrado su cuenta. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "denunciante_id")
    private Usuario denunciante;

    /** Autor del contenido denunciado; null si ha borrado su cuenta. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "denunciado_id")
    private Usuario denunciado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoDenuncia tipo;

    @Column(name = "referencia_uuid", nullable = false, length = 36)
    private String referenciaUuid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MotivoDenuncia motivo;

    @Column(length = 1000)
    private String descripcion;

    @Column(columnDefinition = "TEXT")
    private String contenido;

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoDenuncia estado;

    @Column(name = "contenido_eliminado", nullable = false)
    private boolean contenidoEliminado;

    @Column(name = "usuario_dado_de_baja", nullable = false)
    private boolean usuarioDadoDeBaja;

    @Column(name = "nota_resolucion", length = 500)
    private String notaResolucion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resuelta_por_id")
    private Usuario resueltaPor;

    @CreationTimestamp
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    @PrePersist
    void prePersist() {
        if (uuid == null) uuid = UUID.randomUUID().toString();
        if (estado == null) estado = EstadoDenuncia.pendiente;
    }
}
