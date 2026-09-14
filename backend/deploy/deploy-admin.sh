#!/usr/bin/env bash
#
# Despliegue del panel web de administración de moter@s (Angular) en el VPS.
# Compila en local, sube los estáticos a /var/www/moteros-admin y, si el vhost de Nginx
# (gestionado por Certbot) todavía no sirve /admin/, le añade los dos bloques "location".
#
# Uso (desde la raíz del repositorio):   SSH_HOST=vps bash backend/deploy/deploy-admin.sh
#
set -euo pipefail

SSH_HOST="${SSH_HOST:-vps}"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ADMIN_DIR="$REPO_ROOT/admin"
BUILD_DIR="$ADMIN_DIR/dist/admin/browser"
PAQUETE="$(mktemp -d)/moteros-admin.tar.gz"

echo "==> Build del panel"
(cd "$ADMIN_DIR" && npm ci --no-audit --no-fund --loglevel=error && npx ng build)
[[ -f "$BUILD_DIR/index.html" ]] || { echo "ERROR: no se generó $BUILD_DIR/index.html" >&2; exit 1; }
tar -czf "$PAQUETE" -C "$BUILD_DIR" .

echo "==> Subida a $SSH_HOST"
scp "$PAQUETE" "$SSH_HOST:/tmp/moteros-admin.tar.gz"
rm -f "$PAQUETE"

echo "==> Instalación y Nginx"
ssh "$SSH_HOST" 'sudo bash -s' <<'REMOTO'
set -euo pipefail
WEB_DIR=/var/www/moteros-admin
SITE=/etc/nginx/sites-available/moteros-api

install -d -o www-data -g www-data -m 755 "$WEB_DIR"
find "$WEB_DIR" -mindepth 1 -delete
tar -xzf /tmp/moteros-admin.tar.gz -C "$WEB_DIR"
chown -R www-data:www-data "$WEB_DIR"
rm -f /tmp/moteros-admin.tar.gz

if ! grep -q "location /admin/" "$SITE"; then
  cp "$SITE" "$SITE.bak-$(date +%Y%m%d%H%M%S)"
  # Se insertan antes del primer "location / {" (el del proxy, dentro del server 443 de Certbot).
  python3 - "$SITE" <<'PY'
import sys
ruta = sys.argv[1]
texto = open(ruta, encoding="utf-8").read()
bloque = """    # Panel web de administracion (estaticos en /var/www/moteros-admin, ver deploy-admin.sh).
    location = /admin {
        return 301 /admin/;
    }

    location /admin/ {
        alias /var/www/moteros-admin/;
        try_files $uri $uri/ /admin/index.html;
        add_header X-Robots-Tag "noindex, nofollow" always;
    }

"""
marca = "    location / {"
if marca not in texto:
    sys.exit("No se encontró 'location / {' en " + ruta)
open(ruta, "w", encoding="utf-8").write(texto.replace(marca, bloque + marca, 1))
PY
  echo "    Añadidos los bloques /admin/ al vhost (copia de seguridad junto al fichero)."
fi

nginx -t
systemctl reload nginx
echo "    Panel instalado en $WEB_DIR"
REMOTO

echo "==> Hecho: https://moteros.deveps.dev/admin/"
