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

Textos (nombre, descripción breve y completa), icono de 512×512 y gráfico destacado de 1024×500:
[`store/ficha.md`](store/ficha.md).

### Capturas de pantalla

Entre 2 y 8 por tipo de dispositivo, en PNG o JPG, con el lado corto de 320 px como mínimo y el
largo de 3840 como máximo. Las capturas del propio móvil (1080 × 2400) valen tal cual, sin marcos
ni recortes: se hacen con el APK de pruebas (`eas build -p android --profile preview`) pulsando
bajar volumen y encendido a la vez.

Orden recomendado, que es el que se ve en la ficha:

1. Catálogo de rutas, con varias tarjetas.
2. Detalle de una ruta con el mapa y el trazado por carretera.
3. Crear ruta, con las tarjetas de elegir carretera y la etiqueta «Más curvas».
4. Quedadas, con alguna quedada programada.
5. Feed o perfil, para enseñar la parte social.

Cuida que ninguna pantalla salga vacía, que la barra de estado esté limpia, que no aparezcan datos
personales de nadie y que todas usen el mismo tema (oscuro o claro).

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

La app no se puede usar sin iniciar sesión, así que Google exige una cuenta para sus revisores:
sin ella rechazan la publicación. Se indica en *Prueba y publicación → Contenido de la aplicación
→ Acceso a la aplicación*, marcando que hay funciones restringidas.

Cómo prepararla:

1. Regístrala **desde la app**, con un correo propio al que tengas acceso (por ejemplo
   `deveps+play@deveps.dev`), nunca con la cuenta administradora.
2. Ponle un nombre reconocible, como «Revisión Google Play», y déjala con algo de contenido: una
   moto y alguna ruta creada.
3. No la borres: Google la usa en cada revisión, también en las actualizaciones. Si le cambias la
   contraseña, actualiza la ficha.

En las instrucciones de acceso, indica el usuario y la contraseña, los pasos para entrar y una
nota: que el contenido lo crean los usuarios y que se puede denunciar con el icono de bandera y
bloquear desde el menú ⋮ de un perfil o una conversación.

La contraseña queda guardada en Play Console y la ven los revisores: que sea larga, única y no se
use en ningún otro sitio. No la guardes en el repositorio.

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
eas env:set --name GOOGLE_MAPS_API_KEY --value <clave> --environment production --visibility sensitive
eas build -p android --profile production
eas submit -p android --latest
```

La clave de Google Maps es la del *Maps SDK for Android*; restríngela a *Aplicaciones de Android*
con el paquete `dev.deveps.moteros` y dos huellas SHA‑1: la del certificado de subida de EAS
(`eas credentials -p android`) y la del certificado con el que Google firma la app al publicarla
(Play Console → Configuración → Integridad de la app). Sin la segunda, el mapa sale en gris en la
versión descargada de Play.

## 8. Actualizaciones posteriores

Los cambios de JavaScript llegan a los usuarios con EAS Update, sin pasar por la revisión de
Google: `eas workflow:run .eas/workflows/produccion.yml` (ver [README](README.md)). Solo hay que
subir un AAB nuevo a Play Console cuando cambia la parte nativa.

## 9. Antes de publicar

- [ ] Probar en un móvil el registro con la casilla de términos, las denuncias y los bloqueos.
- [x] Revisar las sugerencias de ruta pendientes en el panel (31 rutas publicadas el 19-09-2026).
- [ ] Crear la clave de Google Maps y guardarla como secreto de EAS (sección 7).
- [ ] Crear la cuenta de prueba para los revisores.
