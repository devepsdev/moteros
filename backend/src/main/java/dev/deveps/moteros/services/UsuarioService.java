package dev.deveps.moteros.services;

import dev.deveps.moteros.dto.UsuarioRequestDTO;
import dev.deveps.moteros.dto.UsuarioResponseDTO;
import dev.deveps.moteros.dto.UsuarioSummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UsuarioService {

    /** Perfil completo del usuario autenticado (incluye email y contadores). */
    UsuarioResponseDTO obtenerPerfilActual();

    /** Perfil publico de un usuario por su UUID (incluye contadores). */
    UsuarioResponseDTO obtenerPorUuid(String uuid);

    /** Busqueda paginada de usuarios por texto. */
    Page<UsuarioSummaryDTO> buscar(String texto, Pageable pageable);

    /** Actualiza el perfil del usuario autenticado. */
    UsuarioResponseDTO actualizarPerfil(UsuarioRequestDTO dto);

    /**
     * Elimina la cuenta del usuario autenticado y todos sus datos (requisito de Google Play).
     * La BBDD borra en cascada motos, rutas, quedadas, publicaciones, comentarios, likes,
     * amistades, conversaciones, mensajes, notificaciones y tokens; aqui se borran ademas
     * las imagenes subidas del disco.
     */
    void eliminarCuentaActual();
}
