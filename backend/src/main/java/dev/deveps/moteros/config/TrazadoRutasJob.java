package dev.deveps.moteros.config;

import dev.deveps.moteros.repositories.RutaRepository;
import dev.deveps.moteros.services.RutaService;
import dev.deveps.moteros.services.TrazadoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Calcula el trazado por carretera de las rutas que aun no lo tienen: las anteriores a esta
 * funcion y las que no se pudieron calcular al guardarlas (servicio caido o sin cuota).
 * Va despacio para no pasar del limite por minuto de OpenRouteService.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TrazadoRutasJob {

    static final int POR_PASADA = 30;
    private static final long PAUSA_MS = 2_000;

    private final RutaRepository rutaRepository;
    private final RutaService rutaService;
    private final TrazadoService trazadoService;

    /** Dos minutos despues de arrancar y luego cada media hora. */
    @Scheduled(initialDelay = 120_000, fixedDelay = 1_800_000)
    public void calcularPendientes() {
        if (!trazadoService.disponible()) {
            return;
        }
        List<Integer> ids = rutaRepository.idsSinTrazado(
                TrazadoService.MIN_PUNTOS, TrazadoService.MAX_PUNTOS, PageRequest.of(0, POR_PASADA));
        int calculadas = 0;
        for (Integer id : ids) {
            try {
                if (rutaService.recalcularTrazado(id)) {
                    calculadas++;
                } else {
                    // Sin respuesta del servicio: se deja para la siguiente pasada.
                    break;
                }
                Thread.sleep(PAUSA_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (RuntimeException e) {
                log.warn("No se ha podido calcular el trazado de la ruta {}: {}", id, e.getMessage());
            }
        }
        if (calculadas > 0) {
            log.info("Trazado por carretera calculado para {} rutas", calculadas);
        }
    }
}
