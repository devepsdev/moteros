#!/usr/bin/env bash
#
# Provisiona la API moter@s en el VPS (Ubuntu 24.04, Nginx, MySQL 8, Certbot, Java 25).
# Idempotente: se puede re-ejecutar. NO borra la base de datos si ya tiene tablas.
#
# Uso (en el VPS, como usuario con sudo):
#   1) sube los artefactos:   scp target/moteros-0.0.1-SNAPSHOT.jar vps:/tmp/moteros.jar
#                             scp db/moteros.sql                     vps:/tmp/moteros.sql
#                             scp backend/deploy/*                   vps:/tmp/moteros-deploy/
#   2) sudo bash /tmp/moteros-deploy/setup-vps.sh moteros.deveps.dev
#
set -euo pipefail

DOMAIN="${1:-}"
EMAIL="${CERTBOT_EMAIL:-devepsdev@gmail.com}"
APP_DIR=/opt/moteros
ENV_DIR=/etc/moteros
DATA_DIR=/var/lib/moteros
SRC_JAR=/tmp/moteros.jar
SRC_SQL=/tmp/moteros.sql
DEPLOY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [[ -z "$DOMAIN" ]]; then
  echo "ERROR: falta el dominio.  Uso: sudo bash setup-vps.sh <dominio>" >&2
  exit 1
fi
if [[ $EUID -ne 0 ]]; then echo "ERROR: ejecuta con sudo." >&2; exit 1; fi
[[ -f "$SRC_JAR" ]] || { echo "ERROR: no existe $SRC_JAR" >&2; exit 1; }
[[ -f "$SRC_SQL" ]] || { echo "ERROR: no existe $SRC_SQL" >&2; exit 1; }

echo "==> Dominio: $DOMAIN"

# ----------------------------------------------------------------------------
echo "==> [1/9] Paquetes (certbot nginx plugin)"
if ! dpkg -s python3-certbot-nginx >/dev/null 2>&1; then
  apt-get update -qq && apt-get install -y -qq python3-certbot-nginx
fi
command -v java >/dev/null || { echo "ERROR: java no esta en PATH" >&2; exit 1; }
java -version

# ----------------------------------------------------------------------------
echo "==> [2/9] Usuario de sistema y directorios"
id moteros >/dev/null 2>&1 || useradd --system --home "$APP_DIR" --shell /usr/sbin/nologin moteros
install -d -o moteros -g moteros -m 755 "$APP_DIR"
install -d -o moteros -g moteros -m 750 "$DATA_DIR" "$DATA_DIR/uploads"
install -d -o root    -g moteros -m 750 "$ENV_DIR"

# ----------------------------------------------------------------------------
echo "==> [3/9] Base de datos MySQL"
DB_EXISTS=$(mysql -N -B -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='moteros';")
if [[ "$DB_EXISTS" -gt 0 ]]; then
  echo "    La BBDD 'moteros' ya tiene $DB_EXISTS tablas. No se toca el esquema."
else
  echo "    Cargando esquema desde $SRC_SQL ..."
  mysql < "$SRC_SQL"
fi

# ----------------------------------------------------------------------------
echo "==> [4/9] Usuario de BBDD de la aplicacion"
if [[ -f "$ENV_DIR/moteros.env" ]] && grep -q '^DB_PASSWORD=' "$ENV_DIR/moteros.env"; then
  DB_PASSWORD=$(grep '^DB_PASSWORD=' "$ENV_DIR/moteros.env" | cut -d= -f2-)
  echo "    Reutilizando DB_PASSWORD existente."
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
echo "==> [5/9] Fichero de entorno /etc/moteros/moteros.env"
if [[ -f "$ENV_DIR/moteros.env" ]] && grep -q '^JWT_SECRET=' "$ENV_DIR/moteros.env"; then
  JWT_SECRET=$(grep '^JWT_SECRET=' "$ENV_DIR/moteros.env" | cut -d= -f2-)
else
  JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')
fi
umask 077
cat > "$ENV_DIR/moteros.env" <<ENV
DB_URL=jdbc:mysql://localhost:3306/moteros?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Europe/Madrid
DB_USER=moteros
DB_PASSWORD=${DB_PASSWORD}
JWT_SECRET=${JWT_SECRET}
JWT_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=2592000000
CORS_ALLOWED_ORIGINS=https://${DOMAIN}
UPLOADS_DIR=${DATA_DIR}/uploads
JAVA_OPTS=-Xms256m -Xmx768m -XX:+UseSerialGC -Djava.security.egd=file:/dev/urandom
SERVER_PORT=8080
ENV
chown root:moteros "$ENV_DIR/moteros.env"
chmod 640 "$ENV_DIR/moteros.env"

# ----------------------------------------------------------------------------
echo "==> [6/9] Binario y servicio systemd"
install -o moteros -g moteros -m 640 "$SRC_JAR" "$APP_DIR/moteros.jar"
install -o root -g root -m 644 "$DEPLOY_DIR/moteros.service" /etc/systemd/system/moteros.service
systemctl daemon-reload
systemctl enable moteros
systemctl restart moteros

echo "    Esperando arranque..."
for i in $(seq 1 30); do
  if curl -fsS -o /dev/null "http://127.0.0.1:8080/v3/api-docs"; then echo "    API arriba (${i}s)"; break; fi
  sleep 1
done
systemctl is-active --quiet moteros || { journalctl -u moteros -n 40 --no-pager; exit 1; }

# ----------------------------------------------------------------------------
echo "==> [7/9] Nginx (vhost HTTP)"
sed "s/__DOMAIN__/${DOMAIN}/g" "$DEPLOY_DIR/nginx-moteros-api.conf" > /etc/nginx/sites-available/moteros-api
ln -sfn /etc/nginx/sites-available/moteros-api /etc/nginx/sites-enabled/moteros-api
nginx -t
systemctl reload nginx

# ----------------------------------------------------------------------------
echo "==> [8/9] Certificado SSL (Let's Encrypt)"
if [[ ! -d "/etc/letsencrypt/live/${DOMAIN}" ]]; then
  certbot --nginx -d "${DOMAIN}" --non-interactive --agree-tos -m "${EMAIL}" --redirect
else
  echo "    Ya existe certificado para ${DOMAIN}."
fi
certbot renew --dry-run

# ----------------------------------------------------------------------------
echo "==> [9/9] Resumen"
systemctl --no-pager status moteros | head -n 6
echo
echo "  API local : http://127.0.0.1:8080/v3/api-docs"
echo "  API publica: https://${DOMAIN}/v3/api-docs"
echo "  Servicio  : systemctl {status|restart|stop} moteros"
echo "  Logs      : journalctl -u moteros -f"
echo "  Entorno   : ${ENV_DIR}/moteros.env"
echo "  Uploads   : ${DATA_DIR}/uploads"
echo
echo "  IMPORTANTE: el registro DNS de ${DOMAIN} debe apuntar a este VPS (A -> 51.77.158.216)."
