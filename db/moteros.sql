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
    activo BOOLEAN DEFAULT TRUE
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
-- DATOS DE EJEMPLO
-- ============================================================

INSERT INTO usuarios (nombre_usuario, nombre_completo, email, password_hash, ciudad, biografia, foto_perfil_url) VALUES
('lobo_asfalto', 'Marc Puig', 'marc.puig@email.com', 'hash_pw_1', 'Barcelona', 'Amante de las carreteras de montaña y el buen café al final de ruta.', 'https://cdn.redmoteros.com/perfiles/marc.jpg'),
('reina_curvas', 'Laia Ferrer', 'laia.ferrer@email.com', 'hash_pw_2', 'Girona', 'Naked y curvas, mi combinación favorita.', 'https://cdn.redmoteros.com/perfiles/laia.jpg'),
('rutas_norte', 'Iker Etxeberria', 'iker.etxe@email.com', 'hash_pw_3', 'Bilbao', 'Trail y offroad por el norte peninsular.', 'https://cdn.redmoteros.com/perfiles/iker.jpg'),
('touring_paco', 'Francisco Molina', 'paco.molina@email.com', 'hash_pw_4', 'Sevilla', 'Kilómetros y más kilómetros, siempre con maletas puestas.', 'https://cdn.redmoteros.com/perfiles/paco.jpg'),
('vespa_carla', 'Carla Soto', 'carla.soto@email.com', 'hash_pw_5', 'Valencia', 'Ciudad y costa, scooter de toda la vida.', 'https://cdn.redmoteros.com/perfiles/carla.jpg');

INSERT INTO motos (usuario_id, marca, modelo, anio, cilindrada_cc, tipo, foto_url) VALUES
(1, 'Yamaha', 'MT-07', 2022, 689, 'naked', 'https://cdn.redmoteros.com/motos/mt07.jpg'),
(2, 'Kawasaki', 'Z900', 2021, 948, 'naked', 'https://cdn.redmoteros.com/motos/z900.jpg'),
(3, 'KTM', '790 Adventure', 2020, 799, 'trail', 'https://cdn.redmoteros.com/motos/790adv.jpg'),
(4, 'Honda', 'Africa Twin', 2023, 1084, 'touring', 'https://cdn.redmoteros.com/motos/africatwin.jpg'),
(5, 'Vespa', 'Primavera', 2019, 150, 'scooter', 'https://cdn.redmoteros.com/motos/primavera.jpg');

INSERT INTO rutas (creador_id, nombre, descripcion, punto_inicio, latitud_inicio, longitud_inicio, punto_fin, latitud_fin, longitud_fin, distancia_km, duracion_estimada_min, dificultad, tipo_terreno) VALUES
(1, 'Coll de Rates al amanecer', 'Curvas técnicas con vistas al Mediterráneo, ideal para primera hora.', 'Parcent', 38.7161000, -0.1097000, 'Alicante', 38.3452000, -0.4810000, 45.0, 60, 'moderada', 'asfalto'),
(2, 'Puerto de la Bonaigua', 'Ruta de montaña por el Pirineo con paisajes espectaculares.', 'Esterri d\'Àneu', 42.6213000, 1.1214000, 'Vielha', 42.7020000, 0.7968000, 38.5, 55, 'dificil', 'asfalto'),
(3, 'Pistas de Urkiola', 'Recorrido offroad por el parque natural, requiere trail o enduro.', 'Durango', 43.1700000, -2.6355000, 'Urkiola', 43.1480000, -2.5780000, 27.0, 90, 'dificil', 'offroad'),
(4, 'Ruta de la Plata en dos días', 'Gran travesía histórica de norte a sur, ideal para touring.', 'Gijón', 43.5357000, -5.6615000, 'Sevilla', 37.3891000, -5.9845000, 720.0, 720, 'moderada', 'asfalto'),
(1, 'Escapada a Montserrat', 'Ruta corta y accesible para cualquier nivel, perfecta para principiantes.', 'Barcelona', 41.3851000, 2.1734000, 'Montserrat', 41.5930000, 1.8347000, 55.0, 70, 'facil', 'asfalto');

INSERT INTO puntos_ruta (ruta_id, orden, latitud, longitud, altitud_m, nombre_punto) VALUES
(1, 1, 38.7161000, -0.1097000, 320, 'Salida - Parcent'),
(1, 2, 38.7280000, -0.0850000, 580, 'Mirador Coll de Rates'),
(1, 3, 38.6650000, -0.2100000, 410, 'Cruce hacia Alicante'),
(1, 4, 38.3452000, -0.4810000, 5, 'Llegada - Alicante'),
(5, 1, 41.3851000, 2.1734000, 12, 'Salida - Barcelona'),
(5, 2, 41.4700000, 1.9800000, 250, 'Área de descanso'),
(5, 3, 41.5930000, 1.8347000, 725, 'Llegada - Montserrat');

INSERT INTO valoraciones_rutas (ruta_id, usuario_id, puntuacion, comentario) VALUES
(1, 2, 5, 'Espectacular al amanecer, poco tráfico y curvas perfectas.'),
(1, 3, 4, 'Muy buena, aunque el firme tiene algún bache.'),
(2, 1, 5, 'De las mejores rutas de montaña que he hecho.'),
(3, 4, 4, 'Exigente pero muy divertida en trail.'),
(5, 2, 5, 'Perfecta para ir con amigos que empiezan.');

INSERT INTO quedadas (ruta_id, organizador_id, titulo, descripcion, punto_encuentro, latitud_encuentro, longitud_encuentro, fecha_hora, max_participantes, nivel_recomendado) VALUES
(1, 1, 'Amanecer en Coll de Rates', 'Salida temprano para ver el amanecer desde el mirador. Desayuno incluido al llegar.', 'Gasolinera Repsol, Parcent', 38.7165000, -0.1090000, '2026-10-04 06:30:00', 15, 'cualquiera'),
(2, 2, 'Pirineo en grupo', 'Ruta de montaña, imprescindible moto en buen estado y experiencia en curvas de montaña.', 'Área de descanso, Esterri d\'Àneu', 42.6210000, 1.1220000, '2026-09-20 09:00:00', 12, 'experimentado'),
(3, 3, 'Iniciación al offroad en Urkiola', 'Quedada pensada para quien empieza en trail, ritmo tranquilo.', 'Parking del parque natural, Durango', 43.1695000, -2.6360000, '2026-09-27 10:00:00', 10, 'iniciacion'),
(4, 4, 'Ruta de la Plata - Etapa 1', 'Primera etapa de la gran travesía, salida desde Gijón.', 'Plaza del Ayuntamiento, Gijón', 43.5350000, -5.6620000, '2026-11-01 08:00:00', 20, 'cualquiera'),
(5, 5, 'Domingo tranquilo a Montserrat', 'Ruta relajada, ideal para quien tiene poca experiencia en carretera.', 'Plaza España, Barcelona', 41.3750000, 2.1490000, '2026-09-13 09:30:00', 25, 'iniciacion');

INSERT INTO inscripciones_quedadas (quedada_id, usuario_id, estado) VALUES
(1, 2, 'confirmado'),
(1, 3, 'confirmado'),
(1, 5, 'pendiente'),
(2, 1, 'confirmado'),
(2, 4, 'confirmado'),
(3, 1, 'confirmado'),
(3, 2, 'confirmado'),
(4, 1, 'confirmado'),
(4, 3, 'pendiente'),
(5, 2, 'confirmado'),
(5, 3, 'confirmado'),
(5, 4, 'confirmado');

INSERT INTO amistades (usuario_id, amigo_id, estado) VALUES
(1, 2, 'aceptada'),
(1, 3, 'aceptada'),
(2, 3, 'pendiente'),
(4, 1, 'aceptada'),
(5, 1, 'aceptada'),
(5, 2, 'pendiente');

INSERT INTO publicaciones (usuario_id, ruta_id, contenido, imagen_url) VALUES
(1, 1, 'Hoy tocaba amanecer en Coll de Rates. Sin palabras, si no lo habéis hecho tenéis que apuntaros a la próxima quedada.', 'https://cdn.redmoteros.com/posts/collderates1.jpg'),
(2, 2, 'La Bonaigua sigue siendo una de las rutas más bonitas de España. Curvas infinitas y unas vistas de otro planeta.', 'https://cdn.redmoteros.com/posts/bonaigua1.jpg'),
(3, NULL, 'Estrenando neumáticos nuevos, ¿alguna recomendación de rodaje para los primeros 500 km?', NULL),
(4, 4, 'Preparando la alforja para la Ruta de la Plata. ¡Nervioso y con muchas ganas!', 'https://cdn.redmoteros.com/posts/rutaplata1.jpg'),
(5, 5, 'Primera quedada organizada y un éxito. Gracias a todos los que os apuntasteis a Montserrat.', 'https://cdn.redmoteros.com/posts/montserrat1.jpg');

INSERT INTO comentarios (publicacion_id, usuario_id, contenido) VALUES
(1, 2, '¡Qué envidia! Apúntame a la próxima.'),
(1, 3, 'Las fotos son brutales, menudo sitio.'),
(2, 1, 'Totalmente de acuerdo, esa ruta engancha.'),
(3, 4, 'Yo suelo hacer rodaje suave los primeros 300 km sin frenadas bruscas.'),
(5, 1, 'Genial iniciativa, seguro que repetimos pronto.');

INSERT INTO likes_publicaciones (publicacion_id, usuario_id) VALUES
(1, 2), (1, 3), (1, 4),
(2, 1), (2, 5),
(3, 1), (3, 2),
(4, 1), (4, 2), (4, 3),
(5, 2), (5, 3), (5, 4);

INSERT INTO conversaciones (usuario1_id, usuario2_id) VALUES
(1, 2),
(1, 3),
(4, 5);

INSERT INTO mensajes (conversacion_id, remitente_id, contenido, leido) VALUES
(1, 1, '¿Te apuntas a la quedada de Coll de Rates el sábado?', TRUE),
(1, 2, '¡Claro! Ya me he inscrito, qué ganas.', TRUE),
(1, 1, 'Genial, llevo el casco de repuesto por si acaso.', FALSE),
(2, 3, '¿Sabes si la pista de Urkiola está en buen estado ahora mismo?', TRUE),
(2, 1, 'La hice hace dos semanas, un poco de barro pero se pasa bien.', FALSE),
(3, 4, '¿Cuántos días le vas a dedicar a la Ruta de la Plata?', TRUE),
(3, 5, 'Dos días, con parada en Cáceres para dormir.', FALSE);

INSERT INTO notificaciones (usuario_id, tipo, referencia_id, usuario_origen_id, mensaje, leido) VALUES
(1, 'like', 1, 2, 'A Laia Ferrer le ha gustado tu publicación.', TRUE),
(1, 'comentario', 1, 3, 'Iker Etxeberria ha comentado tu publicación.', FALSE),
(1, 'inscripcion_quedada', 1, 2, 'Laia Ferrer se ha apuntado a tu quedada "Amanecer en Coll de Rates".', FALSE),
(2, 'solicitud_amistad', NULL, 1, 'Marc Puig te ha enviado una solicitud de amistad.', TRUE),
(3, 'amistad_aceptada', NULL, 1, 'Marc Puig ha aceptado tu solicitud de amistad.', FALSE),
(4, 'mensaje', 3, 5, 'Carla Soto te ha enviado un mensaje.', FALSE),
(5, 'nueva_quedada', 5, 1, 'Nueva quedada cerca de ti: "Domingo tranquilo a Montserrat".', TRUE),
(2, 'valoracion_ruta', 1, 2, 'Tu ruta "Coll de Rates al amanecer" ha recibido una nueva valoración.', TRUE);

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

-- ============================================================
-- FIN DEL SCRIPT
-- ============================================================
