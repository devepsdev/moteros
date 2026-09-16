package dev.deveps.moteros.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Las paginas legales han de verse sin iniciar sesion: las enlaza la ficha de Google Play. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LegalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void paginasLegales_sinAutenticacion_200() throws Exception {
        mockMvc.perform(get("/privacidad")).andExpect(status().isOk()).andExpect(forwardedUrl("/legal/privacidad.html"));
        mockMvc.perform(get("/terminos")).andExpect(status().isOk()).andExpect(forwardedUrl("/legal/terminos.html"));
        mockMvc.perform(get("/eliminar-cuenta")).andExpect(status().isOk()).andExpect(forwardedUrl("/legal/eliminar-cuenta.html"));
    }

    @Test
    void estilosLegales_sinAutenticacion_200() throws Exception {
        mockMvc.perform(get("/legal/legal.css")).andExpect(status().isOk());
    }
}
