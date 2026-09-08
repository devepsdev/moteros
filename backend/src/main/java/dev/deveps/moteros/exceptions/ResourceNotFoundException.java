package dev.deveps.moteros.exceptions;

/** Recurso no encontrado. Se traduce a HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
