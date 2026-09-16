-- Bloqueos entre usuarios (requisito de Google Play para apps con contenido de usuarios).
--
-- Un bloqueo es de un solo sentido en la tabla, pero sus efectos son mutuos: ninguno de
-- los dos puede escribir al otro, pedirle amistad ni ver sus publicaciones o comentarios.

CREATE TABLE bloqueos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    bloqueador_id INT NOT NULL,
    bloqueado_id INT NOT NULL,
    fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_bloqueo (bloqueador_id, bloqueado_id),
    FOREIGN KEY (bloqueador_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (bloqueado_id) REFERENCES usuarios(id) ON DELETE CASCADE
) ENGINE=InnoDB;
