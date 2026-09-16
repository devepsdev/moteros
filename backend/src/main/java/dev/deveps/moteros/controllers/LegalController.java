package dev.deveps.moteros.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Paginas legales publicas (las enlaza la ficha de Google Play y la app). Son HTML estatico
 * en {@code static/legal}; aqui solo se les da una URL corta.
 */
@Controller
public class LegalController {

    @GetMapping("/privacidad")
    public String privacidad() {
        return "forward:/legal/privacidad.html";
    }

    @GetMapping("/terminos")
    public String terminos() {
        return "forward:/legal/terminos.html";
    }

    @GetMapping("/eliminar-cuenta")
    public String eliminarCuenta() {
        return "forward:/legal/eliminar-cuenta.html";
    }
}
