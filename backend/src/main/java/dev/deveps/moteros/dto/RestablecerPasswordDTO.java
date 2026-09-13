package dev.deveps.moteros.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Cuerpo de {@code POST /api/auth/restablecer-password}: email + codigo recibido + contrasena nueva. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestablecerPasswordDTO {

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email ha de ser valido")
    private String email;

    @NotBlank(message = "El codigo es obligatorio")
    @Pattern(regexp = "\\d{6}", message = "El codigo ha de tener 6 digitos")
    private String codigo;

    @NotBlank(message = "La nueva contrasena es obligatoria")
    @Size(min = 8, max = 72, message = "La nueva contrasena ha de tener entre 8 y 72 caracteres")
    private String passwordNueva;
}
