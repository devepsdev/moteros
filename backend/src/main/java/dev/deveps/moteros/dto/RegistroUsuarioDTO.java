package dev.deveps.moteros.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Alta de una cuenta nueva. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroUsuarioDTO {

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 3, max = 50, message = "El nombre de usuario ha de tener entre 3 y 50 caracteres")
    private String nombreUsuario;

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 100, message = "El nombre completo no puede superar los 100 caracteres")
    private String nombreCompleto;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email ha de ser valido")
    @Size(max = 120, message = "El email no puede superar los 120 caracteres")
    private String email;

    @NotBlank(message = "La contrasena es obligatoria")
    @Size(min = 8, max = 72, message = "La contrasena ha de tener entre 8 y 72 caracteres")
    private String password;

    @Size(max = 80, message = "La ciudad no puede superar los 80 caracteres")
    private String ciudad;
}
