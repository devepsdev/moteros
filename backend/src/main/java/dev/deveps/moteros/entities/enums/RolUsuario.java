package dev.deveps.moteros.entities.enums;

/** Valores del ENUM `rol` de la tabla `usuarios`. */
public enum RolUsuario {
    user, admin,
    /** Cuenta del scraper: permisos de usuario, pero puede enviar sugerencias de rutas. */
    scraper,
    /** Cuenta de la propia app (firma el catalogo): no sale en el buscador de moteros. */
    oficial
}
