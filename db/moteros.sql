-- ============================================================
-- RED MOTEROS - Base de datos para red social de moteros
-- Incluye: usuarios, motos, rutas con geolocalizacion, quedadas,
-- interacciones sociales, mensajeria privada y notificaciones
-- Motor: InnoDB | Charset: utf8mb4
-- ============================================================

DROP DATABASE IF EXISTS moteros;
CREATE DATABASE moteros
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE moteros;

-- ============================================================
-- TABLA: usuarios
-- ============================================================
CREATE TABLE usuarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL UNIQUE,
    nombre_usuario VARCHAR(50) NOT NULL UNIQUE,
    nombre_completo VARCHAR(100) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    ciudad VARCHAR(80),
    biografia VARCHAR(280),
    foto_perfil_url VARCHAR(255),
    fecha_registro DATETIME DEFAULT CURRENT_TIMESTAMP,
    activo BOOLEAN DEFAULT TRUE,
    rol ENUM('user','admin') NOT NULL DEFAULT 'user'
) ENGINE=InnoDB;

-- ============================================================
-- TABLA: refresh_tokens (renovacion de sesion sin re-login)
-- ============================================================
CREATE TABLE refresh_tokens (
    id INT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(128) NOT NULL UNIQUE,
    usuario_id INT NOT NULL,
    expira_en DATETIME NOT NULL,
    revocado BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- TABLA: motos (motos que posee cada usuario)
-- ============================================================
CREATE TABLE motos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL UNIQUE,
    usuario_id INT NOT NULL,
    marca VARCHAR(50) NOT NULL,
    modelo VARCHAR(60) NOT NULL,
    anio SMALLINT,
    cilindrada_cc INT,
    tipo ENUM('naked','trail','custom','deportiva','touring','scooter','clasica') DEFAULT 'naked',
    foto_url VARCHAR(255),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- TABLA: rutas (con geolocalizacion de inicio y fin)
-- ============================================================
CREATE TABLE rutas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL UNIQUE,
    creador_id INT NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    descripcion TEXT,
    punto_inicio VARCHAR(120) NOT NULL,
    latitud_inicio DECIMAL(10,7),
    longitud_inicio DECIMAL(10,7),
    punto_fin VARCHAR(120) NOT NULL,
    latitud_fin DECIMAL(10,7),
    longitud_fin DECIMAL(10,7),
    distancia_km DECIMAL(6,1),
    duracion_estimada_min INT,
    dificultad ENUM('facil','moderada','dificil','extrema') DEFAULT 'moderada',
    tipo_terreno ENUM('asfalto','offroad','mixto') DEFAULT 'asfalto',
    fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (creador_id) REFERENCES usuarios(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- TABLA: puntos_ruta (track/waypoints ordenados de una ruta,
-- para poder dibujar el recorrido completo en un mapa)
-- ============================================================
CREATE TABLE puntos_ruta (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL UNIQUE,
    ruta_id INT NOT NULL,
    orden INT NOT NULL,
    latitud DECIMAL(10,7) NOT NULL,
    longitud DECIMAL(10,7) NOT NULL,
    altitud_m INT,
    nombre_punto VARCHAR(100),
    UNIQUE KEY uq_punto_orden (ruta_id, orden),
    FOREIGN KEY (ruta_id) REFERENCES rutas(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- TABLA: valoraciones_rutas
-- ============================================================
CREATE TABLE valoraciones_rutas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL UNIQUE,
    ruta_id INT NOT NULL,
    usuario_id INT NOT NULL,
    puntuacion TINYINT NOT NULL CHECK (puntuacion BETWEEN 1 AND 5),
    comentario VARCHAR(280),
    fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_valoracion (ruta_id, usuario_id),
    FOREIGN KEY (ruta_id) REFERENCES rutas(id) ON DELETE CASCADE,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- TABLA: quedadas (con geolocalizacion del punto de encuentro)
-- ============================================================
CREATE TABLE quedadas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL UNIQUE,
    ruta_id INT,
    organizador_id INT NOT NULL,
    titulo VARCHAR(120) NOT NULL,
    descripcion TEXT,
    punto_encuentro VARCHAR(150) NOT NULL,
    latitud_encuentro DECIMAL(10,7),
    longitud_encuentro DECIMAL(10,7),
    fecha_hora DATETIME NOT NULL,
    max_participantes INT DEFAULT 20,
    nivel_recomendado ENUM('cualquiera','iniciacion','experimentado') DEFAULT 'cualquiera',
    estado ENUM('programada','cancelada','finalizada') DEFAULT 'programada',
    fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ruta_id) REFERENCES rutas(id) ON DELETE SET NULL,
    FOREIGN KEY (organizador_id) REFERENCES usuarios(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- TABLA: inscripciones_quedadas (usuarios apuntados a una quedada)
-- ============================================================
CREATE TABLE inscripciones_quedadas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL UNIQUE,
    quedada_id INT NOT NULL,
    usuario_id INT NOT NULL,
    estado ENUM('confirmado','pendiente','cancelado') DEFAULT 'confirmado',
    fecha_inscripcion DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_inscripcion (quedada_id, usuario_id),
    FOREIGN KEY (quedada_id) REFERENCES quedadas(id) ON DELETE CASCADE,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- TABLA: amistades (relaciones entre usuarios)
-- ============================================================
CREATE TABLE amistades (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL UNIQUE,
    usuario_id INT NOT NULL,
    amigo_id INT NOT NULL,
    estado ENUM('pendiente','aceptada','rechazada') DEFAULT 'pendiente',
    fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_amistad (usuario_id, amigo_id),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (amigo_id) REFERENCES usuarios(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- TABLA: publicaciones (muro social)
-- ============================================================
CREATE TABLE publicaciones (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL UNIQUE,
    usuario_id INT NOT NULL,
    ruta_id INT,
    contenido TEXT NOT NULL,
    imagen_url VARCHAR(255),
    fecha_publicacion DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (ruta_id) REFERENCES rutas(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ============================================================
-- TABLA: comentarios
-- ============================================================
CREATE TABLE comentarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL UNIQUE,
    publicacion_id INT NOT NULL,
    usuario_id INT NOT NULL,
    contenido VARCHAR(280) NOT NULL,
    fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (publicacion_id) REFERENCES publicaciones(id) ON DELETE CASCADE,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- TABLA: likes_publicaciones
-- ============================================================
CREATE TABLE likes_publicaciones (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL UNIQUE,
    publicacion_id INT NOT NULL,
    usuario_id INT NOT NULL,
    fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_like (publicacion_id, usuario_id),
    FOREIGN KEY (publicacion_id) REFERENCES publicaciones(id) ON DELETE CASCADE,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- TABLA: conversaciones (hilo de mensajeria privada 1 a 1)
-- ============================================================
CREATE TABLE conversaciones (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL UNIQUE,
    usuario1_id INT NOT NULL,
    usuario2_id INT NOT NULL,
    fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_par_usuarios (usuario1_id, usuario2_id),
    FOREIGN KEY (usuario1_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (usuario2_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    CHECK (usuario1_id < usuario2_id)
) ENGINE=InnoDB;

-- ============================================================
-- TABLA: mensajes (mensajes privados dentro de una conversacion)
-- ============================================================
CREATE TABLE mensajes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL UNIQUE,
    conversacion_id INT NOT NULL,
    remitente_id INT NOT NULL,
    contenido TEXT NOT NULL,
    leido BOOLEAN DEFAULT FALSE,
    fecha_envio DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (conversacion_id) REFERENCES conversaciones(id) ON DELETE CASCADE,
    FOREIGN KEY (remitente_id) REFERENCES usuarios(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- TABLA: notificaciones
-- ============================================================
CREATE TABLE notificaciones (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL UNIQUE,
    usuario_id INT NOT NULL,
    tipo ENUM('like','comentario','solicitud_amistad','amistad_aceptada',
              'inscripcion_quedada','nueva_quedada','quedada_cancelada',
              'mensaje','valoracion_ruta') NOT NULL,
    referencia_id INT COMMENT 'ID del registro relacionado (publicacion, quedada, mensaje, etc.)',
    usuario_origen_id INT COMMENT 'Usuario que origina la notificacion',
    mensaje VARCHAR(255) NOT NULL,
    leido BOOLEAN DEFAULT FALSE,
    fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (usuario_origen_id) REFERENCES usuarios(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ============================================================
-- ÍNDICES ADICIONALES PARA CONSULTAS FRECUENTES
-- ============================================================
CREATE INDEX idx_quedadas_fecha ON quedadas(fecha_hora);
CREATE INDEX idx_rutas_dificultad ON rutas(dificultad);
CREATE INDEX idx_publicaciones_fecha ON publicaciones(fecha_publicacion);
CREATE INDEX idx_rutas_geo_inicio ON rutas(latitud_inicio, longitud_inicio);
CREATE INDEX idx_quedadas_geo ON quedadas(latitud_encuentro, longitud_encuentro);
CREATE INDEX idx_mensajes_conversacion ON mensajes(conversacion_id, fecha_envio);
CREATE INDEX idx_notificaciones_usuario ON notificaciones(usuario_id, leido, fecha_creacion);
CREATE INDEX idx_refresh_tokens_usuario ON refresh_tokens(usuario_id);

-- ============================================================
-- FIN DEL SCRIPT
-- ============================================================
