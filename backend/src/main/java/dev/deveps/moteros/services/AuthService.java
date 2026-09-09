package dev.deveps.moteros.services;

import dev.deveps.moteros.dto.CambioPasswordDTO;
import dev.deveps.moteros.dto.LoginRequestDTO;
import dev.deveps.moteros.dto.LoginResponseDTO;
import dev.deveps.moteros.dto.RefreshTokenRequestDTO;
import dev.deveps.moteros.dto.RegistroUsuarioDTO;

public interface AuthService {

    LoginResponseDTO registro(RegistroUsuarioDTO dto);

    LoginResponseDTO login(LoginRequestDTO dto);

    /** Rota el refresh token y devuelve un nuevo par access + refresh. */
    LoginResponseDTO refrescar(RefreshTokenRequestDTO dto);

    /** Revoca el refresh token indicado (logout de este dispositivo). */
    void logout(RefreshTokenRequestDTO dto);

    /** Revoca todos los refresh tokens del usuario autenticado. */
    void logoutTodos();

    void cambiarPassword(CambioPasswordDTO dto);
}
