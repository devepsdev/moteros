# Prueba cerrada: 12 probadores durante 14 días

Google exige que una cuenta de desarrollador personal creada después del 13-11-2023 pase una
prueba cerrada antes de poder pedir acceso a producción: **12 probadores apuntados de forma
continua durante 14 días seguidos**. Quien se apunte y se salga antes de los 14 días no cuenta, y
si alguien se sale y vuelve, sus días empiezan de cero.

Referencia oficial:
<https://support.google.com/googleplay/android-developer/answer/14151465?hl=es>

## 1. Antes de invitar a nadie

En Play Console:

1. **Prueba y publicación → Pruebas → Prueba cerrada**, crear una versión y subir el AAB.
2. En la pestaña **Probadores**, crear una lista de correos y pegar las direcciones. Deben ser
   **cuentas de Google** (gmail.com o cualquier correo asociado a una cuenta de Google), la misma
   con la que cada uno tiene configurado su móvil Android.
3. Copiar el **enlace de participación** («Copiar enlace»): es el que se les envía.
4. Comprobar en **Países y regiones** que España (y el país de cada probador) está incluido.
5. Rellenar **Contenido de la aplicación** (política de privacidad, acceso para revisores,
   seguridad de los datos y clasificación), porque sin eso la versión no se publica ni en pruebas.

Recomendaciones:

- Invita a **15 o 16**, no a 12 justos: siempre falla alguien y el contador vuelve a empezar.
- No te cuentes a ti: usa cuentas de otras personas.
- El enlace tarda un rato en funcionar (a veces varias horas) desde que se publica la versión. No
  lo envíes hasta comprobarlo tú mismo con otra cuenta.

## 2. Mensaje para enviarles

> Hola. Estoy publicando en Google Play una app que he hecho, **moter@s**: una red social para
> rutas en moto, quedadas y compartir fotos. Para que Google me deje publicarla necesito 12
> personas que la tengan instalada **14 días seguidos**, así que me harías un favor enorme.
>
> Solo funciona en **Android 7 o superior** (en iPhone no, lo siento).
>
> **Qué tienes que hacer:**
>
> 1. Dime con qué **cuenta de Google** tienes configurado el móvil (el correo). Sin eso no puedo
>    darte acceso.
> 2. Cuando te avise, abre este enlace desde el móvil y pulsa **«Convertirme en probador»**:
>    `PEGAR AQUÍ EL ENLACE DE PARTICIPACIÓN`
> 3. En esa misma página, pulsa **«Descargar en Google Play»** e instala la app.
> 4. Regístrate dentro de la app con tu correo y úsala de vez en cuando durante las dos semanas.
> 5. **Importante:** no te salgas del programa de pruebas ni desinstales la app durante esos 14
>    días. Si alguien lo hace, el contador se reinicia para todos y hay que volver a empezar.
>
> **Qué me viene bien que pruebes** (no hace falta todo, cuanto más mejor):
>
> - Registrarte, poner foto de perfil y añadir tu moto.
> - Mirar las rutas publicadas y abrir alguna para ver el mapa.
> - Crear una ruta tuya y proponerla.
> - Crear o apuntarte a una quedada.
> - Publicar una foto, comentar y dar me gusta.
> - Buscar a alguien, hacerte seguidor y mandar un mensaje por el chat.
>
> **Cuéntame lo que falle**, por aquí mismo: qué móvil tienes, qué estabas haciendo y, si puedes,
> una captura. También lo que te parezca feo o confuso, que para eso son las pruebas.
>
> Gracias de verdad. Cuando pasen los 14 días te aviso y ya podrás tenerla como una app normal.

## 3. Durante los 14 días

- En **Prueba cerrada → Probadores** se ve cuántos están apuntados. Revísalo cada dos o tres días:
  el día que bajes de 12, el contador de Google se reinicia.
- Apunta el **día de inicio** (el primer día con 12 apuntados) y no cuentes desde antes.
- Las correcciones de JavaScript llegan sin pasar por Google con EAS Update
  (`eas workflow:run .eas/workflows/produccion.yml`), así que puedes arreglar cosas durante la
  prueba sin subir un AAB nuevo ni molestar a nadie.
- Guarda lo que te vayan diciendo: al pedir acceso a producción, Google pregunta **qué comentarios
  recogiste y qué cambiaste** a partir de ellos, y hay que contestarlo con detalle.

## 4. Al terminar

En el panel de Play Console, **«Solicitar acceso a producción»**, y responder al formulario: cómo
se hizo la prueba, qué comentarios hubo, qué se cambió y por qué la app está lista. La revisión
puede tardar varios días.

## 5. Problemas típicos

| Lo que te dirán | Qué pasa |
|---|---|
| «Me dice que no está disponible para mi dispositivo» | El móvil está con otra cuenta de Google, no con la que diste de alta. Que la comprueben en Play Store → foto de perfil. |
| «El enlace no hace nada» | Aún no se ha propagado la versión, o no ha pulsado «Convertirme en probador» antes de ir a la ficha. |
| «Ya tenía la app instalada» | Si probaron el APK que mandaste tú por WhatsApp, tienen que **desinstalarlo primero**: está firmado con otro certificado y Play no puede actualizarlo. Sus datos no se pierden, están en el servidor. |
| «No me llega ningún correo» | Google no envía nada: el acceso va por el enlace de participación que les mandas tú. |
| «No veo la app en Play buscándola» | Una app en prueba cerrada no sale en el buscador. Solo se llega por el enlace. |
