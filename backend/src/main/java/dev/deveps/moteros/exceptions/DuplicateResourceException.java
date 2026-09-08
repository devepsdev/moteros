package dev.deveps.moteros.exceptions;

/** Conflicto: el recurso ya existe (email, nombre de usuario, etc.). Se traduce a HTTP 409. */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
