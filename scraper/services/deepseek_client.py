import json

import requests
from tenacity import retry, retry_if_exception_type, stop_after_attempt, wait_exponential

API_URL = "https://api.deepseek.com/v1/chat/completions"


class RetryableError(Exception):
    """Fallo pasajero: saturación, error del servidor o respuesta vacía."""


class TruncatedResponseError(Exception):
    """La respuesta no cabe en el límite de tokens. Repetir la misma petición daría lo mismo."""


class DeepSeekClient:
    def __init__(self, api_key: str, model: str, session: requests.Session | None = None):
        self.api_key = api_key
        self.model = model
        self.session = session or requests.Session()

    @retry(
        retry=retry_if_exception_type((RetryableError, requests.Timeout, requests.ConnectionError)),
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=2, min=4, max=30),
        reraise=True,
    )
    def complete_json(self, messages: list[dict], max_tokens: int = 8000) -> dict:
        """Pide una respuesta en modo JSON y la devuelve ya parseada."""
        response = self.session.post(
            API_URL,
            headers={"Authorization": f"Bearer {self.api_key}", "Content-Type": "application/json"},
            json={
                "model": self.model,
                "messages": messages,
                # Para extraer datos de un texto el razonamiento previo no aporta nada:
                # gasta tokens, tarda más y en páginas largas puede agotar el límite.
                "thinking": {"type": "disabled"},
                "temperature": 0.1,
                "max_tokens": max_tokens,
                "response_format": {"type": "json_object"},
            },
            timeout=120,
        )
        if response.status_code == 429 or response.status_code >= 500:
            raise RetryableError(f"DeepSeek respondió {response.status_code}")
        response.raise_for_status()

        choice = response.json()["choices"][0]
        if choice.get("finish_reason") == "length":
            raise TruncatedResponseError(
                f"la respuesta no cabe en {max_tokens} tokens: la página describe demasiadas rutas para una sola llamada")
        content = choice["message"].get("content")
        # La documentación de DeepSeek avisa de que el modo JSON a veces devuelve vacío.
        if not content or not content.strip():
            raise RetryableError("DeepSeek devolvió una respuesta vacía")
        return json.loads(content)
