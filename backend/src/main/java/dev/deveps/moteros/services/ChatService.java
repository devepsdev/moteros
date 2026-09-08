package dev.deveps.moteros.services;

import dev.deveps.moteros.dto.ConversacionResponseDTO;
import dev.deveps.moteros.dto.MensajeRequestDTO;
import dev.deveps.moteros.dto.MensajeResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ChatService {

    Page<ConversacionResponseDTO> misConversaciones(Pageable pageable);

    ConversacionResponseDTO obtenerConversacion(String conversacionUuid);

    /** Lista mensajes (mas recientes primero) y marca como leidos los recibidos por el usuario actual. */
    Page<MensajeResponseDTO> listarMensajes(String conversacionUuid, Pageable pageable);

    MensajeResponseDTO enviarMensajeAUsuario(String usuarioUuid, MensajeRequestDTO dto);

    MensajeResponseDTO enviarMensajeEnConversacion(String conversacionUuid, MensajeRequestDTO dto);

    void marcarLeidos(String conversacionUuid);

    long totalNoLeidos();
}
