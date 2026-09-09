# Despliegue de la API moter@s en el VPS

VPS OVHcloud · Ubuntu 24.04 · Nginx · MySQL 8 · Certbot · Java 25 · UFW (22/80/443)
SSH: `ssh vps` (config en `~/.ssh/config`, puerto 3573, usuario `ubuntu`, clave `id_ed25519_vps`).

Arquitectura:

```
Android (Expo)  ──HTTPS──►  Cloudflare  ──►  Nginx (443)  ──proxy──►  127.0.0.1:8080  (Spring Boot, systemd)
                                                                              │
                                                                        MySQL 8 (localhost)  ·  /var/lib/moteros/uploads
```

---

## 0. Requisitos previos (una sola vez)

1. **DNS**: crear en Cloudflare un registro **A** para el subdominio de la API
   apuntando a `51.77.158.216`.
   - Elegido: **DNS only** (nube gris) — sin limites del plan Free
     de Cloudflare. Si se deja **Proxied** (nube naranja) también funciona; el reto
     HTTP-01 de Let's Encrypt pasa igualmente.
   - Dominio usado en todos los ejemplos: `moteros.deveps.dev` (cámbialo si eliges otro).
2. **Clave SSH cargada en el agente** (la clave tiene passphrase):
   ```bash
   # Windows PowerShell
   Get-Service ssh-agent | Set-Service -StartupType Automatic
   Start-Service ssh-agent
   ssh-add $env:USERPROFILE\.ssh\id_ed25519_vps
   ```

---

## 1. Build local

```bash
cd backend
./mvnw clean package -DskipTests   # los 59 tests ya pasan; -DskipTests solo acelera
# genera target/moteros-0.0.1-SNAPSHOT.jar (~64 MB)
```

## 2. Subir artefactos al VPS

```bash
cd <raiz-del-repo>
ssh vps 'mkdir -p /tmp/moteros-deploy'
scp backend/target/moteros-0.0.1-SNAPSHOT.jar  vps:/tmp/moteros.jar
scp db/moteros.sql                              vps:/tmp/moteros.sql
scp backend/deploy/*                            vps:/tmp/moteros-deploy/
```

## 3. Provisionar (una sola vez)

```bash
ssh vps
sudo bash /tmp/moteros-deploy/setup-vps.sh moteros.deveps.dev
```

El script (idempotente, **no borra la BBDD si ya tiene tablas**):

| Paso | Qué hace |
|---|---|
| 1 | Instala `python3-certbot-nginx` si falta; comprueba `java` |
| 2 | Crea el usuario de sistema `moteros` y los directorios `/opt/moteros`, `/etc/moteros`, `/var/lib/moteros/uploads` |
| 3 | Carga `db/moteros.sql` en MySQL (solo si la BBDD `moteros` está vacía) |
| 4 | Crea el usuario MySQL `moteros@localhost` con `SELECT/INSERT/UPDATE/DELETE` sobre `moteros.*` y contraseña aleatoria |
| 5 | Escribe `/etc/moteros/moteros.env` (chmod 640, `root:moteros`) con `DB_PASSWORD`, `JWT_SECRET` (`openssl rand -base64 64`), CORS acotado al dominio, rutas de uploads |
| 6 | Instala el jar en `/opt/moteros/moteros.jar`, el unit `moteros.service`, y arranca el servicio (`--spring.profiles.active=prod`) |
| 7 | Instala el vhost Nginx (`/etc/nginx/sites-available/moteros-api`), `nginx -t`, reload |
| 8 | Emite el certificado con `certbot --nginx` y verifica la renovación (`--dry-run`) |
| 9 | Resumen |

## 4. Verificar

```bash
ssh vps 'systemctl status moteros --no-pager | head; curl -s -o /dev/null -w "local %{http_code}\n" http://127.0.0.1:8080/v3/api-docs'
curl -s -o /dev/null -w "publico %{http_code}\n" https://moteros.deveps.dev/v3/api-docs
# alta del primer usuario (queda como admin):
curl -s -X POST https://moteros.deveps.dev/api/auth/registro \
  -H 'Content-Type: application/json' \
  -d '{"nombreUsuario":"admin","nombreCompleto":"Admin","email":"TU_EMAIL","password":"CAMBIA_ESTO_1234"}'
```

## 5. Redespliegues posteriores

```bash
cd backend
SSH_HOST=vps bash deploy/deploy.sh      # build + scp jar + systemctl restart moteros
```

---

## Operación

| Acción | Comando |
|---|---|
| Estado / logs | `systemctl status moteros` · `journalctl -u moteros -f` |
| Reiniciar / parar | `sudo systemctl restart moteros` · `sudo systemctl stop moteros` |
| Cambiar config | editar `/etc/moteros/moteros.env` → `sudo systemctl restart moteros` |
| Backup BBDD | `sudo mysqldump moteros | gzip > ~/moteros_$(date +%F).sql.gz` |
| Renovar SSL | automático (systemd timer de certbot); manual: `sudo certbot renew` |
| Ver uploads | `/var/lib/moteros/uploads` (servido por Spring en `/uploads/**`) |

## Notas

- **Puerto 8080**: solo escucha en `127.0.0.1` (`server.address=127.0.0.1` en el perfil `prod`).
  No hace falta abrir nada en UFW; Nginx publica el 443.
- **`ddl-auto=validate`**: el esquema debe coincidir con las entidades. Si cambian las
  entidades, actualizar `db/moteros.sql` y aplicar el diff a mano (o `mysqldump` + recarga
  en ventana de mantenimiento). No hay migraciones automáticas (Flyway/Liquibase) — se
  puede añadir más adelante.
- **Zona horaria**: la URL JDBC fija `serverTimezone=Europe/Madrid` para que los
  `@CreationTimestamp` cuadren con hora local.
- **Cloudflare Proxied**: si se deja la nube naranja, subir el límite de cuerpo en
  Cloudflare no aplica (el de Nginx es `client_max_body_size 12m`). El plan Free corta
  peticiones a los 100 s; ningún endpoint actual se acerca.
- **Rotación de secretos**: para regenerar `JWT_SECRET` basta con borrar esa línea de
  `moteros.env` y re-ejecutar `setup-vps.sh` (invalida todos los access token vigentes;
  los refresh token siguen en BBDD).
