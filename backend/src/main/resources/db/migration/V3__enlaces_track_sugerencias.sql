-- Enlaces al recorrido exacto (GPX, KML...) que la página de origen ofrece para descargar.
--
-- Muchas webs de rutas los alojan en servicios que no permiten la descarga a bots (Google
-- Drive lo prohíbe en su robots.txt), así que el scraper no descarga el fichero: guarda el
-- enlace y el administrador lo descarga e importa en el panel al crear la ruta.
-- JSON: [{"texto": "Descargar archivo GPX", "url": "https://..."}, ...]

ALTER TABLE sugerencias_ruta
    ADD COLUMN enlaces_track_json TEXT AFTER puntos_json;
