package dev.deveps.moteros.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Credenciales de inicio de sesion. {@code identificador} acepta email o nombre de usuario. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequestDTO {

    @NotBlank(message = "El email o nombre de usuario es obligatorio")
    private String identificador;

    @NotBlank(message = "La contrasena es obligatoria")
    private String password;
}
