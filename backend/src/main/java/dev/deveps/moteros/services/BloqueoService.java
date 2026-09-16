package dev.deveps.moteros.services;

import dev.deveps.moteros.dto.UsuarioSummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BloqueoService {

    /** Bloquea al usuario y deshace la amistad o solicitud que hubiera. Bloquear dos veces no falla. */
    void bloquear(String usuarioUuid);

    void desbloquear(String usuarioUuid);

    Page<UsuarioSummaryDTO> misBloqueados(Pageable pageable);

    /** Si el usuario actual ha bloqueado al indicado (no si el otro le ha bloqueado a el). */
    boolean heBloqueado(String usuarioUuid);

    /** Lanza ResourceNotFoundException si hay un bloqueo, en cualquier sentido, con el usuario actual. */
    void exigirSinBloqueo(Integer otroUsuarioId);
}
