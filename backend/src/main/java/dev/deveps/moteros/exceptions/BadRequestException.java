package dev.deveps.moteros.exceptions;

/** Peticion invalida por reglas de negocio. Se traduce a HTTP 400. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
