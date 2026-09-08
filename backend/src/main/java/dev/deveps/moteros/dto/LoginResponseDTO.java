package dev.deveps.moteros.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Respuesta al login: token de acceso y datos del usuario autenticado. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDTO {

    private String token;

    @Builder.Default
    private String type = "Bearer";

    private UsuarioResponseDTO usuario;
}
