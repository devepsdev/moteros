package dev.deveps.moteros.config;

import dev.deveps.moteros.repositories.DenunciaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Borra las denuncias cerradas hace mas de un año. Guardan una copia del contenido
 * denunciado, y la politica de privacidad promete no conservarla mas tiempo.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LimpiezaDenunciasJob {

    static final int MESES_CONSERVACION = 12;

    private final DenunciaRepository denunciaRepository;

    /** Cada dia a las 04:15. */
    @Scheduled(cron = "0 15 4 * * *")
    @Transactional
    public void limpiar() {
        int borradas = denunciaRepository.borrarCerradasAntesDe(LocalDateTime.now().minusMonths(MESES_CONSERVACION));
        if (borradas > 0) {
            log.info("Borradas {} denuncias cerradas hace mas de {} meses", borradas, MESES_CONSERVACION);
        }
    }
}
