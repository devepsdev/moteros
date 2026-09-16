import math
import statistics
import time

import requests

from core.config import NOMINATIM_DELAY_SECONDS, NOMINATIM_URL, USER_AGENT
from core.logger import logger

# Países donde se buscan los lugares: las rutas del Pirineo cruzan a Francia y Andorra
# (Catalunya Nord, Vall d'Aran) y las del oeste, a Portugal.
PAISES = "es,fr,ad,pt"

# Detección de homónimos lejanos (ver descartar_atipicos). Un lugar intermedio es sospechoso si
# pasar por él supone un rodeo de más de MIN_KM_RODEO y de más de FACTOR_RODEO veces ir directo
# entre sus vecinos; un extremo, si queda muy lejos de su único vecino comparado con los tramos
# normales de la ruta. Los mínimos en km evitan marcar rutas cortas y reviradas.
MIN_KM_RODEO = 60
FACTOR_RODEO = 2
MIN_KM_EXTREMO = 150
FACTOR_EXTREMO = 5


class Geocoder:
    """
    Coordenadas de pueblos y lugares con Nominatim (OpenStreetMap), en España y países vecinos.
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
                params={"q": consulta, "format": "jsonv2", "limit": 1, "countrycodes": PAISES, "accept-language": "es"},
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
        coordenadas = geocoder.locate(lugar["nombre"], lugar.get("provincia"))
        puntos.append({
            "nombre": lugar["nombre"],
            "latitud": coordenadas[0] if coordenadas else None,
            "longitud": coordenadas[1] if coordenadas else None,
        })
    descartar_atipicos(puntos, ruta["nombre"])
    sugerencia = {k: v for k, v in ruta.items() if k != "lugares"}
    sugerencia["puntos"] = puntos
    return sugerencia


def descartar_atipicos(puntos: list[dict], nombre_ruta: str = "") -> None:
    """
    Quita las coordenadas de los lugares que no encajan en el recorrido. Pasa con nombres repetidos
    en varios sitios («Riaño», «Potes», «Arles») cuando la provincia no basta para distinguirlos.
    El lugar se conserva sin coordenadas para que el administrador lo marque en el mapa.
    Limitación conocida: dos homónimos seguidos y cercanos entre sí no se detectan.

    No se compara con el centro del recorrido: en una ruta larga y lineal (Tortosa → Barcelona)
    la salida y la llegada están lógicamente lejos del centro. Se compara cada lugar con sus
    vecinos en el orden de la ruta, tomando como referencia el tramo típico entre lugares.
    """
    ubicados = [p for p in puntos if p["latitud"] is not None]
    if len(ubicados) < 3:
        return
    coord = [(p["latitud"], p["longitud"]) for p in ubicados]
    tramos = [_km(coord[i - 1], coord[i]) for i in range(1, len(coord))]
    tipico = statistics.median(tramos)

    sospechosos = []
    for i, punto in enumerate(ubicados):
        if i == 0 or i == len(ubicados) - 1:
            vecino = coord[1] if i == 0 else coord[-2]
            distancia = _km(coord[i], vecino)
            if distancia > max(MIN_KM_EXTREMO, FACTOR_EXTREMO * tipico):
                sospechosos.append((punto, distancia))
            continue
        anterior, siguiente = coord[i - 1], coord[i + 1]
        ida, vuelta, directo = _km(anterior, coord[i]), _km(coord[i], siguiente), _km(anterior, siguiente)
        rodeo = ida + vuelta - directo
        if rodeo > max(MIN_KM_RODEO, FACTOR_RODEO * directo) and min(ida, vuelta) > MIN_KM_RODEO / 2:
            sospechosos.append((punto, min(ida, vuelta)))

    # Se decide con todos los datos antes de borrar nada: quitar uno cambiaría los vecinos del siguiente.
    for punto, distancia in sospechosos:
        logger.info("  ? «%s» en «%s» queda a %.0f km del recorrido: se deja sin coordenadas", punto["nombre"], nombre_ruta, distancia)
        punto["latitud"] = punto["longitud"] = None


def _km(a: tuple[float, float], b: tuple[float, float]) -> float:
    """Distancia en km entre dos coordenadas (Haversine)."""
    rad = math.radians
    d_lat, d_lon = rad(b[0] - a[0]), rad(b[1] - a[1])
    h = math.sin(d_lat / 2) ** 2 + math.cos(rad(a[0])) * math.cos(rad(b[0])) * math.sin(d_lon / 2) ** 2
    return 2 * 6371 * math.asin(math.sqrt(h))
