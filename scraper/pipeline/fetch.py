import hashlib
import re
from dataclasses import dataclass
from urllib.parse import urlsplit
from urllib.robotparser import RobotFileParser

import requests
from bs4 import BeautifulSoup

from core.config import MAX_PAGE_CHARS, USER_AGENT

# Partes de la página que no describen la ruta y solo meten ruido.
NOISE_TAGS = ["script", "style", "noscript", "svg", "iframe", "nav", "footer", "form", "button"]

# Elementos que forman un bloque propio: solo ellos cortan línea. Si se usara un
# salto por cada etiqueta, cada enlace o negrita partiría la frase en trozos.
BLOCK_TAGS = ["p", "div", "li", "ul", "ol", "tr", "td", "th", "dt", "dd", "section", "article", "header",
              "h1", "h2", "h3", "h4", "h5", "h6", "blockquote", "table", "figcaption"]


@dataclass
class Page:
    url: str
    text: str
    content_hash: str


class Fetcher:
    def __init__(self, session: requests.Session | None = None):
        self.session = session or requests.Session()
        self.session.headers["User-Agent"] = USER_AGENT
        self._robots: dict[str, RobotFileParser | None] = {}

    def allowed(self, url: str) -> bool:
        """Respeta robots.txt. Si no se puede leer, se asume permitido, como hacen los buscadores."""
        parts = urlsplit(url)
        origin = f"{parts.scheme}://{parts.netloc}"
        if origin not in self._robots:
            self._robots[origin] = self._load_robots(origin)
        parser = self._robots[origin]
        return parser is None or parser.can_fetch(USER_AGENT, url)

    def fetch(self, url: str) -> Page:
        response = self.session.get(url, timeout=30)
        response.raise_for_status()
        content_type = response.headers.get("Content-Type", "")
        if "html" not in content_type:
            raise ValueError(f"No es una página HTML ({content_type or 'sin tipo'})")
        # requests asume ISO-8859-1 si el servidor no declara codificación, y
        # rompe tildes y eñes; en ese caso se usa la que detecta en el contenido.
        if response.encoding is None or response.encoding.lower() == "iso-8859-1":
            response.encoding = response.apparent_encoding
        text = html_to_text(response.text)
        return Page(url=url, text=text, content_hash=hashlib.sha256(text.encode("utf-8")).hexdigest())

    def _load_robots(self, origin: str) -> RobotFileParser | None:
        try:
            response = self.session.get(f"{origin}/robots.txt", timeout=15)
        except requests.RequestException:
            return None
        if response.status_code >= 400:
            return None
        parser = RobotFileParser()
        parser.parse(response.text.splitlines())
        return parser


def html_to_text(html: str) -> str:
    """Texto legible de la página, sin menús ni scripts, recortado para la extracción."""
    soup = BeautifulSoup(html, "html.parser")
    for tag in soup(NOISE_TAGS):
        tag.decompose()
    root = soup.find("main") or soup.find("article") or soup.body or soup
    for br in root.find_all("br"):
        br.replace_with("\n")
    for block in root.find_all(BLOCK_TAGS):
        block.insert_after("\n")

    lines = []
    for line in root.get_text(" ").splitlines():
        line = re.sub(r"\s+", " ", line).strip()
        # Al unir con espacios queda "Roa , Burgos": se vuelve a pegar la puntuación.
        line = re.sub(r"\s+([,.;:!?)])", r"\1", line)
        if line:
            lines.append(line)
    return "\n".join(lines)[:MAX_PAGE_CHARS]
