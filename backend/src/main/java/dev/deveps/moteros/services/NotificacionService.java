package dev.deveps.moteros.services;

import dev.deveps.moteros.dto.NotificacionResponseDTO;
import dev.deveps.moteros.entities.Usuario;
import dev.deveps.moteros.entities.enums.TipoNotificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificacionService {

    Page<NotificacionResponseDTO> listar(boolean soloNoLeidas, Pageable pageable);

    long contarNoLeidas();

    void marcarLeida(String uuid);

    void marcarTodasLeidas();

    /**
     * Crea una notificacion para {@code destino}. No hace nada si {@code destino} y {@code origen}
     * son el mismo usuario. Uso interno de otros servicios.
     */
    void notificar(Usuario destino, TipoNotificacion tipo, Integer referenciaId, Usuario origen, String mensaje);
}
