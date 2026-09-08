package dev.deveps.moteros.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Hilo de mensajeria privada, visto desde el usuario autenticado. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversacionResponseDTO {

    private String uuid;

    /** El otro participante de la conversacion (relativo al usuario autenticado). */
    private UsuarioSummaryDTO interlocutor;

    /** Ultimo mensaje del hilo, para la vista de lista de chats. Puede ser null. */
    private MensajeResponseDTO ultimoMensaje;

    private Long numNoLeidos;

    private LocalDateTime fechaCreacion;
}
