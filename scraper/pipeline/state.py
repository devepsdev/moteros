import sqlite3
from datetime import datetime
from pathlib import Path


class State:
    """
    Recuerda el contenido de cada página para no volver a analizar las que no han cambiado,
    y guarda las coordenadas ya buscadas para no repetir consultas a Nominatim.
    """

    def __init__(self, path: Path | str):
        if str(path) != ":memory:":
            Path(path).parent.mkdir(parents=True, exist_ok=True)
        self.db = sqlite3.connect(str(path))
        self.db.execute(
            "CREATE TABLE IF NOT EXISTS pages ("
            " url TEXT PRIMARY KEY, content_hash TEXT NOT NULL, processed_at TEXT NOT NULL)"
        )
        # latitud/longitud NULL = se buscó y no se encontró (tampoco se vuelve a preguntar).
        self.db.execute(
            "CREATE TABLE IF NOT EXISTS geocache ("
            " consulta TEXT PRIMARY KEY, latitud REAL, longitud REAL, searched_at TEXT NOT NULL)"
        )

    def is_unchanged(self, url: str, content_hash: str) -> bool:
        row = self.db.execute("SELECT content_hash FROM pages WHERE url = ?", (url,)).fetchone()
        return row is not None and row[0] == content_hash

    def mark_processed(self, url: str, content_hash: str) -> None:
        self.db.execute(
            "INSERT INTO pages (url, content_hash, processed_at) VALUES (?, ?, ?)"
            " ON CONFLICT(url) DO UPDATE SET content_hash = excluded.content_hash, processed_at = excluded.processed_at",
            (url, content_hash, _now()),
        )
        self.db.commit()

    def cached_coordinates(self, consulta: str) -> tuple[bool, tuple[float, float] | None]:
        """(encontrada en caché, coordenadas o None si se buscó sin resultado)."""
        row = self.db.execute("SELECT latitud, longitud FROM geocache WHERE consulta = ?", (consulta,)).fetchone()
        if row is None:
            return False, None
        return True, (row[0], row[1]) if row[0] is not None else None

    def store_coordinates(self, consulta: str, coordenadas: tuple[float, float] | None) -> None:
        latitud, longitud = coordenadas if coordenadas else (None, None)
        self.db.execute(
            "INSERT INTO geocache (consulta, latitud, longitud, searched_at) VALUES (?, ?, ?, ?)"
            " ON CONFLICT(consulta) DO UPDATE SET latitud = excluded.latitud, longitud = excluded.longitud,"
            " searched_at = excluded.searched_at",
            (consulta, latitud, longitud, _now()),
        )
        self.db.commit()


def _now() -> str:
    return datetime.now().isoformat(timespec="seconds")
