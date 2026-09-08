package dev.deveps.moteros.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Envio de un mensaje privado. El destinatario o la conversacion llegan por la URL;
 * el remitente se toma del usuario autenticado.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MensajeRequestDTO {

    @NotBlank(message = "El contenido del mensaje es obligatorio")
    @Size(max = 5000, message = "El mensaje no puede superar los 5000 caracteres")
    private String contenido;
}
