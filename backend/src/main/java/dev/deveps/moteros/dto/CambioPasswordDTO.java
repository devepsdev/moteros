package dev.deveps.moteros.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Cambio de contrasena del usuario autenticado. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CambioPasswordDTO {

    @NotBlank(message = "La contrasena actual es obligatoria")
    private String passwordActual;

    @NotBlank(message = "La nueva contrasena es obligatoria")
    @Size(min = 8, max = 72, message = "La nueva contrasena ha de tener entre 8 y 72 caracteres")
    private String passwordNueva;
}
