package dev.deveps.moteros.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Token que da Expo en el movil para recibir avisos push. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispositivoPushRequestDTO {

    @NotBlank(message = "El token del dispositivo es obligatorio")
    @Size(max = 255, message = "El token no puede superar los 255 caracteres")
    private String token;

    @Size(max = 20, message = "La plataforma no puede superar los 20 caracteres")
    private String plataforma;
}
