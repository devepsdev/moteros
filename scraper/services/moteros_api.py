from dataclasses import dataclass

import requests

from core.logger import logger


class ApiError(Exception):
    pass


@dataclass
class SubmitResult:
    """created: nueva en la bandeja; duplicate: el catálogo o la bandeja ya la tenían; invalid: la API la rechazó."""

    outcome: str
    message: str = ""


class MoterosApi:
    def __init__(self, base_url: str, usuario: str, password: str, session: requests.Session | None = None):
        self.base_url = base_url
        self.usuario = usuario
        self.password = password
        self.session = session or requests.Session()
        self._token: str | None = None

    def login(self) -> None:
        response = self.session.post(
            f"{self.base_url}/api/auth/login",
            json={"identificador": self.usuario, "password": self.password},
            timeout=30,
        )
        if response.status_code != 200:
            raise ApiError(f"No se ha podido iniciar sesión en la API ({response.status_code}): {_message(response)}")
        self._token = response.json()["data"]["token"]

    def require_scraper_role(self) -> None:
        """
        Con otro rol la API rechaza las sugerencias (403). Es mejor parar en seco al empezar
        que descargar y analizar todas las páginas para nada.
        """
        rol = self._request("GET", "/api/usuarios/me").json().get("data", {}).get("rol")
        if rol != "scraper":
            raise ApiError(f"La cuenta {self.usuario} tiene rol {rol}; asígnale el rol Scraper desde el panel (Usuarios).")

    def submit_suggestion(self, sugerencia: dict) -> SubmitResult:
        response = self._request("POST", "/api/sugerencias-ruta", json=sugerencia)
        if response.status_code == 201:
            return SubmitResult("created")
        if response.status_code == 409:
            return SubmitResult("duplicate", _message(response))
        if response.status_code == 400:
            return SubmitResult("invalid", _message(response))
        raise ApiError(f"Respuesta inesperada al enviar una sugerencia ({response.status_code}): {_message(response)}")

    def _request(self, method: str, path: str, **kwargs) -> requests.Response:
        if self._token is None:
            self.login()
        response = self.session.request(method, f"{self.base_url}{path}", headers=self._auth(), timeout=30, **kwargs)
        # El access token dura 15 minutos y en una pasada larga puede caducar. Con el token
        # caducado la API de moter@s responde 403 (no 401): se vuelve a entrar una vez.
        if response.status_code in (401, 403):
            logger.info("Sesión caducada o rechazada, se vuelve a iniciar sesión")
            self.login()
            response = self.session.request(method, f"{self.base_url}{path}", headers=self._auth(), timeout=30, **kwargs)
        return response

    def _auth(self) -> dict:
        return {"Authorization": f"Bearer {self._token}"}


def _message(response: requests.Response) -> str:
    try:
        body = response.json()
    except ValueError:
        return response.text[:200]
    data = body.get("data")
    detalle = f" {data}" if isinstance(data, dict) and data else ""
    return f"{body.get('message', '')}{detalle}"
