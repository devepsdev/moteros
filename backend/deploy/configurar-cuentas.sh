#!/usr/bin/env bash
#
# Configura las cuentas de moter@s en producción y el scraper de la Orange Pi, preguntando
# los datos. Se ejecuta desde tu PC (Git Bash), con los alias SSH "vps" y "sbc".
#
#   bash backend/deploy/configurar-cuentas.sh
#
# 1. Comprueba que la base de datos 'moteros' tiene su usuario y contraseña (los creó
#    setup-vps.sh con una contraseña aleatoria) y que la API responde.
# 2. Crea el administrador como en rastrix: escribe ADMIN_* en /opt/apps/moteros/.env,
#    reinicia la API (AdminBootstrapRunner crea o asciende la cuenta), comprueba que entra
#    y borra ADMIN_PASSWORD del .env, que ya no hace falta.
# 3. Crea la cuenta del bot del scraper con una contraseña aleatoria y le pone el rol scraper.
# 4. Pide la clave de DeepSeek y escribe /opt/apps/moteros-scraper/.env (600) en la Orange Pi,
#    comprobando la cuenta del bot y la clave con scripts/configure.py.
#
# Las contraseñas y la clave nunca van en la línea de comandos: viajan por la entrada
# estándar de ssh o como variables de entorno de un proceso local.
#
set -euo pipefail

API_URL="${MOTEROS_API_URL:-https://moteros.deveps.dev}"
VPS="${SSH_VPS:-vps}"
SBC="${SSH_SBC:-sbc}"
# En Windows se usa el OpenSSH del sistema, que tiene las claves cargadas en el agente.
if [[ -x /c/Windows/System32/OpenSSH/ssh.exe ]]; then
  export PATH="/c/Windows/System32/OpenSSH:$PATH"
fi
# En Windows "python3" puede ser el acceso directo de la Microsoft Store, que no ejecuta nada.
PYTHON=""
for candidato in python python3; do
  if command -v "$candidato" >/dev/null && "$candidato" -c "import sys" >/dev/null 2>&1; then
    PYTHON="$candidato"
    break
  fi
done

fallo() { echo "ERROR: $*" >&2; exit 1; }
paso() { echo; echo "==> $*"; }

[[ -n "$PYTHON" ]] || fallo "hace falta Python en el PC"

# El Python de Windows escribe los saltos de línea como \r\n y Bash no quita el \r: sin esto
# "admin" llegaría como "admin\r" y la contraseña del bot llevaría un \r al final.
py() {
  "$PYTHON" "$@" | tr -d '\r'
}
command -v ssh >/dev/null || fallo "hace falta ssh"

preguntar() {          # preguntar VARIABLE "Pregunta" "valor por defecto"
  local respuesta
  read -r -p "$2${3:+ [$3]}: " respuesta
  printf -v "$1" '%s' "${respuesta:-$3}"
}

preguntar_secreto() {  # preguntar_secreto VARIABLE "Pregunta"
  local respuesta
  read -r -s -p "$2: " respuesta
  echo
  # Al pegar en el terminal de Windows puede colarse un \r al final.
  printf -v "$1" '%s' "${respuesta//$'\r'/}"
}

# systemd lee el .env de la API: entre comillas dobles no expande "$", pero sí interpreta
# comillas y barras. Se rechazan para no guardar una contraseña distinta de la escrita.
valida_para_env() {
  [[ "$1" != *'"'* && "$1" != *'\'* && "$1" != *$'\n'* ]]
}

# ----------------------------------------------------------------------------------------
paso "Datos del administrador (para entrar en el panel y en la app)"
preguntar ADMIN_NOMBRE_COMPLETO "Nombre completo" "Enrique Pérez Sánchez"
while true; do
  preguntar ADMIN_NOMBRE_USUARIO "Nombre de usuario (3-50 caracteres, sin espacios)" "kike"
  [[ "$ADMIN_NOMBRE_USUARIO" =~ ^[^[:space:]]{3,50}$ ]] && break
  echo "  Ha de tener entre 3 y 50 caracteres y ningún espacio."
done
while true; do
  preguntar ADMIN_EMAIL "Email" "deveps@deveps.dev"
  [[ "$ADMIN_EMAIL" =~ ^[^@[:space:]]+@[^@[:space:]]+\.[^@[:space:]]+$ ]] && break
  echo "  No parece un email válido."
done
while true; do
  preguntar_secreto ADMIN_PASSWORD "Contraseña (8-72 caracteres)"
  if (( ${#ADMIN_PASSWORD} < 8 || ${#ADMIN_PASSWORD} > 72 )); then
    echo "  Ha de tener entre 8 y 72 caracteres."; continue
  fi
  if ! valida_para_env "$ADMIN_PASSWORD"; then
    echo '  No puede contener comillas dobles (") ni barras invertidas (\).'; continue
  fi
  preguntar_secreto REPETIDA "Repite la contraseña"
  [[ "$ADMIN_PASSWORD" == "$REPETIDA" ]] && break
  echo "  No coinciden."
done
unset REPETIDA
valida_para_env "$ADMIN_NOMBRE_COMPLETO" || fallo 'el nombre no puede contener comillas dobles ni barras invertidas'

paso "Datos del bot del scraper"
preguntar BOT_USUARIO "Nombre de usuario del bot" "scraper"
preguntar BOT_EMAIL "Email del bot (no necesita existir como buzón)" "scraper@deveps.dev"
[[ "$BOT_USUARIO" =~ ^[A-Za-z0-9_.-]{3,50}$ ]] || fallo "el nombre de usuario del bot solo puede tener letras, números, _ . - (3-50)"
preguntar_secreto DEEPSEEK_API_KEY "Clave de la API de DeepSeek"
# Una clave nunca lleva espacios: se quitan los que se cuelen al pegarla.
DEEPSEEK_API_KEY="${DEEPSEEK_API_KEY//[[:space:]]/}"
[[ -n "$DEEPSEEK_API_KEY" ]] || fallo "la clave de DeepSeek es obligatoria"
[[ "$DEEPSEEK_API_KEY" == sk-* ]] || echo "  AVISO: las claves de DeepSeek suelen empezar por «sk-»; se comprobará en el paso 4."
preguntar DEEPSEEK_MODEL "Modelo de DeepSeek" "deepseek-flash"
# Contraseña aleatoria: solo la usa el scraper y queda guardada en su .env de la Orange Pi.
BOT_PASSWORD="$(py -c 'import secrets; print(secrets.token_urlsafe(24))')"

# ----------------------------------------------------------------------------------------
paso "1/4 Base de datos y API en el VPS"
ssh "$VPS" 'sudo bash -s' <<'REMOTO'
set -euo pipefail
ENV=/opt/apps/moteros/.env
for clave in DB_URL DB_USER DB_PASSWORD; do
  sudo grep -q "^${clave}=" "$ENV" || { echo "Falta ${clave} en $ENV" >&2; exit 1; }
done
echo "    Usuario de la BBDD: $(grep '^DB_USER=' "$ENV" | cut -d= -f2-) (contraseña guardada en $ENV, no hace falta tocarla)"
curl -fsS -o /dev/null http://127.0.0.1:8080/health && echo "    La API responde y está conectada a la base de datos."
echo "    Usuarios en la BBDD ahora: $(mysql -N -B -e 'SELECT COUNT(*) FROM moteros.usuarios')"
REMOTO

# ----------------------------------------------------------------------------------------
paso "2/4 Administrador"
{
  printf 'A_EMAIL=%q\nA_PASSWORD=%q\nA_USUARIO=%q\nA_NOMBRE=%q\n' \
    "$ADMIN_EMAIL" "$ADMIN_PASSWORD" "$ADMIN_NOMBRE_USUARIO" "$ADMIN_NOMBRE_COMPLETO"
  cat <<'REMOTO'
set -euo pipefail
ENV=/opt/apps/moteros/.env
TMP="$(mktemp)"
grep -vE '^ADMIN_(EMAIL|PASSWORD|NOMBRE_USUARIO|NOMBRE_COMPLETO)=' "$ENV" > "$TMP" || true
{
  printf 'ADMIN_EMAIL="%s"\n' "$A_EMAIL"
  printf 'ADMIN_PASSWORD="%s"\n' "$A_PASSWORD"
  printf 'ADMIN_NOMBRE_USUARIO="%s"\n' "$A_USUARIO"
  printf 'ADMIN_NOMBRE_COMPLETO="%s"\n' "$A_NOMBRE"
} >> "$TMP"
install -o ubuntu -g ubuntu -m 600 "$TMP" "$ENV"
rm -f "$TMP"

INICIO="$(date '+%Y-%m-%d %H:%M:%S')"
systemctl restart moteros
for i in $(seq 1 60); do
  curl -fsS -o /dev/null http://127.0.0.1:8080/health 2>/dev/null && break
  sleep 1
done
curl -fsS -o /dev/null http://127.0.0.1:8080/health || { journalctl -u moteros -n 40 --no-pager; exit 1; }
journalctl -u moteros --since "$INICIO" --no-pager | grep -E 'Administrador .* creado|ascendido a admin|no se crea el administrador' \
  | sed 's/.*AdminBootstrapRunner *: /    /' || echo "    La cuenta ya era administradora."
REMOTO
} | ssh "$VPS" 'sudo bash -s'

# Llamadas a la API desde el PC. Los datos van en variables de entorno del proceso de Python.
api_login() {  # api_login IDENTIFICADOR PASSWORD -> imprime "token uuid rol"
  API_URL="$API_URL" IDENT="$1" PASS="$2" py - <<'PY'
import json, os, sys, urllib.error, urllib.request
body = json.dumps({"identificador": os.environ["IDENT"], "password": os.environ["PASS"]}).encode()
req = urllib.request.Request(os.environ["API_URL"] + "/api/auth/login", data=body,
                             headers={"Content-Type": "application/json"}, method="POST")
try:
    data = json.load(urllib.request.urlopen(req, timeout=30))["data"]
except urllib.error.HTTPError as e:
    sys.exit(f"login rechazado ({e.code}): {e.read().decode(errors='replace')[:200]}")
print(data["token"], data["usuario"]["uuid"], data["usuario"]["rol"])
PY
}

read -r ADMIN_TOKEN _ ADMIN_ROL < <(api_login "$ADMIN_NOMBRE_USUARIO" "$ADMIN_PASSWORD") || true
[[ -n "${ADMIN_TOKEN:-}" ]] \
  || fallo "el administrador no puede entrar; revisa el log con: ssh $VPS 'sudo journalctl -u moteros -n 60'"
[[ "$ADMIN_ROL" == "admin" ]] || fallo "la cuenta $ADMIN_NOMBRE_USUARIO entra pero tiene rol $ADMIN_ROL"
echo "    $ADMIN_NOMBRE_USUARIO entra con rol admin."
# La cuenta ya existe: la contraseña no pinta nada en el servidor.
ssh "$VPS" "sudo sed -i '/^ADMIN_PASSWORD=/d' /opt/apps/moteros/.env"
echo "    ADMIN_PASSWORD borrada del .env del VPS."

# ----------------------------------------------------------------------------------------
paso "3/4 Cuenta del bot"
REGISTRO="$(API_URL="$API_URL" U="$BOT_USUARIO" E="$BOT_EMAIL" P="$BOT_PASSWORD" py - <<'PY'
import json, os, urllib.error, urllib.request
body = json.dumps({"nombreUsuario": os.environ["U"], "nombreCompleto": "Scraper de rutas",
                   "email": os.environ["E"], "password": os.environ["P"]}).encode()
req = urllib.request.Request(os.environ["API_URL"] + "/api/auth/registro", data=body,
                             headers={"Content-Type": "application/json"}, method="POST")
try:
    urllib.request.urlopen(req, timeout=30)
    print("creada")
except urllib.error.HTTPError as e:
    print("existe" if e.code == 409 else f"error {e.code}: {e.read().decode(errors='replace')[:200]}")
PY
)"
case "$REGISTRO" in
  creada) echo "    Cuenta $BOT_USUARIO creada con una contraseña aleatoria." ;;
  existe)
    # Su contraseña aleatoria solo existió durante la ejecución anterior. La API no permite
    # cambiar la contraseña de otra cuenta, así que se recrea (solo si es la cuenta del bot).
    echo "    La cuenta $BOT_USUARIO ya existe y su contraseña aleatoria no se conserva."
    preguntar RECREAR "¿Borrarla y crearla de nuevo con otra contraseña? Solo se hace si tiene rol scraper (s/N)" "N"
    [[ "$RECREAR" =~ ^[sS]$ ]] || fallo "no se puede continuar sin la contraseña del bot"
    BORRADAS="$(ssh "$VPS" "sudo mysql -N -B -e \"DELETE FROM moteros.usuarios WHERE nombre_usuario='$BOT_USUARIO' AND rol='scraper'; SELECT ROW_COUNT();\"" | tr -d '\r')"
    [[ "$BORRADAS" == "1" ]] || fallo "la cuenta $BOT_USUARIO no tiene rol scraper; no se ha borrado nada"
    REGISTRO="$(API_URL="$API_URL" U="$BOT_USUARIO" E="$BOT_EMAIL" P="$BOT_PASSWORD" py - <<'PY'
import json, os, urllib.error, urllib.request
body = json.dumps({"nombreUsuario": os.environ["U"], "nombreCompleto": "Scraper de rutas",
                   "email": os.environ["E"], "password": os.environ["P"]}).encode()
req = urllib.request.Request(os.environ["API_URL"] + "/api/auth/registro", data=body,
                             headers={"Content-Type": "application/json"}, method="POST")
try:
    urllib.request.urlopen(req, timeout=30)
    print("creada")
except urllib.error.HTTPError as e:
    print(f"error {e.code}: {e.read().decode(errors='replace')[:200]}")
PY
)"
    [[ "$REGISTRO" == "creada" ]] || fallo "no se ha podido recrear la cuenta del bot: $REGISTRO"
    echo "    Cuenta $BOT_USUARIO recreada con una contraseña aleatoria nueva."
    ;;
  *) fallo "no se ha podido crear la cuenta del bot: $REGISTRO" ;;
esac

read -r _ BOT_UUID BOT_ROL < <(api_login "$BOT_USUARIO" "$BOT_PASSWORD") || true
[[ -n "${BOT_UUID:-}" ]] || fallo "la cuenta $BOT_USUARIO no puede entrar (¿contraseña incorrecta?)"
if [[ "$BOT_ROL" != "scraper" ]]; then
  API_URL="$API_URL" TOKEN="$ADMIN_TOKEN" UUID="$BOT_UUID" py - <<'PY'
import os, urllib.request
url = f"{os.environ['API_URL']}/api/admin/usuarios/{os.environ['UUID']}/rol?rol=scraper"
req = urllib.request.Request(url, headers={"Authorization": "Bearer " + os.environ["TOKEN"]}, method="PATCH")
urllib.request.urlopen(req, timeout=30)
PY
fi
echo "    $BOT_USUARIO tiene rol scraper."

# ----------------------------------------------------------------------------------------
paso "4/4 Configuración del scraper en la Orange Pi"
# configure.py ya sabe escribir el .env con permisos 600 y comprobar la cuenta y la clave:
# se reutiliza leyendo los valores de la entrada estándar.
API_URL="$API_URL" U="$BOT_USUARIO" P="$BOT_PASSWORD" K="$DEEPSEEK_API_KEY" M="$DEEPSEEK_MODEL" py -c '
import json, os
print(json.dumps({"MOTEROS_API_URL": os.environ["API_URL"], "MOTEROS_USUARIO": os.environ["U"],
  "MOTEROS_PASSWORD": os.environ["P"], "DEEPSEEK_API_KEY": os.environ["K"],
  "DEEPSEEK_MODEL": os.environ["M"], "REQUEST_DELAY_SECONDS": "3"}))' \
| ssh "$SBC" 'SCRAPER_HOME=/opt/apps/moteros-scraper python3 -c "
import json, sys
sys.path.insert(0, \"/opt/apps/moteros-src/scraper/scripts\")
import configure as c
valores = json.load(sys.stdin)
for clave, valor in c.read_env(c.ENV_FILE).items():
    valores.setdefault(clave, valor)
def comprobar(nombre, funcion, *args):
    # Cualquier error inesperado se muestra como fallo: el .env se guarda igualmente.
    try:
        return (nombre, *funcion(*args))
    except Exception as error:
        return (nombre, False, type(error).__name__ + \": \" + str(error))
comprobaciones = [
    comprobar(\"API de moter@s\", c.check_moteros, valores[\"MOTEROS_API_URL\"], valores[\"MOTEROS_USUARIO\"], valores[\"MOTEROS_PASSWORD\"]),
    comprobar(\"DeepSeek\", c.check_deepseek, valores[\"DEEPSEEK_API_KEY\"], valores[\"DEEPSEEK_MODEL\"]),
]
for nombre, ok, mensaje in comprobaciones:
    print(\"    \" + (\"OK   \" if ok else \"FALLO\") + \" \" + nombre + \": \" + mensaje)
c.write_env(c.ENV_FILE, valores)
print(\"    Guardado \" + str(c.ENV_FILE) + \" con permisos 600.\")
sys.exit(0 if all(ok for _, ok, _ in comprobaciones) else 3)
"' || echo "    AVISO: alguna comprobación ha fallado (arriba el motivo). El .env se ha guardado igualmente."

unset ADMIN_PASSWORD BOT_PASSWORD DEEPSEEK_API_KEY ADMIN_TOKEN

echo
echo "Listo."
echo "  Panel:  $API_URL/admin/  (entra con $ADMIN_NOMBRE_USUARIO o $ADMIN_EMAIL)"
echo "  Prueba del scraper sin enviar nada a la bandeja:"
echo "    ssh $SBC 'cd /opt/apps/moteros-src/scraper && SCRAPER_HOME=/opt/apps/moteros-scraper /opt/apps/moteros-scraper/venv/bin/python main.py --dry-run --source burgos'"
