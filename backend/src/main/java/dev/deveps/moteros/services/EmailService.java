package dev.deveps.moteros.services;

public interface EmailService {

    void enviarCodigoRecuperacion(String para, String nombre, String codigo);
}
