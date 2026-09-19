package dev.deveps.moteros.services.impl;

import dev.deveps.moteros.services.TrazadoService;
import dev.deveps.moteros.support.Polilinea;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Recorridos por carretera con OpenRouteService (datos de OpenStreetMap), perfil de coche.
 * Se evitan autopistas y autovias: entre dos pueblos, una ruta en moto va por carreteras
 * secundarias. Si asi no hay recorrido, se repite sin esa restriccion.
 *
 * Sin {@code ORS_API_KEY} el servicio no esta disponible y las rutas se dibujan uniendo sus
 * puntos con lineas rectas, como antes.
 */
@Service
@Slf4j
public class TrazadoServiceImpl implements TrazadoService {

    /** Puntos por peticion: el limite de waypoints de la API publica. */
    static final int PUNTOS_POR_PETICION = 50;
    private static final int MAX_CACHE = 300;
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final String apiKey;
    private final RestClient http;

    /** Vista previa en la app mientras se trazan puntos: se repiten mucho las mismas peticiones. */
    private final Map<String, Optional<Trazado>> cache = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Optional<Trazado>> eldest) {
            return size() > MAX_CACHE;
        }
    };

    public TrazadoServiceImpl(@Value("${app.ors.api-key:}") String apiKey,
                              @Value("${app.ors.url:https://api.openrouteservice.org}") String url) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(20));
        this.http = RestClient.builder().baseUrl(url).requestFactory(factory).build();
    }

    @Override
    public boolean disponible() {
        return !apiKey.isEmpty();
    }

    @Override
    public Optional<Trazado> calcular(List<double[]> puntos) {
        if (!disponible() || puntos.size() < MIN_PUNTOS || puntos.size() > MAX_PUNTOS) {
            return Optional.empty();
        }
        String clave = Polilinea.codificar(puntos);
        synchronized (cache) {
            Optional<Trazado> guardado = cache.get(clave);
            if (guardado != null) {
                return guardado;
            }
        }
        Optional<Trazado> resultado = calcularSinCache(puntos);
        // Los fallos pasajeros no se guardan, para reintentarlos.
        if (resultado.isPresent()) {
            synchronized (cache) {
                cache.put(clave, resultado);
            }
        }
        return resultado;
    }

    private Optional<Trazado> calcularSinCache(List<double[]> puntos) {
        List<double[]> recorrido = new ArrayList<>();
        double metros = 0;
        double segundos = 0;
        // Tramos de hasta 50 puntos que comparten el punto de union.
        for (int inicio = 0; inicio < puntos.size() - 1; inicio += PUNTOS_POR_PETICION - 1) {
            List<double[]> tramo = puntos.subList(inicio, Math.min(inicio + PUNTOS_POR_PETICION, puntos.size()));
            Tramo t;
            try {
                t = pedirTramo(tramo, true);
                if (t == null) {
                    t = pedirTramo(tramo, false);
                }
            } catch (RestClientException e) {
                log.warn("OpenRouteService no ha respondido: {}", e.getMessage());
                return Optional.empty();
            }
            if (t == null) {
                return Optional.of(new Trazado("", 0, 0));
            }
            List<double[]> coords = t.coordenadas();
            recorrido.addAll(recorrido.isEmpty() ? coords : coords.subList(1, coords.size()));
            metros += t.metros();
            segundos += t.segundos();
        }
        return Optional.of(new Trazado(Polilinea.codificar(recorrido),
                Math.round(metros / 100.0) / 10.0, (int) Math.round(segundos / 60.0)));
    }

    /** Un tramo calculado, o null si no hay recorrido por carretera entre esos puntos. */
    record Tramo(List<double[]> coordenadas, double metros, double segundos) {
    }

    private Tramo pedirTramo(List<double[]> puntos, boolean evitarAutopistas) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        // ORS espera [longitud, latitud]; sin radio maximo para encontrar la carretera mas cercana
        // (los puntos del scraper son centros de pueblo, a veces lejos de la via).
        cuerpo.put("coordinates", puntos.stream().map(p -> List.of(p[1], p[0])).toList());
        cuerpo.put("radiuses", puntos.stream().map(p -> -1).toList());
        cuerpo.put("instructions", false);
        if (evitarAutopistas) {
            cuerpo.put("options", Map.of("avoid_features", List.of("highways")));
        }
        try {
            String respuesta = http.post()
                    .uri("/v2/directions/driving-car/geojson")
                    .header("Authorization", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.valueOf("application/geo+json"), MediaType.APPLICATION_JSON)
                    .body(JSON.writeValueAsString(cuerpo))
                    .retrieve()
                    .body(String.class);
            return leerTramo(respuesta);
        } catch (HttpClientErrorException e) {
            int status = e.getStatusCode().value();
            // 401/403: clave mala; 429: cuota agotada. Son pasajeros: se reintentara.
            if (status == 401 || status == 403 || status == 429) {
                throw e;
            }
            log.info("Sin recorrido por carretera ({}): {}", status, e.getResponseBodyAsString());
            return null;
        }
    }

    /** Lee la respuesta GeoJSON de ORS. */
    static Tramo leerTramo(String geojson) {
        JsonNode feature = JSON.readTree(geojson).path("features").path(0);
        JsonNode coords = feature.path("geometry").path("coordinates");
        if (!coords.isArray() || coords.size() < 2) {
            return null;
        }
        List<double[]> lista = new ArrayList<>(coords.size());
        for (JsonNode c : coords) {
            lista.add(new double[]{c.get(1).asDouble(), c.get(0).asDouble()});
        }
        JsonNode resumen = feature.path("properties").path("summary");
        return new Tramo(lista, resumen.path("distance").asDouble(0), resumen.path("duration").asDouble(0));
    }
}
