package dev.deveps.moteros.dto;

import dev.deveps.moteros.entities.enums.TipoNotificacion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Notificacion dirigida al usuario autenticado. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificacionResponseDTO {

    private String uuid;

    private TipoNotificacion tipo;

    private String mensaje;

    /** ID del registro relacionado (publicacion, quedada, mensaje, etc.). Puede ser null. */
    private Integer referenciaId;

    /** Usuario que origina la notificacion. Puede ser null. */
    private UsuarioSummaryDTO usuarioOrigen;

    private Boolean leido;

    private LocalDateTime fechaCreacion;
}
