package dev.deveps.moteros.security;

import dev.deveps.moteros.entities.Usuario;
import dev.deveps.moteros.exceptions.ResourceNotFoundException;
import dev.deveps.moteros.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Acceso al usuario autenticado (el que porta el JWT) desde la capa de servicio.
 * El subject del token es el email del usuario.
 */
@Component
@RequiredArgsConstructor
public class UsuarioAutenticadoProvider {

    private final UsuarioRepository usuarioRepository;

    /** Usuario autenticado como entidad gestionada. */
    public Usuario obtenerUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            throw new ResourceNotFoundException("No hay usuario autenticado");
        }
        return usuarioRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario autenticado no encontrado: " + auth.getName()));
    }

    public Integer obtenerIdUsuarioActual() {
        return obtenerUsuarioActual().getId();
    }

    public String obtenerUuidUsuarioActual() {
        return obtenerUsuarioActual().getUuid();
    }
}
