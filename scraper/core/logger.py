import logging
import sys

logger = logging.getLogger("moteros-scraper")


def setup_logging(verbose: bool = False) -> None:
    # Bajo cron la codificación de la salida puede no ser UTF-8 y las tildes de
    # los nombres de pueblos romperían el log.
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    handler = logging.StreamHandler(sys.stdout)
    handler.setFormatter(logging.Formatter("%(asctime)s %(levelname)-7s %(message)s", "%Y-%m-%d %H:%M:%S"))
    logger.handlers[:] = [handler]
    logger.setLevel(logging.DEBUG if verbose else logging.INFO)
