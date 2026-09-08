package dev.deveps.moteros.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Mensaje dentro de una conversacion privada. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MensajeResponseDTO {

    private String uuid;

    private String conversacionUuid;

    private UsuarioSummaryDTO remitente;

    private String contenido;

    private Boolean leido;

    private LocalDateTime fechaEnvio;

    /** true si el remitente es el usuario que consulta. */
    private Boolean propio;
}
