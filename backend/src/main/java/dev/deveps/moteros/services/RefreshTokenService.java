package dev.deveps.moteros.services;

import dev.deveps.moteros.entities.RefreshToken;
import dev.deveps.moteros.entities.Usuario;

public interface RefreshTokenService {

    /** Crea y persiste un refresh token nuevo para el usuario. */
    RefreshToken crear(Usuario usuario);

    /**
     * Valida el refresh token recibido y lo rota: revoca el actual y devuelve uno nuevo.
     * @throws dev.deveps.moteros.exceptions.BadRequestException si no existe, esta revocado o ha caducado
     */
    RefreshToken validarYRotar(String token);

    /** Revoca un refresh token concreto (logout). No falla si no existe. */
    void revocar(String token);

    /** Revoca todos los refresh tokens activos del usuario (logout en todos los dispositivos). */
    void revocarTodos(Integer usuarioId);
}
