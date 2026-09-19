package dev.deveps.moteros.controllers;

import dev.deveps.moteros.dto.ApiResponseDTO;
import dev.deveps.moteros.dto.TrazadoRequestDTO;
import dev.deveps.moteros.dto.TrazadoResponseDTO;
import dev.deveps.moteros.exceptions.TooManyRequestsException;
import dev.deveps.moteros.security.UsuarioAutenticadoProvider;
import dev.deveps.moteros.services.RutaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Vista previa del recorrido por carretera mientras se marcan los puntos de una ruta (app y
 * panel). La cuota de OpenRouteService es compartida, asi que cada usuario tiene un limite.
 */
@RestController
@RequestMapping("/api/rutas/trazado")
@RequiredArgsConstructor
public class TrazadoController {

    static final int MAX_POR_HORA = 150;
    private static final long HORA_MS = 3_600_000L;

    private final RutaService rutaService;
    private final UsuarioAutenticadoProvider usuarioAutenticado;
    private final Map<Integer, Deque<Long>> peticiones = new ConcurrentHashMap<>();

    @PostMapping
    public ResponseEntity<ApiResponseDTO<TrazadoResponseDTO>> vistaPrevia(@Valid @RequestBody TrazadoRequestDTO dto) {
        exigirCuota(usuarioAutenticado.obtenerIdUsuarioActual());
        List<double[]> puntos = dto.getPuntos().stream()
                .map(c -> new double[]{c.getLatitud(), c.getLongitud()})
                .toList();
        TrazadoResponseDTO res = rutaService.vistaPreviaTrazado(puntos)
                .filter(t -> t.encontrado())
                .map(t -> TrazadoResponseDTO.builder()
                        .trazado(t.polilinea())
                        .distanciaKm(t.distanciaKm())
                        .duracionMin(t.duracionMin())
                        .build())
                .orElseGet(TrazadoResponseDTO::new);
        return ResponseEntity.ok(ApiResponseDTO.success(res, "Trazado calculado"));
    }

    private void exigirCuota(Integer usuarioId) {
        long ahora = System.currentTimeMillis();
        Deque<Long> marcas = peticiones.computeIfAbsent(usuarioId, id -> new ArrayDeque<>());
        synchronized (marcas) {
            while (!marcas.isEmpty() && ahora - marcas.peekFirst() > HORA_MS) {
                marcas.pollFirst();
            }
            if (marcas.size() >= MAX_POR_HORA) {
                long espera = (HORA_MS - (ahora - marcas.peekFirst())) / 1000;
                throw new TooManyRequestsException("Has calculado muchos recorridos seguidos. Espera un poco.", espera);
            }
            marcas.addLast(ahora);
        }
    }
}
