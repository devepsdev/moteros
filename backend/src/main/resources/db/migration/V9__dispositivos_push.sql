-- Dispositivos donde un usuario quiere recibir avisos con la app cerrada. El token lo da Expo
-- en cada movil; un usuario puede tener varios (movil y tablet) y un mismo movil puede cambiar
-- de usuario, asi que el token es unico y se reasigna al ultimo que inicia sesion.

CREATE TABLE dispositivos_push (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id    INT          NOT NULL,
    token         VARCHAR(255) NOT NULL,
    plataforma    VARCHAR(20)  NULL,
    fecha_alta    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_uso     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_dispositivos_push_token UNIQUE (token),
    CONSTRAINT fk_dispositivos_push_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id) ON DELETE CASCADE
);

CREATE INDEX idx_dispositivos_push_usuario ON dispositivos_push (usuario_id);
