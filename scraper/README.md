# Scraper de moter@s

Agente que recorre una lista de páginas web con rutas en moto, extrae con DeepSeek cada
ruta (salida, llegada, lugares de paso, distancia…), ubica los lugares en el mapa con
OpenStreetMap y la envía como **sugerencia** a la bandeja de revisión del panel. Nunca
publica nada por su cuenta: todo pasa por un administrador. Es el mismo diseño que el
scraper de rastrix.

## Cómo funciona

1. Lee las fuentes de `sources.yaml`.
2. Descarga cada página respetando `robots.txt`, con una pausa entre páginas.
3. Si el texto no ha cambiado desde la última pasada, la salta sin gastar DeepSeek.
4. DeepSeek extrae las rutas en JSON; cada una se valida aquí (valores permitidos,
   longitudes, salida y llegada al principio y al final del recorrido). La descripción la
   redacta el modelo; no se copia el texto de la página.
5. Cada lugar de paso se busca en Nominatim (OpenStreetMap), limitado a España, con una
   petición por segundo como pide su política de uso. Los resultados, también los que no se
   encuentran, se guardan en `data/state.db` para no repetir consultas.
6. Se envían a `POST /api/sugerencias-ruta` con la cuenta del bot. La API descarta las que
   ya están en el catálogo (mismo nombre y salida) o ya se sugirieron antes, aunque se
   rechazaran.

En el panel, **Sugerencias** muestra el recorrido aproximado; «Crear ruta con estos datos»
abre el formulario precargado para ajustar los puntos en el mapa y, al publicar, la
sugerencia queda aprobada.

## Cuenta del bot

1. Regístrala desde la app (por ejemplo, con el nombre de usuario `scraper`).
2. En el panel, **Usuarios**, cámbiale el rol a **Scraper**.

El scraper comprueba el rol al arrancar y se detiene si no es `scraper`: con otro rol la
API rechaza las sugerencias.

## Instalación en la Orange Pi

La primera vez, crea las dos carpetas (`/opt/apps` es de root):

```bash
sudo install -d -o "$USER" -g "$USER" /opt/apps/moteros-src /opt/apps/moteros-scraper
```

Y después, sin sudo:

```bash
git clone https://github.com/devepsdev/moteros.git /opt/apps/moteros-src
bash /opt/apps/moteros-src/scraper/scripts/install.sh
```

Crea la configuración con el asistente, que pide cada dato, comprueba la cuenta del bot y la
clave de DeepSeek, y guarda `/opt/apps/moteros-scraper/.env` con permisos 600 (Enter mantiene
los valores actuales, así que sirve también para cambiar la contraseña o la clave):

```bash
python3 /opt/apps/moteros-src/scraper/scripts/configure.py
```

Y prueba sin enviar nada:

```bash
cd /opt/apps/moteros-src/scraper
SCRAPER_HOME=/opt/apps/moteros-scraper /opt/apps/moteros-scraper/venv/bin/python main.py --dry-run
```

La pasada automática queda en el crontab del usuario (lunes a las 06:30, media hora después
de la de rastrix), con logs en `/opt/apps/moteros-scraper/logs/`. Para actualizar código o
fuentes, vuelve a ejecutar `install.sh`.

## Desarrollo

```bash
cd scraper
python -m venv .venv
.venv/Scripts/pip install -r requirements-dev.txt   # Windows
.venv/Scripts/python -m pytest
```
