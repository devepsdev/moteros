# moter@s — app

App Android de moter@s hecha con Expo SDK 57 y expo-router. Consume la API del backend
(`../backend`), desplegada en <https://moteros.deveps.dev>.

> Antes de tocar código, leer los docs versionados de Expo: <https://docs.expo.dev/versions/v57.0.0/>.

## Arrancar en local

```bash
npm install
npx expo start
```

Se abre con **Expo Go** en el móvil (el mapa funciona sin clave en Expo Go).

Variables de entorno (opcionales, en `.env.local`, que no se sube al repositorio):

| Variable | Para qué | Por defecto |
| --- | --- | --- |
| `EXPO_PUBLIC_API_URL` | URL de la API (p. ej. `http://192.168.1.50:8080` para el backend local) | `https://moteros.deveps.dev` |
| `GOOGLE_MAPS_API_KEY` | Clave de *Maps SDK for Android*; solo hace falta al compilar el APK/AAB | — |

Comprobaciones:

```bash
npm run typecheck
npm run lint
```

Los tipos de las rutas (`typedRoutes`) se generan al arrancar `npx expo start`; si se añade una
pantalla nueva y `tsc` no la reconoce, basta con arrancar Metro una vez.

## Estructura

```
src/
├── app/          Pantallas (expo-router). (tabs): Inicio, Rutas, Quedadas, Chat, Perfil
│                 acceso/recuperar solo sin sesión; admin/* solo para administradores
├── api/          Un módulo por recurso de la API; client.ts desenvuelve ApiResponse y renueva el token
├── auth/         Tokens en SecureStore y AuthContext (sesión, perfil)
├── components/   Tarjetas y vistas de dominio; ui/ con los componentes base
├── lib/          Hooks (listas paginadas, carga, foco), formato, contadores de no leídos
├── theme/        Colores (oscuro con acento naranja), tipografía y espaciado
└── types/dto.ts  Espejo de los DTOs del backend
```

Notas:

- **Fechas**: el backend usa `LocalDateTime` sin zona en hora de España; la app las trata como hora
  local (`lib/format.ts`: `fechaDeApi` / `fechaParaApi`).
- **Chat y notificaciones** no tienen push: se consultan periódicamente mientras la app está abierta
  (`lib/noLeidos.ts`, `components/ConversacionView.tsx`).
- No usar `con` como nombre de carpeta o fichero: es un nombre reservado en Windows y Git no lo indexa.

## Compilar (EAS)

Perfiles en `eas.json`: `preview` genera un APK para instalar a mano y `production` un AAB para
Google Play.

```bash
npm install -g eas-cli
eas login
eas init                      # vincula el proyecto con la cuenta de Expo (una sola vez)
eas env:set --name GOOGLE_MAPS_API_KEY --value <clave> --environment production --visibility sensitive
eas build -p android --profile preview      # APK
eas build -p android --profile production   # AAB para Play Store
```

Para la ficha de Google Play (páginas legales, seguridad de los datos, permisos, cuenta de prueba
para la revisión): [GOOGLE-PLAY.md](GOOGLE-PLAY.md).
