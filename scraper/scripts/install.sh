#!/usr/bin/env bash
# Instala o actualiza el scraper de moter@s en la Orange Pi, sin necesitar sudo.
#
#   bash /opt/apps/moteros-src/scraper/scripts/install.sh
#
# Deja el código en /opt/apps/moteros-src (clon del repositorio) y los datos en
# /opt/apps/moteros-scraper: entorno virtual, .env (600), estado y logs. La pasada
# semanal se programa en el crontab del usuario que lo ejecuta.
#
# /opt/apps pertenece a root: la primera vez hay que crear las dos carpetas con
#   sudo install -d -o "$USER" -g "$USER" /opt/apps/moteros-src /opt/apps/moteros-scraper
set -euo pipefail

REPO_URL="https://github.com/devepsdev/moteros.git"
SRC_DIR="/opt/apps/moteros-src"
HOME_DIR="/opt/apps/moteros-scraper"
RUN_USER="$(id -un)"
CRON_TAG="# moteros-scraper"
# Lunes a las 06:30: media hora después del de rastrix, para no coincidir en la misma
# placa ni en las llamadas a DeepSeek.
CRON_LINE="30 6 * * 1 $SRC_DIR/scraper/scripts/run_weekly.sh $CRON_TAG"

for dir in "$SRC_DIR" "$HOME_DIR"; do
    if [[ ! -d "$dir" ]] && ! mkdir -p "$dir" 2>/dev/null; then
        echo "No se puede crear $dir. Créalo una vez con:" >&2
        echo "  sudo install -d -o $RUN_USER -g $RUN_USER $SRC_DIR $HOME_DIR" >&2
        exit 1
    fi
    if [[ ! -w "$dir" ]]; then
        echo "$dir existe pero $RUN_USER no puede escribir en él. Arréglalo con:" >&2
        echo "  sudo chown $RUN_USER:$RUN_USER $dir" >&2
        exit 1
    fi
done

if [[ -d "$SRC_DIR/.git" ]]; then
    echo "Actualizando el repositorio en $SRC_DIR..."
    git -C "$SRC_DIR" pull --ff-only --quiet
else
    echo "Clonando el repositorio en $SRC_DIR..."
    git clone --quiet "$REPO_URL" "$SRC_DIR"
fi

mkdir -p "$HOME_DIR/data" "$HOME_DIR/logs"

if [[ ! -d "$HOME_DIR/venv" ]]; then
    echo "Creando el entorno virtual..."
    python3 -m venv "$HOME_DIR/venv"
fi
"$HOME_DIR/venv/bin/pip" install --quiet --upgrade pip
"$HOME_DIR/venv/bin/pip" install --quiet -r "$SRC_DIR/scraper/requirements.txt"
chmod +x "$SRC_DIR/scraper/scripts/run_weekly.sh"

if [[ -f "$HOME_DIR/.env" ]]; then
    # Contiene la contraseña del bot y la clave de DeepSeek: solo para su dueño.
    chmod 600 "$HOME_DIR/.env"
fi

# Se sustituye la línea anterior en vez de añadir otra en cada instalación.
{ crontab -l 2>/dev/null | grep -v "$CRON_TAG" || true; echo "$CRON_LINE"; } | crontab -
echo "Pasada semanal programada en el crontab de $RUN_USER (lunes 06:30)."

echo "Scraper instalado."
if [[ ! -f "$HOME_DIR/.env" ]]; then
    echo "Falta la configuración. Créala con:"
    echo "  python3 $SRC_DIR/scraper/scripts/configure.py"
else
    echo "Prueba sin enviar nada con:"
    echo "  cd $SRC_DIR/scraper && SCRAPER_HOME=$HOME_DIR $HOME_DIR/venv/bin/python main.py --dry-run"
fi
