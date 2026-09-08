package dev.deveps.moteros.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representacion ligera de un usuario para anidar en otros DTOs
 * (autor de una publicacion, creador de una ruta, remitente de un mensaje, etc.).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioSummaryDTO {

    private String uuid;

    private String nombreUsuario;

    private String nombreCompleto;

    private String fotoPerfilUrl;

    private String ciudad;
}
