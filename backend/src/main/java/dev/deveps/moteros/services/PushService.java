package dev.deveps.moteros.services;

import dev.deveps.moteros.entities.Usuario;

import java.util.Map;

/** Avisos al movil con la app cerrada, a traves del servicio de Expo. */
public interface PushService {

    /** Da de alta (o reasigna) el token del dispositivo para el usuario que ha iniciado sesion. */
    void registrar(String token, String plataforma);

    /** Baja del dispositivo, al cerrar sesion. */
    void eliminar(String token);

    /**
     * Envia un aviso a todos los dispositivos del usuario. No lanza: un fallo del servicio de
     * Expo no puede tumbar la operacion que provoco la notificacion.
     */
    void enviar(Usuario destino, String titulo, String cuerpo, Map<String, Object> datos);
}
