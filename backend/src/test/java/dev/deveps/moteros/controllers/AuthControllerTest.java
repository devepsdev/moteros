package dev.deveps.moteros.controllers;

import dev.deveps.moteros.dto.LoginResponseDTO;
import dev.deveps.moteros.dto.UsuarioResponseDTO;
import dev.deveps.moteros.exceptions.GlobalExceptionHandler;
import dev.deveps.moteros.services.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private MockMvc mockMvc;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = Mockito.mock(AuthService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void login_devuelve200YToken() throws Exception {
        LoginResponseDTO res = LoginResponseDTO.builder()
                .token("jwt.aqui")
                .type("Bearer")
                .usuario(UsuarioResponseDTO.builder().email("motero@test.com").nombreUsuario("motero").build())
                .build();
        when(authService.login(any())).thenReturn(res);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificador\":\"motero\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("jwt.aqui"))
                .andExpect(jsonPath("$.data.usuario.email").value("motero@test.com"));
    }

    @Test
    void registro_devuelve201() throws Exception {
        when(authService.registro(any())).thenReturn(LoginResponseDTO.builder().token("t").build());

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\":\"nuevo\",\"nombreCompleto\":\"Nuevo\","
                                + "\"email\":\"nuevo@test.com\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.token").value("t"));
    }

    @Test
    void registro_emailInvalido_devuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\":\"nuevo\",\"nombreCompleto\":\"Nuevo\","
                                + "\"email\":\"no-es-email\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void refresh_devuelve200YNuevoPar() throws Exception {
        when(authService.refrescar(any())).thenReturn(LoginResponseDTO.builder()
                .token("nuevo.jwt").refreshToken("nuevo.refresh").build());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"rt-viejo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("nuevo.jwt"))
                .andExpect(jsonPath("$.data.refreshToken").value("nuevo.refresh"));
    }

    @Test
    void refresh_sinRefreshToken_devuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logout_devuelve200() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"rt-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void cambiarPassword_devuelve200() throws Exception {
        mockMvc.perform(patch("/api/auth/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passwordActual\":\"password123\",\"passwordNueva\":\"password456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
