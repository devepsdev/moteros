package dev.deveps.moteros.services;

import dev.deveps.moteros.dto.AmistadResponseDTO;
import dev.deveps.moteros.dto.UsuarioSummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AmistadService {

    AmistadResponseDTO enviarSolicitud(String usuarioUuid);

    AmistadResponseDTO responder(String amistadUuid, boolean aceptar);

    /** Elimina la amistad o cancela la solicitud con el usuario indicado (en cualquier direccion). */
    void eliminar(String usuarioUuid);

    Page<UsuarioSummaryDTO> listarAmigos(String usuarioUuid, Pageable pageable);

    Page<AmistadResponseDTO> solicitudesRecibidas(Pageable pageable);

    Page<AmistadResponseDTO> solicitudesEnviadas(Pageable pageable);

    /** Relacion con otro usuario, o null si no existe ninguna. */
    AmistadResponseDTO relacionCon(String usuarioUuid);
}
