import time

import requests

from core.config import NOMINATIM_DELAY_SECONDS, NOMINATIM_URL, USER_AGENT
from core.logger import logger


class Geocoder:
    """
    Coordenadas de pueblos y lugares con Nominatim (OpenStreetMap), limitado a España.
    Cumple su política de uso: User-Agent propio, como mucho una petición por segundo y
    resultados cacheados (también los que no se encuentran) en el estado del scraper.
    """

    def __init__(self, state, session: requests.Session | None = None, delay: float = NOMINATIM_DELAY_SECONDS):
        self.state = state
        self.session = session or requests.Session()
        self.session.headers["User-Agent"] = USER_AGENT
        self.delay = delay
        self._ultima_peticion = 0.0

    def locate(self, lugar: str, provincia: str | None) -> tuple[float, float] | None:
        consulta = f"{lugar}, {provincia}" if provincia else lugar
        en_cache, coordenadas = self.state.cached_coordinates(consulta.lower())
        if en_cache:
            return coordenadas

        coordenadas = self._search(consulta)
        # Con la provincia a veces no encuentra puertos o parajes que sí existen: segundo intento sin ella.
        if coordenadas is None and provincia:
            coordenadas = self._search(lugar)
        self.state.store_coordinates(consulta.lower(), coordenadas)
        return coordenadas

    def _search(self, consulta: str) -> tuple[float, float] | None:
        espera = self.delay - (time.monotonic() - self._ultima_peticion)
        if espera > 0:
            time.sleep(espera)
        self._ultima_peticion = time.monotonic()
        try:
            response = self.session.get(
                NOMINATIM_URL,
                params={"q": consulta, "format": "jsonv2", "limit": 1, "countrycodes": "es", "accept-language": "es"},
                timeout=20,
            )
            response.raise_for_status()
            resultados = response.json()
        except (requests.RequestException, ValueError) as error:
            # Un fallo puntual no invalida la ruta: el lugar queda sin coordenadas.
            logger.warning("Nominatim no ha respondido para «%s»: %s", consulta, error)
            return None
        if not resultados:
            return None
        return round(float(resultados[0]["lat"]), 6), round(float(resultados[0]["lon"]), 6)


def add_coordinates(ruta: dict, geocoder: Geocoder) -> dict:
    """Sugerencia lista para la API: los lugares de paso pasan a ser puntos con coordenadas si se encontraron."""
    puntos = []
    for lugar in ruta["lugares"]:
        coordenadas = geocoder.locate(lugar, ruta.get("provincia"))
        puntos.append({
            "nombre": lugar,
            "latitud": coordenadas[0] if coordenadas else None,
            "longitud": coordenadas[1] if coordenadas else None,
        })
    sugerencia = {k: v for k, v in ruta.items() if k not in ("lugares", "provincia")}
    sugerencia["puntos"] = puntos
    return sugerencia
