-- Sugerencias de rutas del scraper.
--
-- El scraper entra en la API con una cuenta propia de rol 'scraper' (permisos de
-- usuario normal) y deja aqui las rutas que encuentra en webs de rutas en moto.
-- No escriben en "rutas": un administrador las revisa en el panel y, si son buenas,
-- crea la ruta a partir de ella y vincula la sugerencia con la ruta creada.

ALTER TABLE usuarios
    MODIFY rol ENUM('user','admin','scraper') NOT NULL DEFAULT 'user';

CREATE TABLE sugerencias_ruta (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL UNIQUE,
    usuario_id INT NOT NULL,
    url_fuente VARCHAR(500) NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    descripcion TEXT,
    punto_inicio VARCHAR(120) NOT NULL,
    punto_fin VARCHAR(120) NOT NULL,
    distancia_km DECIMAL(6,1),
    duracion_estimada_min INT,
    dificultad ENUM('facil','moderada','dificil','extrema'),
    tipo_terreno ENUM('asfalto','offroad','mixto'),
    -- Pueblos o lugares de paso en orden, con coordenadas si se pudieron geolocalizar:
    -- [{"nombre":"Montseny","latitud":41.76,"longitud":2.39}, ...]
    puntos_json TEXT,
    estado ENUM('pendiente','aprobada','rechazada') NOT NULL DEFAULT 'pendiente',
    ruta_id INT DEFAULT NULL,
    motivo_rechazo VARCHAR(255) DEFAULT NULL,
    fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_sugerencias_ruta_estado (estado, fecha_creacion),
    INDEX idx_sugerencias_ruta_nombre (nombre, punto_inicio),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (ruta_id) REFERENCES rutas(id) ON DELETE SET NULL
) ENGINE=InnoDB;
