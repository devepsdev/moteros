package dev.deveps.moteros.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/** Publicacion del muro con autor, ruta compartida y contadores sociales. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicacionResponseDTO {

    private String uuid;

    private UsuarioSummaryDTO autor;

    /** Ruta compartida. Puede ser null. */
    private RutaSummaryDTO ruta;

    private String contenido;

    private String imagenUrl;

    private LocalDateTime fechaPublicacion;

    // ===== CALCULADOS =====
    private Long numLikes;

    private Long numComentarios;

    /** true si el usuario que consulta ha dado like a esta publicacion. */
    private Boolean likeUsuarioActual;

    /** Comentarios (p.ej. los ultimos N). Puede venir vacia en el feed. */
    private List<ComentarioResponseDTO> comentarios;
}
