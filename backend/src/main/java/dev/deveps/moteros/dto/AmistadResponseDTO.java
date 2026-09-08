package dev.deveps.moteros.dto;

import dev.deveps.moteros.entities.enums.EstadoAmistad;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Relacion de amistad / solicitud entre dos usuarios.
 * {@code solicitante} envio la peticion, {@code destinatario} la recibe.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmistadResponseDTO {

    private String uuid;

    private UsuarioSummaryDTO solicitante;

    private UsuarioSummaryDTO destinatario;

    private EstadoAmistad estado;

    private LocalDateTime fecha;

    /** true si la solicitud la envio el usuario que consulta. */
    private Boolean enviadaPorMi;
}
