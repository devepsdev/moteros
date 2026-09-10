# moter@s

Red social para moteros y sus rutas: perfiles y motos, rutas con geolocalización y track,
quedadas, muro social (publicaciones, comentarios, likes), amistades, chat privado 1‑a‑1 y
notificaciones. App Android con Expo sobre una API REST en Spring Boot.

| Entorno | URL |
|---|---|
| API producción | `https://moteros.deveps.dev` |
| Health | `https://moteros.deveps.dev/health` |

---

## Estructura del repositorio

```
moter@s/
├── db/moteros.sql        Esquema MySQL (15 tablas, InnoDB, utf8mb4). Sin datos de ejemplo.
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
│   ├── src/test/         59 tests (JUnit 5, @DataJpaTest con H2, Mockito, MockMvc)
│   └── deploy/           Artefactos y runbook de despliegue en VPS  →  deploy/DEPLOY.md
└── frontend/             App Expo (SDK 57, expo-router)  —  PENDIENTE
```

## Stack

- **Backend**: Spring Boot 4.1, Java 25, Spring Data JPA, Spring Security + JWT (jjwt 0.11),
  springdoc-openapi 3.1 (Swagger, solo en `dev`), Lombok.
- **BBDD**: MySQL 8 (en local, MariaDB vía XAMPP también sirve).
- **Frontend**: Expo / React Native, expo-router (sin empezar).

---

## Backend — desarrollo local

Requisitos: JDK 25, MySQL/MariaDB en `localhost:3306`.

```bash
# 1) Cargar el esquema
mysql -u root < db/moteros.sql          # crea la BBDD 'moteros'

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
`/api/amistades`, `/api/chat`, `/api/notificaciones`, `/api/admin`.

---

## Despliegue

VPS OVHcloud (Ubuntu 24.04, Nginx, MySQL 8, Certbot). El backend corre como servicio systemd
(`moteros.service`) tras Nginx con TLS de Let's Encrypt. Backup diario de la BBDD vía
`backup-mysql.timer`.

Runbook completo y scripts: **[`backend/deploy/DEPLOY.md`](backend/deploy/DEPLOY.md)**.
Redespliegue rápido: `cd backend && SSH_HOST=vps bash deploy/deploy.sh`.

## Frontend

Sin empezar. Carpeta `frontend/` inicializada con Expo SDK 57 + expo-router. Antes de tocar
código, leer los docs versionados: <https://docs.expo.dev/versions/v57.0.0/>.
