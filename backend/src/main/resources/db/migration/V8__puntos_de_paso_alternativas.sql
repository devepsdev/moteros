-- Puntos de paso que no marca el usuario: al elegir una de las carreteras alternativas entre
-- dos puntos, se guarda un punto sobre esa carretera para que el trazado la siga siempre.
-- No se dibujan como puntos de la ruta y se sustituyen al elegir otra alternativa.

ALTER TABLE puntos_ruta
    ADD COLUMN via BOOLEAN NOT NULL DEFAULT FALSE;
