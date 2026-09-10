#!/usr/bin/env bash
#
# Provisiona la API moter@s en el VPS (Ubuntu 24.04, Nginx, MySQL 8, Certbot, Java 25).
# Sigue la convencion del resto de apps del VPS: /opt/apps/<app>/, servicio como usuario 'ubuntu'.
# Idempotente. NO borra la base de datos si ya tiene tablas.
#
# Uso (en el VPS):
#   sudo bash /tmp/moteros-deploy/setup-vps.sh moteros.deveps.dev
# Requiere en /tmp:  moteros.jar  y  moteros.sql
#
set -euo pipefail

DOMAIN="${1:-}"
EMAIL="${CERTBOT_EMAIL:-devepsdev@gmail.com}"
SVC_USER=ubuntu
APP_DIR=/opt/apps/moteros
ENV_FILE="$APP_DIR/.env"
UPLOADS_DIR="$APP_DIR/uploads"
SRC_JAR=/tmp/moteros.jar
SRC_SQL=/tmp/moteros.sql
DEPLOY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

[[ -n "$DOMAIN" ]]     || { echo "ERROR: uso: sudo bash setup-vps.sh <dominio>" >&2; exit 1; }
[[ $EUID -eq 0 ]]      || { echo "ERROR: ejecuta con sudo." >&2; exit 1; }
[[ -f "$SRC_JAR" ]]    || { echo "ERROR: falta $SRC_JAR" >&2; exit 1; }
[[ -f "$SRC_SQL" ]]    || { echo "ERROR: falta $SRC_SQL" >&2; exit 1; }

echo "==> Dominio: $DOMAIN   ·   App: $APP_DIR   ·   Servicio como: $SVC_USER"

# ----------------------------------------------------------------------------
echo "==> [1/8] Comprobaciones"
command -v java >/dev/null || { echo "ERROR: java no esta en PATH" >&2; exit 1; }
java -version
dpkg -s python3-certbot-nginx >/dev/null 2>&1 || { apt-get update -qq && apt-get install -y -qq python3-certbot-nginx; }

# ----------------------------------------------------------------------------
echo "==> [2/8] Directorios"
install -d -o "$SVC_USER" -g "$SVC_USER" -m 755 "$APP_DIR" "$UPLOADS_DIR"

# ----------------------------------------------------------------------------
echo "==> [3/8] Base de datos MySQL"
DB_TABLES=$(mysql -N -B -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='moteros';")
if [[ "$DB_TABLES" -gt 0 ]]; then
  echo "    La BBDD 'moteros' ya tiene $DB_TABLES tablas. No se toca el esquema."
else
  echo "    Cargando esquema desde $SRC_SQL ..."
  mysql < "$SRC_SQL"
fi

# ----------------------------------------------------------------------------
echo "==> [4/8] Usuario de BBDD de la aplicacion"
if [[ -f "$ENV_FILE" ]] && grep -q '^DB_PASSWORD=' "$ENV_FILE"; then
  DB_PASSWORD=$(grep '^DB_PASSWORD=' "$ENV_FILE" | cut -d= -f2-)
  echo "    Reutilizando DB_PASSWORD del .env existente."
else
  DB_PASSWORD=$(openssl rand -base64 24)
  echo "    Generada DB_PASSWORD nueva."
fi
mysql <<SQL
CREATE USER IF NOT EXISTS 'moteros'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';
ALTER USER 'moteros'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';
GRANT SELECT, INSERT, UPDATE, DELETE ON moteros.* TO 'moteros'@'localhost';
FLUSH PRIVILEGES;
SQL

# ----------------------------------------------------------------------------
echo "==> [5/8] Fichero de entorno $ENV_FILE"
if [[ -f "$ENV_FILE" ]] && grep -q '^JWT_SECRET=' "$ENV_FILE"; then
  JWT_SECRET=$(grep '^JWT_SECRET=' "$ENV_FILE" | cut -d= -f2-)
else
  JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')
fi
umask 077
cat > "$ENV_FILE" <<ENV
DB_URL=jdbc:mysql://localhost:3306/moteros?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Europe/Madrid
DB_USER=moteros
DB_PASSWORD=${DB_PASSWORD}
JWT_SECRET=${JWT_SECRET}
JWT_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=2592000000
CORS_ALLOWED_ORIGINS=https://${DOMAIN}
UPLOADS_DIR=${UPLOADS_DIR}
ENV
chown "$SVC_USER":"$SVC_USER" "$ENV_FILE"
chmod 600 "$ENV_FILE"

# ----------------------------------------------------------------------------
echo "==> [6/8] Binario y servicio systemd"
install -o "$SVC_USER" -g "$SVC_USER" -m 640 "$SRC_JAR" "$APP_DIR/moteros.jar"
install -o root -g root -m 644 "$DEPLOY_DIR/moteros.service" /etc/systemd/system/moteros.service
systemctl daemon-reload
systemctl enable moteros >/dev/null
systemctl restart moteros

echo "    Esperando arranque..."
UP=0
for i in $(seq 1 40); do
  if curl -fsS -o /dev/null "http://127.0.0.1:8080/health"; then UP=1; echo "    API arriba (${i}s)"; break; fi
  sleep 1
done
if [[ "$UP" -ne 1 ]]; then echo "ERROR: la API no responde. Ultimos logs:"; journalctl -u moteros -n 50 --no-pager; exit 1; fi

# ----------------------------------------------------------------------------
echo "==> [7/8] Nginx (vhost HTTP)"
if [[ -f /etc/nginx/sites-available/moteros-api ]]; then
  # Ya existe: certbot lo gestiona (bloque 443). No lo sobrescribimos.
  echo "    El vhost ya existe; no se toca (lo gestiona certbot)."
else
  sed "s/__DOMAIN__/${DOMAIN}/g" "$DEPLOY_DIR/nginx-moteros-api.conf" > /etc/nginx/sites-available/moteros-api
  ln -sfn /etc/nginx/sites-available/moteros-api /etc/nginx/sites-enabled/moteros-api
  nginx -t
  systemctl reload nginx
fi

# ----------------------------------------------------------------------------
echo "==> [8/8] Certificado SSL (Let's Encrypt)"
if [[ ! -d "/etc/letsencrypt/live/${DOMAIN}" ]]; then
  certbot --nginx -d "${DOMAIN}" --non-interactive --agree-tos -m "${EMAIL}" --redirect
else
  echo "    Ya existe certificado para ${DOMAIN}."
fi

# ----------------------------------------------------------------------------
echo
echo "================  RESUMEN  ================"
systemctl --no-pager --property=ActiveState,SubState,MainPID show moteros | sed 's/^/  /'
echo "  Local  : http://127.0.0.1:8080/health"
echo "  Publico: https://${DOMAIN}/health"
echo "  Servicio : systemctl {status|restart|stop} moteros   ·   journalctl -u moteros -f"
echo "  Entorno  : $ENV_FILE"
echo "  Uploads  : $UPLOADS_DIR"
echo "=========================================="
