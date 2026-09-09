package dev.deveps.moteros.controllers;

import dev.deveps.moteros.dto.ApiResponseDTO;
import dev.deveps.moteros.dto.CambioPasswordDTO;
import dev.deveps.moteros.dto.LoginRequestDTO;
import dev.deveps.moteros.dto.LoginResponseDTO;
import dev.deveps.moteros.dto.RefreshTokenRequestDTO;
import dev.deveps.moteros.dto.RegistroUsuarioDTO;
import dev.deveps.moteros.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/registro")
    public ResponseEntity<ApiResponseDTO<LoginResponseDTO>> registro(
            @Valid @RequestBody RegistroUsuarioDTO dto) {
        LoginResponseDTO res = authService.registro(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(res, "Cuenta creada correctamente"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponseDTO<LoginResponseDTO>> login(
            @Valid @RequestBody LoginRequestDTO dto) {
        LoginResponseDTO res = authService.login(dto);
        return ResponseEntity.ok(ApiResponseDTO.success(res, "Sesion iniciada correctamente"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponseDTO<LoginResponseDTO>> refrescar(
            @Valid @RequestBody RefreshTokenRequestDTO dto) {
        return ResponseEntity.ok(ApiResponseDTO.success(
                authService.refrescar(dto), "Sesion renovada correctamente"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponseDTO<Void>> logout(@Valid @RequestBody RefreshTokenRequestDTO dto) {
        authService.logout(dto);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Sesion cerrada correctamente"));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponseDTO<Void>> logoutTodos() {
        authService.logoutTodos();
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Todas las sesiones cerradas correctamente"));
    }

    @PatchMapping("/password")
    public ResponseEntity<ApiResponseDTO<Void>> cambiarPassword(
            @Valid @RequestBody CambioPasswordDTO dto) {
        authService.cambiarPassword(dto);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Contrasena actualizada correctamente"));
    }
}
