# moter@s

Red social para moteros y sus rutas: perfiles y motos, rutas con geolocalización y track,
quedadas, muro social (publicaciones, comentarios, likes), amistades, chat privado 1‑a‑1 y
notificaciones. Incluye bloqueos entre usuarios, denuncias de contenido con moderación desde el
panel y páginas legales públicas, como pide Google Play. App Android con Expo sobre una API REST en
Spring Boot.

| Entorno | URL |
|---|---|
| API producción | `https://moteros.deveps.dev` |
| Health | `https://moteros.deveps.dev/health` |
| Panel de administración | `https://moteros.deveps.dev/admin/` |
| Privacidad · términos · eliminar cuenta | `/privacidad` · `/terminos` · `/eliminar-cuenta` |

---

## Estructura del repositorio

```
moter@s/
├── backend/              API REST — Spring Boot 4 / Java 25 / Maven
│   ├── src/main/java/dev/deveps/moteros/
│   │   ├── entities/     Entidades JPA (+ entities/enums)
│   │   ├── dto/          DTOs (ApiResponseDTO / PagedResponseDTO + Request/Response/Summary/Search/Filter)
│   │   ├── repositories/ Spring Data JPA
│   │   ├── services/     Interfaces + services/impl (aquí vive el mapeo entidad↔DTO)
│   │   ├── controllers/  REST (/api/...)
│   │   ├── mapper/        EntityDtoMapper
│   │   ├── security/      JWT (access + refresh rotatorio), filtro, SecurityConfig
│   │   └── config/        OpenAPI, WebConfig (uploads)
│   ├── src/test/         130 tests (JUnit 5, @DataJpaTest con H2, Mockito, MockMvc)
│   └── deploy/           Artefactos y runbook de despliegue en VPS  →  deploy/DEPLOY.md
├── admin/                Panel web de administración (Angular 21 + Tailwind 4) → moteros.deveps.dev/admin/
├── scraper/              Agente en Python que propone rutas a la bandeja del panel → scraper/README.md
└── frontend/             App Android con Expo (SDK 57, expo-router)  →  frontend/README.md
```

## Stack

- **Backend**: Spring Boot 4.1, Java 25, Spring Data JPA, Spring Security + JWT (jjwt 0.11),
  springdoc-openapi 3.1 (Swagger, solo en `dev`), Lombok.
- **BBDD**: MySQL 8 (en local, MariaDB vía XAMPP también sirve).
- **Frontend**: Expo SDK 57 / React Native 0.86, expo-router, react-native-maps, SecureStore.

---

## Backend — desarrollo local

Requisitos: JDK 25, MySQL/MariaDB en `localhost:3306`.

```bash
# 1) Crear la BBDD vacía (las tablas las crea Flyway al arrancar)
mysql -u root -e "CREATE DATABASE IF NOT EXISTS moteros CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 2) Ajustar credenciales si hace falta en backend/src/main/resources/application.properties
#    (por defecto: usuario root, sin contraseña)

# 3) Arrancar
cd backend
./mvnw spring-boot:run

# API en http://localhost:8080  ·  Swagger UI en http://localhost:8080/swagger-ui/index.html
```

Tests (usan H2 en memoria, no necesitan MySQL):

```bash
cd backend && ./mvnw test
```

Perfiles: por defecto usa `application.properties` (`ddl-auto=validate`). El perfil **`prod`**
(`--spring.profiles.active=prod`) toma todo de variables de entorno y desactiva Swagger.

## API — nociones

- Todas las respuestas van envueltas en `ApiResponseDTO { success, message, data, timestamp }`.
  Los listados usan `PagedResponseDTO { content, pageable{...} }`.
- **Auth**: `POST /api/auth/registro` y `POST /api/auth/login` → `{ token, refreshToken, expiresIn, usuario }`.
  El access token (JWT) dura 15 min; se renueva con `POST /api/auth/refresh` (rota el refresh token).
  `POST /api/auth/logout` revoca el refresh token.
- **El primer usuario que se registra queda como `admin`**; el resto, `user`.
  `/api/admin/**` requiere rol admin (panel de estadísticas, gestión de roles).
- Cabecera: `Authorization: Bearer <token>`.
- Subida de imágenes: `POST /api/uploads` (multipart `file`, jpg/png/webp/gif, ≤8 MB) → devuelve
  una `url` que se guarda en `fotoPerfilUrl` / `fotoUrl` / `imagenUrl`.

Recursos principales: `/api/usuarios`, `/api/motos`, `/api/rutas` (+ `/puntos`, `/valoraciones`),
`/api/quedadas` (+ `/inscripciones`), `/api/publicaciones` (+ `/comentarios`, `/like`),
`/api/amistades`, `/api/bloqueos`, `/api/denuncias`, `/api/chat`, `/api/notificaciones`, `/api/admin`.

- **Registro**: exige `aceptaTerminos` (la casilla de la app) y guarda la fecha de aceptación.
- **Bloqueos**: `/api/bloqueos` corta mensajes, solicitudes y visibilidad del contenido en los dos
  sentidos. **Denuncias**: `POST /api/denuncias` guarda una copia del contenido denunciado y avisa
  por correo a los administradores; se resuelven en `/api/admin/denuncias`.
- **Páginas legales**: `/privacidad`, `/terminos` y `/eliminar-cuenta` son públicas (HTML estático en
  `backend/src/main/resources/static/legal`). Requisitos de la ficha de Play:
  [`frontend/GOOGLE-PLAY.md`](frontend/GOOGLE-PLAY.md).

---

## Despliegue

VPS OVHcloud (Ubuntu 24.04, Nginx, MySQL 8, Certbot). El backend corre como servicio systemd
(`moteros.service`) tras Nginx con TLS de Let's Encrypt. Backup diario de la BBDD vía
`backup-mysql.timer`.

Runbook completo y scripts: **[`backend/deploy/DEPLOY.md`](backend/deploy/DEPLOY.md)**.
Redespliegue rápido: `cd backend && SSH_HOST=vps bash deploy/deploy.sh`.

## Panel de administración

Web interna en `admin/` (Angular 21 zoneless + Tailwind 4, el mismo stack y estructura que el panel
de rastrix, con la identidad oscura y naranja de la app). Solo entran cuentas `admin`: si la cuenta
no es administradora se cierra la sesión en el acto.

Pantallas: resumen con tareas pendientes, rutas (listado con búsqueda, alta y edición trazando el
recorrido sobre un mapa de OpenStreetMap), sugerencias del scraper (revisión con vista previa del
recorrido, alta de la ruta precargada y rechazo), denuncias (copia del contenido denunciado, borrado
del contenido y baja del autor) y usuarios (roles, incluido `scraper`, y bajas).

En local, con el backend en el puerto 8080:

```bash
cd admin
npm install
npm start          # http://localhost:4200 — proxy.conf.json reenvía /api al backend
```

Despliegue: `SSH_HOST=vps bash backend/deploy/deploy-admin.sh` (compila, sube a
`/var/www/moteros-admin` y añade el bloque `/admin/` al vhost de Nginx si falta).

## Scraper

Agente en Python (`scraper/`) que corre en la Orange Pi una vez por semana, como el de rastrix.
Lee las páginas de `scraper/sources.yaml`, salta las que no han cambiado, extrae las rutas en
moto con DeepSeek, ubica los lugares de paso con OpenStreetMap y las envía a la bandeja de
sugerencias del panel. No publica nada por su cuenta. Detalles, instalación y pruebas en
[scraper/README.md](scraper/README.md).

## Frontend

App Android: feed, rutas trazadas sobre el mapa con valoraciones, quedadas, amigos, chat,
notificaciones y panel de administración. Arranque, variables de entorno y compilación con EAS en
**[`frontend/README.md`](frontend/README.md)**.

```bash
cd frontend && npm install && npx expo start
```
