package dev.deveps.moteros.services;

import dev.deveps.moteros.dto.CambioPasswordDTO;
import dev.deveps.moteros.dto.LoginRequestDTO;
import dev.deveps.moteros.dto.LoginResponseDTO;
import dev.deveps.moteros.dto.RegistroUsuarioDTO;

public interface AuthService {

    LoginResponseDTO registro(RegistroUsuarioDTO dto);

    LoginResponseDTO login(LoginRequestDTO dto);

    void cambiarPassword(CambioPasswordDTO dto);
}
