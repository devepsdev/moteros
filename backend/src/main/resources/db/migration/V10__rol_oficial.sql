-- Rol para las cuentas de la propia app (la que firma las rutas del catalogo, por ejemplo).
-- Permisos de usuario normal, pero no aparecen en el buscador de moteros ni se pueden agregar
-- como amigos, igual que la cuenta del scraper.

ALTER TABLE usuarios
    MODIFY rol ENUM('user','admin','scraper','oficial') NOT NULL DEFAULT 'user';
