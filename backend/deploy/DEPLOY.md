# Despliegue de la API moter@s en el VPS

VPS OVHcloud · Ubuntu 24.04 · Nginx 1.24 · MySQL 8 · Certbot 2.9 · Java 25 (Oracle) · UFW (22/80/443)
SSH: `ssh vps` (`~/.ssh/config`: puerto 3573, usuario `ubuntu`, clave `id_ed25519_vps` — con passphrase, cargar en ssh-agent).

Convención del VPS (igual que `pedidai`, `rastrix`, `orderflow`): cada app en `/opt/apps/<app>/`,
servicio systemd corriendo como **`ubuntu`**, `.env` dentro de la carpeta de la app.

```
Android (Expo)  ──HTTPS──►  Cloudflare (DNS only)  ──►  Nginx :443  ──proxy──►  127.0.0.1:8080
                                                                                     │
                                              Spring Boot (systemd: moteros.service, perfil prod)
                                                     │
                                        MySQL 8 (localhost)   ·   /opt/apps/moteros/uploads
```

Rutas en el VPS:

| Ruta | Contenido |
|---|---|
| `/opt/apps/moteros/moteros.jar` | binario |
| `/opt/apps/moteros/.env` | secretos (chmod 600, `ubuntu:ubuntu`) |
| `/opt/apps/moteros/uploads/` | imágenes subidas (servidas por Spring en `/uploads/**`) |
| `/etc/systemd/system/moteros.service` | unit |
| `/etc/nginx/sites-available/moteros-api` | vhost (+ symlink en `sites-enabled`) |
| `/etc/letsencrypt/live/moteros.deveps.dev/` | certificado |

---

## 0. Requisitos previos (una sola vez)

1. **DNS** — Cloudflare: registro **A** `moteros` → `51.77.158.216`, **DNS only** (nube gris).
2. **Clave SSH en el agente de Windows** (la clave tiene passphrase):
   ```powershell
   Set-Service ssh-agent -StartupType Automatic
   Start-Service ssh-agent
   ssh-add $env:USERPROFILE\.ssh\id_ed25519_vps
   ```

## 1. Build + subida

```bash
cd backend
./mvnw clean package -DskipTests            # genera target/moteros-0.0.1-SNAPSHOT.jar (~64 MB)
cd ..
ssh vps 'mkdir -p /tmp/moteros-deploy'
scp backend/target/moteros-0.0.1-SNAPSHOT.jar  vps:/tmp/moteros.jar
scp backend/deploy/*                            vps:/tmp/moteros-deploy/
```

## 2. Provisionar (una sola vez)

```bash
ssh vps 'sudo bash /tmp/moteros-deploy/setup-vps.sh moteros.deveps.dev'
```

`setup-vps.sh` (idempotente, **no borra la BBDD si ya tiene tablas**):

| Paso | Qué hace |
|---|---|
| 1 | Comprueba `java`; instala `python3-certbot-nginx` si falta |
| 2 | Crea `/opt/apps/moteros/` y `/opt/apps/moteros/uploads/` (`ubuntu:ubuntu`) |
| 3 | Crea la BBDD `moteros` vacía si no existe (las tablas las crea Flyway al arrancar) |
| 4 | Crea `moteros'@'localhost` con todos los permisos sobre `moteros.*` (Flyway necesita DDL) y contraseña aleatoria |
| 5 | Escribe `/opt/apps/moteros/.env` (600) con `DB_PASSWORD` y `JWT_SECRET` (`openssl rand -base64 64`) generados, CORS acotado al dominio |
| 6 | Instala el jar, el unit `moteros.service`, `enable --now`, y espera el healthcheck local |
| 7 | Vhost Nginx → `nginx -t` → reload |
| 8 | `certbot --nginx -d moteros.deveps.dev --redirect` |

## 3. Verificar

```bash
ssh vps 'systemctl status moteros --no-pager | head; curl -s -o /dev/null -w "local %{http_code}\n" http://127.0.0.1:8080/health'
curl -s -o /dev/null -w "publico %{http_code}\n" https://moteros.deveps.dev/health
# primer usuario -> queda como ADMIN
curl -s -X POST https://moteros.deveps.dev/api/auth/registro -H 'Content-Type: application/json' \
  -d '{"nombreUsuario":"admin","nombreCompleto":"Admin","email":"TU_EMAIL","password":"CAMBIA_ESTO"}'
```

## 4. Redespliegues

```bash
cd backend && SSH_HOST=vps bash deploy/deploy.sh   # build + scp jar + systemctl restart
```

---

## Operación

| Acción | Comando |
|---|---|
| Estado / logs | `systemctl status moteros` · `journalctl -u moteros -f` |
| Reiniciar | `sudo systemctl restart moteros` |
| Cambiar config | editar `/opt/apps/moteros/.env` → `sudo systemctl restart moteros` |
| Backup BBDD | **automático**: `/usr/local/bin/backup-mysql.sh` (timer `backup-mysql.timer`, ~03:15 UTC) descubre todas las BBDD y deja `moteros-<fecha>.sql.gz` en `/var/backups/mysql/` (retención 14 días). Manual: `sudo systemctl start backup-mysql.service` |
| Restaurar | `zcat /var/backups/mysql/moteros-<fecha>.sql.gz \| sudo mysql moteros` |
| Renovar SSL | automático (timer certbot); manual `sudo certbot renew` |

## Panel de administración

```bash
SSH_HOST=vps bash backend/deploy/deploy-admin.sh
```

Compila `admin/` en local, instala los estáticos en `/var/www/moteros-admin` y, si el vhost
`moteros-api` (gestionado por Certbot) no tiene aún `location /admin/`, inserta los dos bloques
antes del `location /` del proxy, guardando una copia `.bak-<fecha>`. Queda en
<https://moteros.deveps.dev/admin/>.

## Notas

- **8080** escucha solo en `127.0.0.1` (`server.address=127.0.0.1` en `application-prod.properties`). No se abre nada en UFW.
- **Correo (recuperación de contraseña)**: añadir a `/opt/apps/moteros/.env` `MAIL_USERNAME` y `MAIL_PASSWORD`
  (contraseña de aplicación de Gmail) y `sudo systemctl restart moteros`. Sin ellas la app arranca, pero los
  códigos no se envían (queda `Authentication failed` en el log). `setup-vps.sh` conserva estas líneas al re-ejecutarse.
- **Swagger deshabilitado en `prod`** (`springdoc.*.enabled=false`). Para inspeccionar la API en el VPS, túnel SSH al 8080 con el perfil `dev`, o mirar en local. Healthcheck público: `GET /health`.
- **Esquema con Flyway**: las migraciones de `src/main/resources/db/migration` se aplican solas al arrancar; Hibernate va con `ddl-auto=validate`. Para cambiar el esquema, añadir un `V<n>__descripcion.sql` nuevo; nunca editar uno ya aplicado. La BBDD de producción (creada antes de Flyway) se marcó en la versión 1.
- **Zona horaria**: la URL JDBC fija `serverTimezone=Europe/Madrid`.
- **Rotar secretos** (`DB_PASSWORD` / `JWT_SECRET`) sin tocar Nginx ni el certificado:
  ```bash
  ssh vps 'sudo bash -s' <<'EOF'
  set -e; ENV=/opt/apps/moteros/.env
  NEW_DB=$(openssl rand -base64 24); NEW_JWT=$(openssl rand -base64 64 | tr -d "\n")
  mysql -e "ALTER USER 'moteros'@'localhost' IDENTIFIED BY '${NEW_DB}'; FLUSH PRIVILEGES;"
  sed -i "s|^DB_PASSWORD=.*|DB_PASSWORD=${NEW_DB}|; s|^JWT_SECRET=.*|JWT_SECRET=${NEW_JWT}|" "$ENV"
  systemctl restart moteros
  EOF
  ```
  Rotar `JWT_SECRET` invalida los access token vigentes (15 min); los refresh token siguen en BBDD.

## Zona horaria

El servicio arranca con `-Duser.timezone=Europe/Madrid`. La API devuelve fechas sin zona
(`LocalDateTime`) y la app las interpreta como hora local del movil: si la JVM va en UTC, todo lo
que marca el servidor (mensajes, publicaciones, notificaciones) se ve dos horas mas viejo en
verano. El VPS sigue en UTC, igual que MySQL: un `NOW()` escrito a mano desde `mysql` va en UTC y
hay que sumarle el desfase para que cuadre con lo que escribe la aplicacion.

