package dev.deveps.moteros.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Cuerpo de {@code POST /api/auth/recuperar-password}: pide el codigo por email. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecuperarPasswordDTO {

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email ha de ser valido")
    private String email;
}
