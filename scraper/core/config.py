import os
from pathlib import Path

from dotenv import load_dotenv

BASE_DIR = Path(__file__).resolve().parent.parent

# En el despliegue, el código está en el repositorio y los datos (.env, estado,
# logs) en SCRAPER_HOME. En local, todo cuelga de la carpeta scraper/.
HOME = Path(os.getenv("SCRAPER_HOME", str(BASE_DIR)))

# interpolate=False: sin esto python-dotenv expande "$VAR" y una contraseña que
# contenga "$" llegaría corrupta a la API.
load_dotenv(HOME / ".env", interpolate=False)

MOTEROS_API_URL = os.getenv("MOTEROS_API_URL", "https://moteros.deveps.dev").rstrip("/")
# Email o nombre de usuario de la cuenta del bot (el login de moter@s acepta ambos).
MOTEROS_USUARIO = os.getenv("MOTEROS_USUARIO", "")
MOTEROS_PASSWORD = os.getenv("MOTEROS_PASSWORD", "")

DEEPSEEK_API_KEY = os.getenv("DEEPSEEK_API_KEY", "")
DEEPSEEK_MODEL = os.getenv("DEEPSEEK_MODEL", "deepseek-flash")

SOURCES_FILE = Path(os.getenv("SOURCES_FILE", str(BASE_DIR / "sources.yaml")))
STATE_DB = HOME / "data" / "state.db"

REQUEST_DELAY_SECONDS = float(os.getenv("REQUEST_DELAY_SECONDS", "3"))
USER_AGENT = "MoterosBot/1.0 (+https://moteros.deveps.dev)"

# Tope de texto por página que se manda a DeepSeek: suficiente para una ruta
# larga o un listado de varias y mantiene acotado el coste de cada llamada.
MAX_PAGE_CHARS = 20000

# Nominatim (OpenStreetMap): su política de uso pide como máximo una petición por
# segundo, un User-Agent identificable y cachear los resultados.
NOMINATIM_URL = os.getenv("NOMINATIM_URL", "https://nominatim.openstreetmap.org/search")
NOMINATIM_DELAY_SECONDS = 1.1


def missing_settings(dry_run: bool) -> list[str]:
    """Variables obligatorias que faltan. En simulación no hace falta la cuenta de la API."""
    required = {"DEEPSEEK_API_KEY": DEEPSEEK_API_KEY}
    if not dry_run:
        required |= {"MOTEROS_USUARIO": MOTEROS_USUARIO, "MOTEROS_PASSWORD": MOTEROS_PASSWORD}
    return [name for name, value in required.items() if not value]
