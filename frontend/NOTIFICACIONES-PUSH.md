# Avisos push: lo que falta configurar

El código está hecho y desplegado: el servidor guarda el token de cada móvil (`/api/dispositivos`)
y manda por la API de Expo el mismo aviso que ya aparece en la campana de la app (mensajes,
solicitudes de amistad, comentarios, me gusta, quedadas, valoraciones). Tocar el aviso abre el
chat o las notificaciones.

Falta una pieza que solo puedes poner tú, porque va con tu cuenta de Google: **en Android, Expo
entrega los avisos a través de Firebase Cloud Messaging (FCM)**. Sin eso la app funciona igual,
pero el móvil no consigue token y no llega ningún aviso (el fallo se captura y no molesta).

Referencia: <https://docs.expo.dev/push-notifications/fcm-credentials/>

## 1. Proyecto de Firebase

1. <https://console.firebase.google.com> → **Añadir proyecto** (por ejemplo, «moteros»). Google
   Analytics no hace falta.
2. En el proyecto, **Añadir app → Android**, con el paquete **`dev.deveps.moteros`**.
3. Descarga **`google-services.json`** y déjalo en `frontend/` (junto a `app.json`). Solo lleva
   identificadores públicos del proyecto: se puede subir al repositorio.

## 2. Apuntarlo en `app.json`

Dentro de `"android"`:

```json
"googleServicesFile": "./google-services.json"
```

## 3. Clave de servidor para Expo

1. Firebase → ⚙ **Configuración del proyecto → Cuentas de servicio → Generar nueva clave
   privada**. Se descarga un JSON.
2. Súbelo a EAS:

   ```bash
   eas credentials -p android
   ```

   *Google Service Account → Manage your Google Service Account Key for Push Notifications (FCM
   V1) → Upload*. También se puede hacer desde expo.dev → proyecto → Credentials.
3. **Ese JSON es secreto**: no lo metas en el repositorio y bórralo del disco cuando esté subido.

## 4. Compilar

`expo-notifications` es nativo, así que no llega por el aire: hace falta un APK/AAB nuevo. El push
a `main` ya lanza el workflow de pruebas, que detecta el cambio de huella y compila un APK nuevo;
después de añadir `google-services.json` hará falta otro (cambia de nuevo la huella).

```bash
eas workflow:run .eas/workflows/produccion.yml
```

para el AAB de Google Play cuando toque.

## 5. Probar

1. Instala el APK nuevo, inicia sesión y acepta el permiso de notificaciones.
2. En el VPS, comprueba que el móvil se ha registrado:
   `sudo mysql moteros -e "SELECT usuario_id, plataforma, fecha_uso FROM dispositivos_push;"`
3. Cierra la app del todo y, desde otra cuenta, mándate un mensaje: tiene que llegar el aviso.

Si no llega, el registro del servidor dice por qué (`sudo journalctl -u moteros | grep -i push`).
