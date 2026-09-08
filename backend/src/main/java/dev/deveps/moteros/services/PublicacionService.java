package dev.deveps.moteros.services;

import dev.deveps.moteros.dto.ComentarioRequestDTO;
import dev.deveps.moteros.dto.ComentarioResponseDTO;
import dev.deveps.moteros.dto.LikePublicacionResponseDTO;
import dev.deveps.moteros.dto.PublicacionRequestDTO;
import dev.deveps.moteros.dto.PublicacionResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PublicacionService {

    Page<PublicacionResponseDTO> feed(Pageable pageable);

    Page<PublicacionResponseDTO> buscar(String texto, Pageable pageable);

    Page<PublicacionResponseDTO> listarPorUsuario(String usuarioUuid, Pageable pageable);

    PublicacionResponseDTO obtenerPorUuid(String uuid);

    PublicacionResponseDTO crear(PublicacionRequestDTO dto);

    PublicacionResponseDTO actualizar(String uuid, PublicacionRequestDTO dto);

    void eliminar(String uuid);

    // ===== COMENTARIOS =====

    Page<ComentarioResponseDTO> listarComentarios(String publicacionUuid, Pageable pageable);

    ComentarioResponseDTO comentar(String publicacionUuid, ComentarioRequestDTO dto);

    void eliminarComentario(String comentarioUuid);

    // ===== LIKES =====

    /** Alterna el "me gusta" del usuario actual. Devuelve true si queda con like. */
    boolean alternarLike(String publicacionUuid);

    Page<LikePublicacionResponseDTO> listarLikes(String publicacionUuid, Pageable pageable);
}
