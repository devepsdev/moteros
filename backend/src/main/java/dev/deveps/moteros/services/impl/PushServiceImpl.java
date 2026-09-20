package dev.deveps.moteros.services.impl;

import dev.deveps.moteros.entities.DispositivoPush;
import dev.deveps.moteros.entities.Usuario;
import dev.deveps.moteros.repositories.DispositivoPushRepository;
import dev.deveps.moteros.repositories.UsuarioRepository;
import dev.deveps.moteros.security.UsuarioAutenticadoProvider;
import dev.deveps.moteros.services.PushService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Avisos push con el servicio de Expo (https://docs.expo.dev/push-notifications/sending-notifications/).
 * No hace falta ninguna clave: el token de cada movil identifica la instalacion.
 *
 * El envio va en segundo plano porque una peticion HTTP no puede retrasar la respuesta de la API,
 * y los tokens que Expo rechaza por no existir ya (DeviceNotRegistered) se borran solos.
 */
@Service
@Slf4j
public class PushServiceImpl implements PushService {

    /** Limite de mensajes por peticion que acepta Expo. */
    private static final int POR_LOTE = 100;

    private final DispositivoPushRepository dispositivoRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioAutenticadoProvider usuarioAutenticado;
    private final TransactionTemplate transacciones;
    private final RestClient http;
    private final Executor envios = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "push");
        t.setDaemon(true);
        return t;
    });

    public PushServiceImpl(DispositivoPushRepository dispositivoRepository,
                           UsuarioRepository usuarioRepository,
                           UsuarioAutenticadoProvider usuarioAutenticado,
                           TransactionTemplate transacciones) {
        this.dispositivoRepository = dispositivoRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioAutenticado = usuarioAutenticado;
        this.transacciones = transacciones;
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(15));
        this.http = RestClient.builder()
                .baseUrl("https://exp.host/--/api/v2/push")
                .requestFactory(factory)
                .build();
    }

    @Override
    @Transactional
    public void registrar(String token, String plataforma) {
        if (token == null || token.isBlank()) {
            return;
        }
        Usuario usuario = usuarioRepository.findByUuid(usuarioAutenticado.obtenerUuidUsuarioActual())
                .orElse(null);
        if (usuario == null) {
            return;
        }
        DispositivoPush dispositivo = dispositivoRepository.findByToken(token.trim())
                .orElseGet(() -> DispositivoPush.builder().token(token.trim()).build());
        // El mismo movil puede cambiar de usuario: el token pasa al ultimo que inicia sesion.
        dispositivo.setUsuario(usuario);
        dispositivo.setPlataforma(plataforma);
        dispositivo.setFechaUso(LocalDateTime.now());
        dispositivoRepository.save(dispositivo);
    }

    @Override
    @Transactional
    public void eliminar(String token) {
        if (token != null && !token.isBlank()) {
            dispositivoRepository.deleteByToken(token.trim());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void enviar(Usuario destino, String titulo, String cuerpo, Map<String, Object> datos) {
        if (destino == null) {
            return;
        }
        List<String> tokens = dispositivoRepository.findByUsuarioId(destino.getId()).stream()
                .map(DispositivoPush::getToken)
                .toList();
        if (tokens.isEmpty()) {
            return;
        }
        List<Map<String, Object>> mensajes = tokens.stream()
                .map(token -> mensaje(token, titulo, cuerpo, datos))
                .toList();
        envios.execute(() -> enviarLotes(mensajes));
    }

    private Map<String, Object> mensaje(String token, String titulo, String cuerpo, Map<String, Object> datos) {
        Map<String, Object> mensaje = new java.util.LinkedHashMap<>();
        mensaje.put("to", token);
        mensaje.put("title", titulo);
        mensaje.put("body", cuerpo);
        mensaje.put("sound", "default");
        mensaje.put("channelId", "default");
        if (datos != null && !datos.isEmpty()) {
            mensaje.put("data", datos);
        }
        return mensaje;
    }

    private void enviarLotes(List<Map<String, Object>> mensajes) {
        for (int desde = 0; desde < mensajes.size(); desde += POR_LOTE) {
            List<Map<String, Object>> lote = mensajes.subList(desde, Math.min(desde + POR_LOTE, mensajes.size()));
            try {
                JsonNode respuesta = http.post()
                        .uri("/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(lote)
                        .retrieve()
                        .body(JsonNode.class);
                descartarMuertos(lote, respuesta);
            } catch (RestClientException e) {
                log.warn("No se han podido enviar {} avisos push: {}", lote.size(), e.getMessage());
            }
        }
    }

    /**
     * Expo responde con un resultado por mensaje, en el mismo orden. Los tokens de apps
     * desinstaladas dan DeviceNotRegistered y hay que dejar de usarlos.
     */
    private void descartarMuertos(List<Map<String, Object>> lote, JsonNode respuesta) {
        JsonNode datos = respuesta == null ? null : respuesta.get("data");
        if (datos == null || !datos.isArray()) {
            return;
        }
        List<String> muertos = new ArrayList<>();
        for (int i = 0; i < datos.size() && i < lote.size(); i++) {
            JsonNode detalles = datos.get(i).get("details");
            String error = detalles == null ? null : detalles.path("error").asString(null);
            if ("DeviceNotRegistered".equals(error)) {
                muertos.add(String.valueOf(lote.get(i).get("to")));
            }
        }
        if (!muertos.isEmpty()) {
            try {
                transacciones.executeWithoutResult(estado -> dispositivoRepository.deleteByTokenIn(muertos));
                log.info("Descartados {} dispositivos que ya no tienen la app", muertos.size());
            } catch (RuntimeException e) {
                log.warn("No se han podido descartar dispositivos: {}", e.getMessage());
            }
        }
    }
}
