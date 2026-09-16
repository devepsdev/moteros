import json
import time
from dataclasses import dataclass, field

from core.logger import logger
from pipeline.extract import extract_routes
from pipeline.geocode import add_coordinates


@dataclass
class Summary:
    sources: int = 0
    skipped_unchanged: int = 0
    skipped_robots: int = 0
    found: int = 0
    created: int = 0
    duplicates: int = 0
    invalid: int = 0
    failed_sources: list[str] = field(default_factory=list)

    def line(self) -> str:
        return (
            f"fuentes {self.sources} · sin cambios {self.skipped_unchanged} · bloqueadas por robots {self.skipped_robots}"
            f" · rutas encontradas {self.found} · nuevas en la bandeja {self.created}"
            f" · ya conocidas {self.duplicates} · rechazadas {self.invalid} · fuentes con error {len(self.failed_sources)}"
        )


def run(sources, *, fetcher, client, geocoder, api, state, dry_run=False, force=False, delay=0.0) -> Summary:
    summary = Summary()
    for index, source in enumerate(sources):
        summary.sources += 1
        url = source["url"]
        label = source.get("nombre", url)
        if index > 0 and delay:
            time.sleep(delay)

        try:
            if not fetcher.allowed(url):
                logger.info("[%s] robots.txt no permite descargarla; se salta", label)
                summary.skipped_robots += 1
                continue

            page = fetcher.fetch(url)
            if not force and state.is_unchanged(url, page.content_hash):
                logger.info("[%s] sin cambios desde la última pasada", label)
                summary.skipped_unchanged += 1
                continue

            rutas = extract_routes(page.text, source, client)
            summary.found += len(rutas)
            logger.info("[%s] %d rutas extraídas", label, len(rutas))

            for ruta in rutas:
                sugerencia = add_coordinates(ruta, geocoder)
                # Solo con una ruta en la página se sabe a cuál pertenecen los enlaces al recorrido.
                if len(rutas) == 1 and page.enlaces_track:
                    sugerencia["enlacesTrack"] = page.enlaces_track
                ubicados = sum(1 for p in sugerencia["puntos"] if p["latitud"] is not None)
                if dry_run:
                    logger.info("  (simulación) %s", json.dumps(sugerencia, ensure_ascii=False))
                    continue
                result = api.submit_suggestion(sugerencia)
                etiqueta = f"{sugerencia['nombre']} ({sugerencia['puntoInicio']} → {sugerencia['puntoFin']})"
                if result.outcome == "created":
                    summary.created += 1
                    logger.info("  + %s · %d/%d lugares ubicados", etiqueta, ubicados, len(sugerencia["puntos"]))
                elif result.outcome == "duplicate":
                    summary.duplicates += 1
                    logger.info("  = %s: %s", etiqueta, result.message)
                else:
                    summary.invalid += 1
                    logger.warning("  ! %s rechazada por la API: %s", etiqueta, result.message)

            # Solo se da la página por procesada si todo ha ido bien: si algo falla
            # a medias, la próxima pasada la vuelve a intentar entera.
            if not dry_run:
                state.mark_processed(url, page.content_hash)
        except Exception as error:  # una fuente rota no debe parar las demás
            logger.error("[%s] error: %s", label, error)
            summary.failed_sources.append(label)
    return summary
