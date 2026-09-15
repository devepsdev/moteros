import re
import unicodedata

from core.logger import logger

DIFICULTADES = {"facil", "moderada", "dificil", "extrema"}
TERRENOS = {"asfalto", "offroad", "mixto"}

SYSTEM_PROMPT = """Eres un asistente que extrae rutas para hacer en moto de páginas web españolas.

Extrae SOLO rutas en moto por carretera o pista con un recorrido identificable
(una salida, una llegada y los lugares por los que pasa). Ignora rutas a pie o en
bicicleta, viajes organizados con fechas y precio, y listas de consejos sin recorrido.

Reglas:
- No inventes nada. Si un dato no aparece en el texto, usa null.
- "puntoInicio" y "puntoFin": nombre de la localidad o lugar concreto de salida y de
  llegada (un pueblo, una ciudad, un puerto de montaña). NUNCA una comunidad autónoma,
  provincia, comarca, costa o región: si el texto solo da regiones, no extraigas esa ruta.
  Si la ruta es circular, la llegada es la misma que la salida.
- "lugaresDePaso": los lugares por los que pasa, EN ORDEN de recorrido, empezando por la
  salida y terminando por la llegada. Cada uno es un objeto con "nombre" (solo el nombre
  del sitio, tal como aparece en el texto) y "provincia" (la provincia española donde
  está ESE lugar concreto; en una ruta que cruza varias provincias, cada lugar lleva la
  suya). Si no sabes con seguridad la provincia de un lugar, pon null.
- "distanciaKm": número de kilómetros si aparece; "duracionMin": minutos si aparece
  (convierte "3 horas" en 180).
- "dificultad" solo puede ser: facil, moderada, dificil o extrema. Solo si el texto
  permite deducirla (curvas cerradas, puertos exigentes, pistas); si no, null.
- "tipoTerreno" solo puede ser: asfalto, offroad o mixto.
- "descripcion": dos o tres frases en español redactadas por ti sobre qué ofrece la
  ruta (paisaje, tipo de carretera, qué ver). No copies el texto de la página.

Responde únicamente con un objeto json con esta forma:
{"rutas": [{"nombre": "...", "puntoInicio": "...", "puntoFin": "...",
  "lugaresDePaso": [{"nombre": "...", "provincia": "..."}, {"nombre": "...", "provincia": "..."}],
  "distanciaKm": 175, "duracionMin": null, "dificultad": null, "tipoTerreno": "asfalto",
  "descripcion": "..."}]}
Si la página no describe ninguna ruta de este tipo, responde {"rutas": []}."""

LIMITES = {"nombre": 120, "puntoInicio": 120, "puntoFin": 120, "descripcion": 5000, "lugar": 100, "provincia": 60}
MAX_LUGARES = 40
# Salida, al menos un lugar de paso y llegada: con menos no hay recorrido que dibujar.
MIN_LUGARES_DISTINTOS = 3


def extract_routes(page_text: str, source: dict, client) -> list[dict]:
    """Pide a DeepSeek las rutas de la página y devuelve solo las que pasan la validación."""
    pista = f"Si no indica la provincia de algún lugar, la página trata de: {source['provincia']}.\n" if source.get("provincia") else ""
    data = client.complete_json([
        {"role": "system", "content": SYSTEM_PROMPT},
        {"role": "user", "content": f"Página: {source['url']}\n{pista}\nTexto de la página:\n{page_text}"},
    ])

    raw_rutas = data.get("rutas") if isinstance(data, dict) else None
    if not isinstance(raw_rutas, list):
        logger.warning("Respuesta de DeepSeek sin lista de rutas; se ignora")
        return []

    rutas = []
    for raw in raw_rutas:
        ruta = normalize_route(raw, source)
        if ruta is not None:
            rutas.append(ruta)
    return rutas


def normalize_route(raw, source: dict) -> dict | None:
    """
    Convierte lo que devuelve el modelo en una ruta válida, o None si no sirve. Un modelo
    de lenguaje no es fiable con los formatos: todo se comprueba aquí antes de enviarlo.
    Devuelve los lugares de paso aún sin coordenadas; se geolocalizan después.
    """
    if not isinstance(raw, dict):
        return None

    nombre = _text(raw.get("nombre"))
    inicio = _text(raw.get("puntoInicio"))
    fin = _text(raw.get("puntoFin"))
    if not nombre or not inicio or not fin:
        return None
    if len(nombre) > LIMITES["nombre"] or len(inicio) > LIMITES["puntoInicio"] or len(fin) > LIMITES["puntoFin"]:
        logger.debug("Descartada por nombre, salida o llegada demasiado largos: %s", nombre[:60])
        return None

    # La provincia de la fuente solo se usa si el modelo no da la de un lugar: en una página
    # de una sola provincia (Turismo Burgos) es fiable; en un listado de toda España no la hay.
    provincia_fuente = _text(source.get("provincia"))
    lugares: list[dict] = []
    for raw_lugar in raw.get("lugaresDePaso") or []:
        lugar = _lugar(raw_lugar, provincia_fuente)
        # Se evitan repeticiones seguidas ("Roa, Roa") y textos que no son un nombre de sitio.
        if lugar and (not lugares or lugares[-1]["nombre"].lower() != lugar["nombre"].lower()):
            lugares.append(lugar)
    # La salida y la llegada abren y cierran siempre el recorrido.
    if not lugares or lugares[0]["nombre"].lower() != inicio.lower():
        lugares.insert(0, {"nombre": inicio, "provincia": provincia_fuente})
    if lugares[-1]["nombre"].lower() != fin.lower() or len(lugares) == 1:
        lugares.append({"nombre": fin, "provincia": provincia_fuente})
    lugares = lugares[:MAX_LUGARES]

    distintos = {lugar["nombre"].lower() for lugar in lugares}
    if len(distintos) < MIN_LUGARES_DISTINTOS:
        logger.info("  - Descartada «%s»: sin lugares de paso suficientes para dibujar el recorrido", nombre)
        return None

    dificultad = _plain(raw.get("dificultad"))
    terreno = _plain(raw.get("tipoTerreno"))
    if terreno == "off-road":
        terreno = "offroad"

    return {
        "urlFuente": source["url"][:500],
        "nombre": nombre,
        "descripcion": _limited(raw.get("descripcion"), "descripcion"),
        "puntoInicio": inicio,
        "puntoFin": fin,
        "distanciaKm": _positive(raw.get("distanciaKm"), maximum=99999.9, decimals=1),
        "duracionEstimadaMin": _positive_int(raw.get("duracionMin")),
        "dificultad": dificultad if dificultad in DIFICULTADES else None,
        "tipoTerreno": terreno if terreno in TERRENOS else None,
        "lugares": lugares,
    }


def _lugar(raw, provincia_fuente: str | None) -> dict | None:
    """Admite el formato pedido ({"nombre", "provincia"}) y, por si acaso, un texto suelto."""
    if isinstance(raw, dict):
        nombre = _text(raw.get("nombre"))
        provincia = _text(raw.get("provincia"))
    else:
        nombre, provincia = _text(raw), None
    if not nombre or len(nombre) > LIMITES["lugar"]:
        return None
    if provincia and len(provincia) > LIMITES["provincia"]:
        provincia = None
    return {"nombre": nombre, "provincia": provincia or provincia_fuente}


def _text(value) -> str | None:
    if not isinstance(value, str):
        return None
    collapsed = re.sub(r"\s+", " ", value).strip()
    return collapsed or None


def _limited(value, field: str) -> str | None:
    text = _text(value)
    return text[: LIMITES[field]] if text else None


def _plain(value) -> str | None:
    """"Difícil" -> "dificil": el modelo no siempre respeta la regla de las tildes."""
    text = _text(value)
    if not text:
        return None
    return "".join(c for c in unicodedata.normalize("NFD", text.lower()) if unicodedata.category(c) != "Mn")


def _positive(value, maximum: float, decimals: int) -> float | None:
    if isinstance(value, bool):
        return None
    if isinstance(value, str):
        match = re.search(r"\d+(?:[.,]\d+)?", value)
        value = float(match.group(0).replace(",", ".")) if match else None
    if not isinstance(value, (int, float)) or value <= 0 or value > maximum:
        return None
    return round(float(value), decimals)


def _positive_int(value) -> int | None:
    number = _positive(value, maximum=100000, decimals=0)
    return int(number) if number else None
