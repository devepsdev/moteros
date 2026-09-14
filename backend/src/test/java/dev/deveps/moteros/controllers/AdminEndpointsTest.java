package dev.deveps.moteros.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminEndpointsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void estadisticas_sinAutenticacion_403() throws Exception {
        mockMvc.perform(get("/api/admin/estadisticas"))
                .andExpect(status().isForbidden());
    }

    @Test
    void estadisticas_conRolUser_403() throws Exception {
        mockMvc.perform(get("/api/admin/estadisticas").with(user("motero").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void estadisticas_conRolAdmin_200YEstructura() throws Exception {
        mockMvc.perform(get("/api/admin/estadisticas").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.usuariosTotales").exists())
                .andExpect(jsonPath("$.data.rutasPorDificultad").exists())
                .andExpect(jsonPath("$.data.topRutasPorValoracion").isArray());
    }

    private static final String SUGERENCIA = """
            {"urlFuente":"https://rutas.example/x","nombre":"Ruta","puntoInicio":"A","puntoFin":"B"}
            """;

    @Test
    void sugerenciasRuta_conRolUser_403() throws Exception {
        mockMvc.perform(post("/api/sugerencias-ruta").with(user("motero").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content(SUGERENCIA))
                .andExpect(status().isForbidden());
    }

    @Test
    void sugerenciasRuta_conRolScraper_pasaLaSeguridad() throws Exception {
        // El usuario del token no existe en la BBDD de test: el servicio responde 404, no 403.
        mockMvc.perform(post("/api/sugerencias-ruta").with(user("bot@test.com").roles("SCRAPER"))
                        .contentType(MediaType.APPLICATION_JSON).content(SUGERENCIA))
                .andExpect(status().isNotFound());
    }

    @Test
    void bandejaSugerencias_conRolScraper_403() throws Exception {
        mockMvc.perform(get("/api/admin/sugerencias-ruta").with(user("bot").roles("SCRAPER")))
                .andExpect(status().isForbidden());
    }
}
