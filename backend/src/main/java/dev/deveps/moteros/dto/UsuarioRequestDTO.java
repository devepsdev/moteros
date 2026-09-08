package dev.deveps.moteros.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Datos editables del perfil del usuario autenticado.
 * El email y la contrasena se cambian por endpoints dedicados.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioRequestDTO {

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(max = 50, message = "El nombre de usuario no puede superar los 50 caracteres")
    private String nombreUsuario;

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 100, message = "El nombre completo no puede superar los 100 caracteres")
    private String nombreCompleto;

    @Size(max = 80, message = "La ciudad no puede superar los 80 caracteres")
    private String ciudad;

    @Size(max = 280, message = "La biografia no puede superar los 280 caracteres")
    private String biografia;

    @Size(max = 255, message = "La URL de la foto de perfil no puede superar los 255 caracteres")
    private String fotoPerfilUrl;
}
