-- Trazado por carretera de cada ruta, calculado con OpenRouteService a partir de sus puntos de
-- paso. Se guarda como polilinea codificada (algoritmo de Google, precision 5).
--   NULL -> aun sin calcular (o sin servicio de rutas configurado): se dibujan los puntos.
--   ''   -> no se ha encontrado recorrido por carretera: se dibujan los puntos.
-- Las rutas importadas de un GPX ya siguen la carretera y no se recalculan.

ALTER TABLE rutas
    ADD COLUMN trazado MEDIUMTEXT DEFAULT NULL;
