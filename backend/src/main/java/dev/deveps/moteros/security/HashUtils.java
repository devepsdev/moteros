package dev.deveps.moteros.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * SHA-256 sin sal para valores generados por nosotros (codigos de un solo uso)
 * que no son contrasenas elegidas por una persona: no hace falta un hash lento con
 * sal como BCrypt, basta con no guardarlos en claro por si la base de datos se filtra.
 */
public final class HashUtils {

    private HashUtils() {
    }

    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en esta JVM", e);
        }
    }
}
