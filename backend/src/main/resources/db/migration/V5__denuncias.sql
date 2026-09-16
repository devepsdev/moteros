-- Denuncias de contenido (requisito de Google Play para apps con contenido de usuarios).
--
-- Cualquier usuario puede denunciar un perfil, una publicacion, un comentario, un mensaje
-- recibido, una ruta o una quedada. Se guarda una copia del contenido tal como estaba, para
-- poder revisarlo aunque su autor lo edite o lo borre. Un administrador la revisa en el panel:
-- puede eliminar el contenido, dar de baja al autor, las dos cosas o descartarla.

CREATE TABLE denuncias (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL UNIQUE,
    -- Se conservan aunque se borre la cuenta de quien denuncia o de quien es denunciado.
    denunciante_id INT DEFAULT NULL,
    denunciado_id INT DEFAULT NULL,
    tipo ENUM('usuario','publicacion','comentario','mensaje','ruta','quedada') NOT NULL,
    referencia_uuid CHAR(36) NOT NULL,
    motivo ENUM('spam','acoso','odio','sexual','violencia','suplantacion','otro') NOT NULL,
    descripcion VARCHAR(1000) DEFAULT NULL,
    contenido TEXT,
    imagen_url VARCHAR(500) DEFAULT NULL,
    estado ENUM('pendiente','resuelta','descartada') NOT NULL DEFAULT 'pendiente',
    contenido_eliminado BOOLEAN NOT NULL DEFAULT FALSE,
    usuario_dado_de_baja BOOLEAN NOT NULL DEFAULT FALSE,
    nota_resolucion VARCHAR(500) DEFAULT NULL,
    resuelta_por_id INT DEFAULT NULL,
    fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP,
    fecha_resolucion DATETIME DEFAULT NULL,
    INDEX idx_denuncias_estado (estado, fecha_creacion),
    INDEX idx_denuncias_referencia (tipo, referencia_uuid),
    FOREIGN KEY (denunciante_id) REFERENCES usuarios(id) ON DELETE SET NULL,
    FOREIGN KEY (denunciado_id) REFERENCES usuarios(id) ON DELETE SET NULL,
    FOREIGN KEY (resuelta_por_id) REFERENCES usuarios(id) ON DELETE SET NULL
) ENGINE=InnoDB;
