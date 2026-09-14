"""
Scraper de moter@s: lee las fuentes de sources.yaml, extrae las rutas en moto con
DeepSeek, ubica los lugares de paso con OpenStreetMap y las envía como sugerencias a
la bandeja de revisión del panel.

Uso:
  python main.py                   Pasada normal.
  python main.py --dry-run         Extrae y muestra las rutas sin enviar nada.
  python main.py --force           Reanaliza también las páginas sin cambios.
  python main.py --source burgos   Solo las fuentes cuyo nombre o URL contengan el texto.
"""
import argparse
import sys

import yaml

from core import config
from core.logger import logger, setup_logging
from pipeline.fetch import Fetcher
from pipeline.geocode import Geocoder
from pipeline.run import run
from pipeline.state import State
from services.deepseek_client import DeepSeekClient
from services.moteros_api import ApiError, MoterosApi


def load_sources(path, only: str | None) -> list[dict]:
    with open(path, encoding="utf-8") as handle:
        sources = (yaml.safe_load(handle) or {}).get("sources") or []
    valid = [source for source in sources if isinstance(source, dict) and source.get("url")]
    if only:
        needle = only.lower()
        valid = [s for s in valid if needle in s["url"].lower() or needle in str(s.get("nombre", "")).lower()]
    return valid


def main() -> int:
    parser = argparse.ArgumentParser(description="Scraper de rutas en moto de moter@s")
    parser.add_argument("--dry-run", action="store_true", help="no envía nada a la API ni marca páginas como procesadas")
    parser.add_argument("--force", action="store_true", help="analiza también las páginas sin cambios")
    parser.add_argument("--source", help="procesa solo las fuentes que contengan este texto")
    parser.add_argument("--verbose", action="store_true")
    args = parser.parse_args()
    setup_logging(args.verbose)

    missing = config.missing_settings(args.dry_run)
    if missing:
        logger.error("Faltan variables en %s: %s", config.HOME / ".env", ", ".join(missing))
        return 2

    sources = load_sources(config.SOURCES_FILE, args.source)
    if not sources:
        logger.warning("No hay fuentes que procesar en %s", config.SOURCES_FILE)
        return 0

    api = None
    if not args.dry_run:
        api = MoterosApi(config.MOTEROS_API_URL, config.MOTEROS_USUARIO, config.MOTEROS_PASSWORD)
        try:
            api.login()
            api.require_scraper_role()
        except ApiError as error:
            logger.error("%s", error)
            return 2

    state = State(config.STATE_DB)
    logger.info("Inicio de pasada: %d fuentes%s", len(sources), " (simulación)" if args.dry_run else "")
    summary = run(
        sources,
        fetcher=Fetcher(),
        client=DeepSeekClient(config.DEEPSEEK_API_KEY, config.DEEPSEEK_MODEL),
        # La caché de coordenadas se guarda también en simulación: no gasta nada y ahorra consultas.
        geocoder=Geocoder(state),
        api=api,
        state=state,
        dry_run=args.dry_run,
        force=args.force,
        delay=config.REQUEST_DELAY_SECONDS,
    )
    logger.info("Fin de pasada: %s", summary.line())
    return 1 if summary.failed_sources else 0


if __name__ == "__main__":
    sys.exit(main())
