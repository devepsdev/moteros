package dev.deveps.moteros.services.impl;

import dev.deveps.moteros.services.TrazadoService;
import dev.deveps.moteros.support.Curvas;
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
    /** Dos recorridos que no se separan mas de esto son, a efectos practicos, el mismo. */
    static final double SEPARACION_MINIMA_KM = 0.4;
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

    private final Map<String, List<Alternativa>> cacheAlternativas = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, List<Alternativa>> eldest) {
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
        List<Tramo> tramos = pedir(puntos, evitarAutopistas, false);
        return tramos.isEmpty() ? null : tramos.getFirst();
    }

    /** Pide el recorrido (o las alternativas) a ORS. Lista vacia si no hay carretera. */
    private List<Tramo> pedir(List<double[]> puntos, boolean evitarAutopistas, boolean conAlternativas) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        // ORS espera [longitud, latitud]; sin radio maximo para encontrar la carretera mas cercana
        // (los puntos del scraper son centros de pueblo, a veces lejos de la via).
        cuerpo.put("coordinates", puntos.stream().map(p -> List.of(p[1], p[0])).toList());
        cuerpo.put("radiuses", puntos.stream().map(p -> -1).toList());
        cuerpo.put("instructions", false);
        if (evitarAutopistas) {
            cuerpo.put("options", Map.of("avoid_features", List.of("highways")));
        }
        if (conAlternativas) {
            // Hasta 3 caminos que compartan como mucho el 60 % y no sean mas de un 60 % mas largos.
            cuerpo.put("alternative_routes", Map.of("target_count", 3, "share_factor", 0.6, "weight_factor", 1.6));
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
            return leerTramos(respuesta);
        } catch (HttpClientErrorException e) {
            int status = e.getStatusCode().value();
            // 401/403: clave mala; 429: cuota agotada. Son pasajeros: se reintentara.
            if (status == 401 || status == 403 || status == 429) {
                throw e;
            }
            log.info("Sin recorrido por carretera ({}): {}", status, e.getResponseBodyAsString());
            return List.of();
        }
    }

    @Override
    public Optional<List<Alternativa>> alternativas(double[] origen, double[] destino) {
        if (!disponible()) {
            return Optional.empty();
        }
        String clave = Polilinea.codificar(List.of(origen, destino));
        synchronized (cacheAlternativas) {
            List<Alternativa> guardadas = cacheAlternativas.get(clave);
            if (guardadas != null) {
                return Optional.of(guardadas);
            }
        }
        Optional<List<Alternativa>> resultado = calcularAlternativas(origen, destino);
        resultado.ifPresent(lista -> {
            synchronized (cacheAlternativas) {
                cacheAlternativas.put(clave, lista);
            }
        });
        return resultado;
    }

    private Optional<List<Alternativa>> calcularAlternativas(double[] origen, double[] destino) {
        List<Tramo> tramos;
        try {
            tramos = pedir(List.of(origen, destino), true, true);
            if (tramos.isEmpty()) {
                tramos = pedir(List.of(origen, destino), false, true);
            }
        } catch (RestClientException e) {
            log.warn("OpenRouteService no ha respondido: {}", e.getMessage());
            return Optional.empty();
        }
        List<Alternativa> lista = new ArrayList<>();
        List<List<double[]>> elegidas = new ArrayList<>();
        for (int i = 0; i < tramos.size(); i++) {
            Tramo t = tramos.get(i);
            List<double[]> geometria = t.coordenadas();
            // Casi igual que otra ya elegida: no aporta nada.
            if (elegidas.stream().anyMatch(otra -> Curvas.separacionKm(geometria, otra) < SEPARACION_MINIMA_KM)) {
                continue;
            }
            List<double[]> paso = List.of();
            if (i > 0) {
                paso = puntosQueReproducen(origen, destino, geometria, tramos.getFirst().coordenadas());
                if (paso == null) {
                    // No se puede obligar al trazado a ir por ahi: mejor no ofrecerla.
                    continue;
                }
            }
            elegidas.add(geometria);
            lista.add(new Alternativa(
                    Polilinea.codificar(geometria),
                    Math.round(t.metros() / 100.0) / 10.0,
                    (int) Math.round(t.segundos() / 60.0),
                    Math.round(Curvas.gradosPorKm(geometria)),
                    paso));
        }
        return Optional.of(lista);
    }

    /**
     * Puntos de paso sobre {@code alternativa} con los que el trazado normal (el de
     * {@link #calcular}) la sigue. Se prueban varios candidatos y se comprueba cada uno
     * calculando el recorrido; null si ninguno la reproduce.
     */
    private List<double[]> puntosQueReproducen(double[] origen, double[] destino,
                                               List<double[]> alternativa, List<double[]> principal) {
        for (List<double[]> candidato : Curvas.candidatosDePaso(alternativa, principal)) {
            List<double[]> puntos = new ArrayList<>();
            puntos.add(origen);
            puntos.addAll(candidato);
            puntos.add(destino);
            Tramo t = pedirTramo(puntos, true);
            if (t != null
                    && Curvas.separacionKm(t.coordenadas(), alternativa) < SEPARACION_MINIMA_KM
                    && Curvas.separacionKm(alternativa, t.coordenadas()) < SEPARACION_MINIMA_KM) {
                return candidato;
            }
        }
        return null;
    }

    /** Lee la primera ruta de la respuesta GeoJSON de ORS; null si no trae ninguna. */
    static Tramo leerTramo(String geojson) {
        List<Tramo> tramos = leerTramos(geojson);
        return tramos.isEmpty() ? null : tramos.getFirst();
    }

    /** Todas las rutas de la respuesta GeoJSON de ORS (varias si se pidieron alternativas). */
    static List<Tramo> leerTramos(String geojson) {
        List<Tramo> tramos = new ArrayList<>();
        for (JsonNode feature : JSON.readTree(geojson).path("features")) {
            JsonNode coords = feature.path("geometry").path("coordinates");
            if (!coords.isArray() || coords.size() < 2) {
                continue;
            }
            List<double[]> lista = new ArrayList<>(coords.size());
            for (JsonNode c : coords) {
                lista.add(new double[]{c.get(1).asDouble(), c.get(0).asDouble()});
            }
            JsonNode resumen = feature.path("properties").path("summary");
            tramos.add(new Tramo(lista, resumen.path("distance").asDouble(0), resumen.path("duration").asDouble(0)));
        }
        return tramos;
    }
}
