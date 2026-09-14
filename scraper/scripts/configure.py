#!/usr/bin/env python3
"""
Crea o actualiza el .env del scraper de moter@s de forma interactiva.

    python3 /opt/apps/moteros-src/scraper/scripts/configure.py

Pide cada dato (Enter mantiene el valor actual), oculta la contraseña y la clave mientras
se escriben, comprueba que la cuenta del bot entra en la API con rol scraper y que la
clave de DeepSeek funciona, y guarda el fichero con permisos 600. Solo usa la biblioteca
estándar: funciona antes de crear el entorno virtual.
"""
import getpass
import json
import os
import sys
import urllib.error
import urllib.request
from pathlib import Path

HOME = Path(os.environ.get("SCRAPER_HOME", "/opt/apps/moteros-scraper"))
ENV_FILE = HOME / ".env"
USER_AGENT = "MoterosBot/1.0 (+https://moteros.deveps.dev)"

# (clave, pregunta, valor por defecto, se oculta al escribir)
FIELDS = [
    ("MOTEROS_API_URL", "URL de la API de moter@s", "https://moteros.deveps.dev", False),
    ("MOTEROS_USUARIO", "Email o nombre de usuario de la cuenta del bot", "", False),
    ("MOTEROS_PASSWORD", "Contraseña de la cuenta del bot", "", True),
    ("DEEPSEEK_API_KEY", "Clave de la API de DeepSeek", "", True),
    ("DEEPSEEK_MODEL", "Modelo de DeepSeek", "deepseek-flash", False),
    ("REQUEST_DELAY_SECONDS", "Segundos de espera entre páginas", "3", False),
]
REQUIRED = {"MOTEROS_USUARIO", "MOTEROS_PASSWORD", "DEEPSEEK_API_KEY"}


def read_env(path: Path) -> dict:
    """Lectura sencilla del .env existente, respetando comillas simples y dobles."""
    values = {}
    if not path.exists():
        return values
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, raw = line.split("=", 1)
        raw = raw.strip()
        if len(raw) >= 2 and raw[0] == raw[-1] == "'":
            value = raw[1:-1]
        elif len(raw) >= 2 and raw[0] == raw[-1] == '"':
            value = raw[1:-1].replace('\\"', '"').replace("\\\\", "\\")
        else:
            value = raw
        values[key.strip()] = value
    return values


def quote(value: str) -> str:
    """
    Entre comillas simples python-dotenv lee el valor tal cual, sin interpretar "$", "#"
    ni barras. Solo si el propio valor lleva una comilla simple se usan dobles, escapando
    barras y comillas.
    """
    if "'" not in value:
        return f"'{value}'"
    return '"' + value.replace("\\", "\\\\").replace('"', '\\"') + '"'


def write_env(path: Path, values: dict) -> None:
    lines = ["# Configuración del scraper de moter@s. Generado por scripts/configure.py."]
    lines += [f"{key}={quote(value)}" for key, value in values.items()]
    content = "\n".join(lines) + "\n"

    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_name(path.name + ".tmp")
    # Se crea ya con 600: el fichero con secretos no llega a existir ni un instante con
    # permisos más abiertos.
    descriptor = os.open(temporary, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
    with os.fdopen(descriptor, "w", encoding="utf-8") as handle:
        handle.write(content)
    os.replace(temporary, path)
    os.chmod(path, 0o600)


def http_json(method: str, url: str, body: dict | None = None, token: str | None = None) -> dict:
    headers = {"User-Agent": USER_AGENT, "Accept": "application/json"}
    data = None
    if body is not None:
        headers["Content-Type"] = "application/json"
        data = json.dumps(body).encode("utf-8")
    if token:
        headers["Authorization"] = f"Bearer {token}"
    request = urllib.request.Request(url, data=data, headers=headers, method=method)
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.loads(response.read().decode("utf-8"))


def check_moteros(api_url: str, usuario: str, password: str) -> tuple[bool, str]:
    try:
        token = http_json("POST", f"{api_url}/api/auth/login", {"identificador": usuario, "password": password})["data"]["token"]
        rol = http_json("GET", f"{api_url}/api/usuarios/me", token=token).get("data", {}).get("rol")
    except urllib.error.HTTPError as error:
        if error.code in (400, 401):
            return False, "el usuario o la contraseña no son correctos"
        if error.code == 429:
            return False, "demasiados intentos de inicio de sesión; espera 15 minutos"
        return False, f"la API respondió {error.code}"
    except (urllib.error.URLError, TimeoutError) as error:
        return False, f"no se puede conectar con la API ({error})"
    if rol != "scraper":
        return False, f"la cuenta tiene rol {rol}; cámbialo a Scraper en el panel (Usuarios)"
    return True, "la cuenta entra y tiene rol scraper"


def check_deepseek(api_key: str, model: str) -> tuple[bool, str]:
    """Consulta la lista de modelos: comprueba la clave y el modelo sin gastar saldo."""
    try:
        models = [item["id"] for item in http_json("GET", "https://api.deepseek.com/models", token=api_key).get("data", [])]
    except urllib.error.HTTPError as error:
        if error.code == 401:
            return False, "DeepSeek rechaza la clave"
        return False, f"DeepSeek respondió {error.code}"
    except (urllib.error.URLError, TimeoutError) as error:
        return False, f"no se puede conectar con DeepSeek ({error})"
    if model not in models:
        return False, f"la clave funciona, pero el modelo «{model}» no existe; disponibles: {', '.join(models)}"
    return True, f"la clave funciona y el modelo «{model}» está disponible"


def ask(question: str, default: str, secret: bool, current: str | None) -> str:
    if secret:
        hint = " (Enter para mantener la actual)" if current else ""
        answer = getpass.getpass(f"{question}{hint}: ")
        return answer or (current or "")
    shown = current or default
    answer = input(f"{question} [{shown}]: " if shown else f"{question}: ").strip()
    return answer or shown


def main() -> int:
    if not HOME.is_dir():
        print(f"No existe {HOME}. Créalo primero (ver scraper/README.md).", file=sys.stderr)
        return 1

    existing = read_env(ENV_FILE)
    print(f"Configurando {ENV_FILE}\n")

    values = {}
    for key, question, default, secret in FIELDS:
        while True:
            value = ask(question, default, secret, existing.get(key))
            if value or key not in REQUIRED:
                break
            print("  Este dato es obligatorio.")
        values[key] = value
    # Variables añadidas a mano que este script no conoce: se conservan.
    for key, value in existing.items():
        values.setdefault(key, value)

    print("\nComprobando...")
    results = [
        ("API de moter@s", *check_moteros(values["MOTEROS_API_URL"].rstrip("/"), values["MOTEROS_USUARIO"], values["MOTEROS_PASSWORD"])),
        ("DeepSeek", *check_deepseek(values["DEEPSEEK_API_KEY"], values["DEEPSEEK_MODEL"])),
    ]
    for name, ok, message in results:
        print(f"  {'OK   ' if ok else 'FALLO'} {name}: {message}")

    if not all(ok for _, ok, _ in results):
        if input("\nHay comprobaciones fallidas. ¿Guardar igualmente? [s/N]: ").strip().lower() != "s":
            print("No se ha guardado nada.")
            return 1

    write_env(ENV_FILE, values)
    print(f"\nGuardado {ENV_FILE} con permisos 600.")
    print("Prueba sin enviar nada a la bandeja:")
    print(f"  cd /opt/apps/moteros-src/scraper && SCRAPER_HOME={HOME} {HOME}/venv/bin/python main.py --dry-run")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        print("\nCancelado. No se ha guardado nada.")
        sys.exit(130)
