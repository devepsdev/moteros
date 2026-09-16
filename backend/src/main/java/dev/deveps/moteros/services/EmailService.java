package dev.deveps.moteros.services;

public interface EmailService {

    void enviarCodigoRecuperacion(String para, String nombre, String codigo);

    /** Aviso a un administrador de que hay una denuncia nueva por revisar. */
    void avisarDenuncia(String para, String tipo, String motivo);
}
