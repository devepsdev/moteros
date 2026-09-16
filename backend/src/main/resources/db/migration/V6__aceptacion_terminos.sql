-- Fecha en que cada usuario acepto los terminos de uso y la politica de privacidad al
-- registrarse. Las cuentas anteriores a esta migracion quedan sin fecha (NULL).

ALTER TABLE usuarios
    ADD COLUMN fecha_aceptacion_terminos DATETIME DEFAULT NULL AFTER fecha_registro;
