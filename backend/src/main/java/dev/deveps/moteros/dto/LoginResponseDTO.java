package dev.deveps.moteros.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Respuesta al login / registro / refresh: access token, refresh token y datos del usuario. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDTO {

    /** Access token (JWT) de corta duracion. */
    private String token;

    @Builder.Default
    private String type = "Bearer";

    /** Segundos de validez del access token. */
    private Long expiresIn;

    /** Refresh token opaco de larga duracion; se usa en POST /api/auth/refresh. */
    private String refreshToken;

    private UsuarioResponseDTO usuario;
}
