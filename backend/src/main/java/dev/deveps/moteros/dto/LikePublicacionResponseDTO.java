package dev.deveps.moteros.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** "Me gusta" de un usuario sobre una publicacion (para listar quien ha dado like). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LikePublicacionResponseDTO {

    private String uuid;

    private String publicacionUuid;

    private UsuarioSummaryDTO usuario;

    private LocalDateTime fecha;
}
