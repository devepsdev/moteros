#!/usr/bin/env bash
#
# Redespliegue de la API moter@s (build + subida del jar + reinicio).
# Requiere que setup-vps.sh ya se haya ejecutado una vez en el VPS.
#
# Uso (desde la carpeta backend/):   SSH_HOST=vps bash deploy/deploy.sh
#
set -euo pipefail

SSH_HOST="${SSH_HOST:-vps}"
JAR="target/moteros-0.0.1-SNAPSHOT.jar"

echo "==> Build"
./mvnw -q clean package -DskipTests
[[ -f "$JAR" ]] || { echo "ERROR: no se genero $JAR" >&2; exit 1; }
echo "    $(du -h "$JAR" | cut -f1)  $JAR"

echo "==> Subida a $SSH_HOST"
scp "$JAR" "$SSH_HOST:/tmp/moteros.jar"

echo "==> Instalacion y reinicio"
ssh "$SSH_HOST" '
  set -e
  sudo install -o ubuntu -g ubuntu -m 640 /tmp/moteros.jar /opt/apps/moteros/moteros.jar
  rm -f /tmp/moteros.jar
  sudo systemctl restart moteros
  for i in $(seq 1 40); do
    curl -fsS -o /dev/null http://127.0.0.1:8080/health && { echo "OK arranque (${i}s)"; break; }
    sleep 1
  done
  systemctl is-active --quiet moteros && echo "servicio activo" || { sudo journalctl -u moteros -n 40 --no-pager; exit 1; }
'
echo "==> Hecho."
