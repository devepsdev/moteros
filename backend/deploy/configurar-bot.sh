#!/usr/bin/env bash
#
# Crea (o rehace) la cuenta del bot del scraper de moter@s. Se ejecuta en el VPS:
#
#   sudo bash /opt/apps/moteros/configurar-bot.sh
#
# Hace lo mismo que en rastrix se hizo registrando el bot desde la app y cambiándole el rol
# en el panel: pide nombre de usuario, email y una contraseña que eliges tú, registra la
# cuenta en la API local y le pone el rol scraper. Si ya existe una cuenta con ese nombre y
# rol scraper, ofrece borrarla y crearla de nuevo (por ejemplo, si se perdió su contraseña).
#
# Después, en la Orange Pi, configure.py pedirá ese mismo usuario y contraseña y la clave
# de DeepSeek:
#
#   python3 /opt/apps/moteros-src/scraper/scripts/configure.py
#
set -euo pipefail

API="http://127.0.0.1:8080"

fallo() { echo "ERROR: $*" >&2; exit 1; }

[[ $EUID -eq 0 ]] || fallo "ejecútalo con sudo: sudo bash $0"
for comando in mysql curl python3; do
  command -v "$comando" >/dev/null || fallo "falta $comando"
done
curl -fsS -o /dev/null "$API/health" || fallo "la API de moter@s no responde en $API (sudo systemctl status moteros)"

echo "== Cuenta del bot del scraper =="
echo

while true; do
  read -r -p "Nombre de usuario del bot [scraper]: " BOT_USUARIO
  BOT_USUARIO="${BOT_USUARIO:-scraper}"
  [[ "$BOT_USUARIO" =~ ^[A-Za-z0-9_.-]{3,50}$ ]] && break
  echo "  Solo letras, números, _ . - y entre 3 y 50 caracteres."
done

while true; do
  read -r -p "Email del bot (no hace falta que exista el buzón) [scraper@deveps.dev]: " BOT_EMAIL
  BOT_EMAIL="${BOT_EMAIL:-scraper@deveps.dev}"
  [[ "$BOT_EMAIL" =~ ^[^@[:space:]]+@[^@[:space:]]+\.[^@[:space:]]+$ ]] && break
  echo "  No parece un email válido."
done

while true; do
  read -r -s -p "Contraseña del bot (8-72 caracteres; apúntala, la pedirá configure.py): " BOT_PASSWORD
  echo
  if (( ${#BOT_PASSWORD} < 8 || ${#BOT_PASSWORD} > 72 )); then
    echo "  Ha de tener entre 8 y 72 caracteres."
    continue
  fi
  read -r -s -p "Repite la contraseña: " REPETIDA
  echo
  [[ "$BOT_PASSWORD" == "$REPETIDA" ]] && break
  echo "  No coinciden."
done
unset REPETIDA

# nombre_usuario ya está validado con la expresión de arriba: se puede usar en la consulta.
consulta() { mysql -N -B moteros -e "$1"; }

echo
ROL_ACTUAL="$(consulta "SELECT rol FROM usuarios WHERE nombre_usuario = '$BOT_USUARIO'")"
if [[ -n "$ROL_ACTUAL" ]]; then
  [[ "$ROL_ACTUAL" == "scraper" ]] \
    || fallo "ya existe una cuenta «$BOT_USUARIO» con rol $ROL_ACTUAL; elige otro nombre para el bot"
  read -r -p "Ya existe la cuenta «$BOT_USUARIO» (rol scraper). ¿Borrarla y crearla de nuevo con esta contraseña? [s/N]: " RECREAR
  [[ "$RECREAR" =~ ^[sS]$ ]] || fallo "no se ha cambiado nada"
  consulta "DELETE FROM usuarios WHERE nombre_usuario = '$BOT_USUARIO' AND rol = 'scraper'"
  echo "  Cuenta anterior borrada."
fi

# El cuerpo JSON se construye con Python leyendo variables de entorno: la contraseña no
# aparece en la línea de comandos (visible en ps) y cualquier carácter queda bien escapado.
export BOT_USUARIO BOT_EMAIL BOT_PASSWORD
cuerpo_json() {
  python3 -c "import json, os; print(json.dumps($1))"
}

RESPUESTA="$(cuerpo_json '{"nombreUsuario": os.environ["BOT_USUARIO"], "nombreCompleto": "Scraper de rutas", "email": os.environ["BOT_EMAIL"], "password": os.environ["BOT_PASSWORD"]}' \
  | curl -sS -o /tmp/moteros-registro-bot.json -w '%{http_code}' -H 'Content-Type: application/json' --data-binary @- "$API/api/auth/registro")"
if [[ "$RESPUESTA" != "201" ]]; then
  MENSAJE="$(python3 -c 'import json; print(json.load(open("/tmp/moteros-registro-bot.json")).get("message", ""))' 2>/dev/null || true)"
  rm -f /tmp/moteros-registro-bot.json
  fallo "la API no ha creado la cuenta ($RESPUESTA): $MENSAJE"
fi
rm -f /tmp/moteros-registro-bot.json

consulta "UPDATE usuarios SET rol = 'scraper', activo = TRUE WHERE nombre_usuario = '$BOT_USUARIO'"

# Comprobación final igual que la que hará el scraper: entrar y leer el rol.
ROL_LOGIN="$(cuerpo_json '{"identificador": os.environ["BOT_USUARIO"], "password": os.environ["BOT_PASSWORD"]}' \
  | curl -sS -H 'Content-Type: application/json' --data-binary @- "$API/api/auth/login" \
  | python3 -c 'import json, sys; print(json.load(sys.stdin).get("data", {}).get("usuario", {}).get("rol", ""))' 2>/dev/null || true)"
unset BOT_PASSWORD

[[ "$ROL_LOGIN" == "scraper" ]] || fallo "la cuenta se ha creado pero no entra con rol scraper (rol: ${ROL_LOGIN:-sin respuesta})"

echo "  Cuenta «$BOT_USUARIO» creada: entra y tiene rol scraper."
echo
echo "Siguiente paso, en la Orange Pi (con el mismo usuario y contraseña y tu clave de DeepSeek):"
echo "  python3 /opt/apps/moteros-src/scraper/scripts/configure.py"
