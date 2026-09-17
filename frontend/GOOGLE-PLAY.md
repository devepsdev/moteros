# Publicar moter@s en Google Play

Guía para preparar la ficha y la primera subida. Lo que ya está resuelto en el código aparece
como hecho; lo demás se rellena en Play Console.

## 1. Lo que Google exige y ya está implementado

| Requisito de Play | Dónde está |
|---|---|
| Política de privacidad accesible sin instalar la app | `https://moteros.deveps.dev/privacidad` |
| Eliminar la cuenta desde la app y desde la web | Perfil → Eliminar cuenta · `https://moteros.deveps.dev/eliminar-cuenta` |
| Términos de uso aceptados al registrarse | Casilla obligatoria en el registro; la API guarda la fecha |
| Denunciar contenido generado por usuarios | Perfiles, publicaciones, comentarios, mensajes, rutas y quedadas |
| Bloquear a otros usuarios | Perfil o conversación; se gestionan en Perfil → Usuarios bloqueados |
| Moderar lo denunciado | Panel `https://moteros.deveps.dev/admin/denuncias` (aviso por correo al administrador) |
| Edad mínima declarada | 16 años, en la casilla del registro y en las dos páginas legales |

## 2. Ficha de la aplicación

- **Política de privacidad**: `https://moteros.deveps.dev/privacidad`
- **Eliminación de cuentas** (en Seguridad de los datos): `https://moteros.deveps.dev/eliminar-cuenta`
- **Correo de contacto**: `deveps@deveps.dev`
- **Categoría sugerida**: Estilo de vida (o Redes sociales).

Hacen falta además capturas de pantalla (mínimo 2), icono de 512×512 y gráfico destacado de
1024×500. El icono está en `assets/images/icon.png`.

## 3. Seguridad de los datos

Los datos que recoge la app, tal como los describe la política de privacidad. Ninguno se comparte
con terceros con fines publicitarios y no hay publicidad ni analítica.

| Tipo de dato | Se recoge | Obligatorio | Para qué |
|---|---|---|---|
| Nombre y nombre de usuario | Sí | Sí | Funciones de la app (perfil) |
| Correo electrónico | Sí | Sí | Gestión de la cuenta |
| Contraseña | Sí | Sí | Gestión de la cuenta (se guarda cifrada con bcrypt) |
| Ciudad y biografía | Sí | No | Funciones de la app (perfil) |
| Fotos | Sí | No | Funciones de la app (perfil, motos, publicaciones) |
| Mensajes con otros usuarios | Sí | No | Funciones de la app (chat) |
| Otro contenido: rutas, quedadas, publicaciones y comentarios | Sí | No | Funciones de la app |
| Ubicación | **No se recoge** | — | Solo se usa en el dispositivo para centrar el mapa; lo que se envía son los puntos que el usuario marca a mano |
| Identificadores del dispositivo o de publicidad | No | — | — |

Responde además: datos cifrados en tránsito (sí, HTTPS) y el usuario puede pedir que se borren
sus datos (sí, desde la app y por correo).

## 4. Clasificación del contenido

Al rellenar el cuestionario hay que indicar que la app:

- permite que los usuarios se comuniquen entre sí (chat, comentarios y publicaciones);
- muestra contenido creado por los usuarios, con denuncias, bloqueos y moderación;
- permite compartir la ubicación de un punto de encuentro que el usuario marca (no su ubicación en
  tiempo real);
- no tiene compras, publicidad, apuestas ni contenido para adultos.

## 5. Cuenta de prueba para la revisión

La app no se puede usar sin iniciar sesión, así que en Play Console hay que dar acceso a los
revisores (*Acceso a la aplicación → Se requiere acceso*): crea una cuenta normal desde la app y
apunta ahí su usuario y su contraseña. No guardes esas credenciales en el repositorio.

## 6. Permisos de Android

La app declara solo estos permisos (comprobado con `npx expo config --type introspect`):

- `INTERNET`
- `ACCESS_COARSE_LOCATION` y `ACCESS_FINE_LOCATION`: centrar el mapa al trazar una ruta o marcar un
  punto de encuentro. La app funciona si se deniegan.
- `READ_EXTERNAL_STORAGE`: elegir una foto de la galería en Android antiguos.

En `app.json`, `android.blockedPermissions` impide que las dependencias añadan cámara, micrófono,
escritura en almacenamiento o ubicación en segundo plano.

## 7. Compilar y subir

```bash
eas env:create --name GOOGLE_MAPS_API_KEY --value <clave> --environment production
eas build -p android --profile production
eas submit -p android --latest
```

La clave de Google Maps es la del *Maps SDK for Android*; restríngela al paquete
`dev.deveps.moteros` y a la huella SHA‑1 del certificado de subida que genere EAS.

## 8. Antes de publicar

- [ ] Probar en un móvil el registro con la casilla de términos, las denuncias y los bloqueos.
- [ ] Revisar las sugerencias de ruta pendientes en el panel para que la app no se publique vacía.
- [ ] Comprobar que las rutas del catálogo tomadas de webs ajenas cuentan con permiso de su autor.
- [ ] Crear la cuenta de prueba para los revisores.
