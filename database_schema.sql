-- =============================================================================
-- PetFeeder / PawFeeder - Base de Datos SQL
-- Aplicacion: Dispensador automatico de croquetas para mascotas
-- Generado: 2026-06-14
-- Compatible con: MySQL 8.0+ / MariaDB 10.5+
-- =============================================================================
-- NOTA: Este esquema representa la base de datos BACKEND (servidor).
--       La app movil usa SQLite local (pawfeeder.db) como cache offline;
--       estas tablas son la fuente de verdad cuando el servidor este disponible.
-- =============================================================================


-- ==========================
-- CONFIGURACION INICIAL
-- ==========================

CREATE DATABASE IF NOT EXISTS petfeeder_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE petfeeder_db;

SET FOREIGN_KEY_CHECKS = 0;


-- ==========================
-- TABLA: usuarios
-- Gestiona cuentas de usuario (login / registro / verificacion OTP)
-- ==========================

CREATE TABLE IF NOT EXISTS usuarios (
    id              INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    nombre          VARCHAR(100)    NOT NULL,
    email           VARCHAR(150)    NOT NULL,
    telefono        VARCHAR(20)         NULL,
    password_hash   VARCHAR(255)    NOT NULL,
    verificado      TINYINT(1)      NOT NULL DEFAULT 0,    -- 0=pendiente, 1=verificado
    activo          TINYINT(1)      NOT NULL DEFAULT 1,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_usuarios_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ==========================
-- TABLA: otp_verificacion
-- Codigos OTP de 6 digitos para verificar registro (pantalla VerificarActivity)
-- Expiran a los 5 minutos (300 segundos), coincidiendo con el countdown del app
-- ==========================

CREATE TABLE IF NOT EXISTS otp_verificacion (
    id              INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    usuario_id      INT UNSIGNED    NOT NULL,
    codigo          CHAR(6)         NOT NULL,
    intentos        TINYINT         NOT NULL DEFAULT 0,
    max_intentos    TINYINT         NOT NULL DEFAULT 3,
    expira_en       DATETIME        NOT NULL,              -- created_at + 5 minutos
    usado           TINYINT(1)      NOT NULL DEFAULT 0,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    KEY idx_otp_usuario (usuario_id),
    KEY idx_otp_codigo (codigo),
    CONSTRAINT fk_otp_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ==========================
-- TABLA: sesiones
-- Tokens de sesion activa (se limpia al hacer logout desde Perfil)
-- ==========================

CREATE TABLE IF NOT EXISTS sesiones (
    id              INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    usuario_id      INT UNSIGNED    NOT NULL,
    token           VARCHAR(512)    NOT NULL,              -- JWT o token opaco
    dispositivo     VARCHAR(200)        NULL,              -- User-Agent del celular
    ip_origen       VARCHAR(45)         NULL,
    activa          TINYINT(1)      NOT NULL DEFAULT 1,
    expira_en       DATETIME            NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_sesion_token (token(255)),
    KEY idx_sesion_usuario (usuario_id),
    CONSTRAINT fk_sesion_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ==========================
-- TABLA: mascotas
-- Perfiles de las mascotas (pantalla Mascotas.kt)
-- Una mascota puede estar "activa" => es la que se muestra en el dashboard
-- ==========================

CREATE TABLE IF NOT EXISTS mascotas (
    id              INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    usuario_id      INT UNSIGNED    NOT NULL,
    nombre          VARCHAR(100)    NOT NULL,
    raza            VARCHAR(100)    NOT NULL,
    edad_anos       TINYINT UNSIGNED NOT NULL DEFAULT 0,
    peso_kg         DECIMAL(5,2)    NOT NULL DEFAULT 0.00,
    tamano          ENUM('pequeño','mediano','grande','gigante')
                                    NOT NULL DEFAULT 'mediano',
    activa          TINYINT(1)      NOT NULL DEFAULT 0,   -- solo 1 activa por usuario
    foto_uri        VARCHAR(500)        NULL,              -- ruta local o URL CDN
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    KEY idx_mascotas_usuario (usuario_id),
    KEY idx_mascotas_activa (usuario_id, activa),
    CONSTRAINT fk_mascotas_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Porcion recomendada segun tamano (logica del app):
--   pequeño  ->  80 g/dia
--   mediano  -> 180 g/dia
--   grande   -> 280 g/dia
--   gigante  -> 450 g/dia

-- Razas mapeadas por tamano en el app:
--   pequeño : Chihuahua, Poodle Toy, Shih Tzu
--   mediano : Beagle, Cocker Spaniel, Bulldog Frances
--   grande  : Labrador, Golden Retriever, Pastor Aleman
--   gigante : Gran Danes


-- ==========================
-- TABLA: dispensadores
-- Dispositivos IoT de dispensado vinculados por usuario (pantalla Equipo.kt)
-- Un usuario puede tener un dispensador activo a la vez
-- ==========================

CREATE TABLE IF NOT EXISTS dispensadores (
    id              INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    usuario_id      INT UNSIGNED    NOT NULL,
    nombre          VARCHAR(100)    NOT NULL,
    codigo_unico    VARCHAR(50)     NOT NULL,              -- impreso en el hardware
    firmware_version VARCHAR(20)    NOT NULL DEFAULT 'v1.0.0',
    estado          ENUM('activo','offline','emparejando')
                                    NOT NULL DEFAULT 'offline',
    bateria_percent TINYINT UNSIGNED NOT NULL DEFAULT 100,-- 0-100%
    nivel_tolva_pct TINYINT UNSIGNED NOT NULL DEFAULT 60, -- 0-100% => 0-4 kg
    ssid_wifi       VARCHAR(100)        NULL,
    activo          TINYINT(1)      NOT NULL DEFAULT 1,   -- soft-delete
    last_ping_at    DATETIME            NULL,              -- ultimo heartbeat del hardware
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_dispensador_codigo (codigo_unico),
    KEY idx_dispensador_usuario (usuario_id),
    CONSTRAINT fk_dispensador_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Capacidad de la tolva: nivel_tolva_pct / 100 * 4 kg
-- Comidas estimadas = (tolva_kg * 1000) / 120 g (porcion promedio)


-- ==========================
-- TABLA: horarios
-- Programas de alimentacion por usuario (pantalla Horarios.kt)
-- Cada horario define que dias y a que hora se dispensa
-- ==========================

CREATE TABLE IF NOT EXISTS horarios (
    id              INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    usuario_id      INT UNSIGNED    NOT NULL,
    mascota_id      INT UNSIGNED        NULL,              -- NULL = aplica a mascota activa
    dispensador_id  INT UNSIGNED        NULL,              -- NULL = usa el dispensador activo
    nombre          VARCHAR(50)     NOT NULL,              -- Desayuno / Almuerzo / Cena / Snack / Otro
    icono           VARCHAR(20)     NOT NULL DEFAULT 'sun',-- sun / moon / clock / bone
    hora            VARCHAR(10)     NOT NULL,              -- formato "07:30 AM" (12h con AM/PM)
    lunes           TINYINT(1)      NOT NULL DEFAULT 0,
    martes          TINYINT(1)      NOT NULL DEFAULT 0,
    miercoles       TINYINT(1)      NOT NULL DEFAULT 0,
    jueves          TINYINT(1)      NOT NULL DEFAULT 0,
    viernes         TINYINT(1)      NOT NULL DEFAULT 0,
    sabado          TINYINT(1)      NOT NULL DEFAULT 0,
    domingo         TINYINT(1)      NOT NULL DEFAULT 0,
    porcion_gramos  DECIMAL(6,1)    NOT NULL DEFAULT 100.0,
    activo          TINYINT(1)      NOT NULL DEFAULT 1,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    KEY idx_horarios_usuario (usuario_id),
    KEY idx_horarios_activo (usuario_id, activo),
    CONSTRAINT fk_horarios_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id) ON DELETE CASCADE,
    CONSTRAINT fk_horarios_mascota FOREIGN KEY (mascota_id)
        REFERENCES mascotas (id) ON DELETE SET NULL,
    CONSTRAINT fk_horarios_dispensador FOREIGN KEY (dispensador_id)
        REFERENCES dispensadores (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ==========================
-- TABLA: dispensaciones
-- Historial de cada evento de dispensado (pantalla Historial.kt)
-- Se genera tanto por horario programado como por dispense manual
-- ==========================

CREATE TABLE IF NOT EXISTS dispensaciones (
    id              INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    usuario_id      INT UNSIGNED    NOT NULL,
    mascota_id      INT UNSIGNED        NULL,
    dispensador_id  INT UNSIGNED        NULL,
    horario_id      INT UNSIGNED        NULL,              -- NULL si fue manual
    tipo            ENUM('programada','manual')
                                    NOT NULL DEFAULT 'manual',
    nombre          VARCHAR(100)    NOT NULL DEFAULT 'Manual',-- "Desayuno" o "Manual"
    porcion_gramos  DECIMAL(6,1)    NOT NULL,
    fecha_hora      DATETIME        NOT NULL,
    estado          ENUM('ejecutada','fallida','pendiente')
                                    NOT NULL DEFAULT 'ejecutada',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    KEY idx_disp_usuario (usuario_id),
    KEY idx_disp_fecha (usuario_id, fecha_hora),           -- dashboard "hoy"
    KEY idx_disp_mascota (mascota_id),
    KEY idx_disp_horario (horario_id),
    CONSTRAINT fk_disp_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id) ON DELETE CASCADE,
    CONSTRAINT fk_disp_mascota FOREIGN KEY (mascota_id)
        REFERENCES mascotas (id) ON DELETE SET NULL,
    CONSTRAINT fk_disp_dispensador FOREIGN KEY (dispensador_id)
        REFERENCES dispensadores (id) ON DELETE SET NULL,
    CONSTRAINT fk_disp_horario FOREIGN KEY (horario_id)
        REFERENCES horarios (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ==========================
-- TABLA: telemetria_dispensador
-- Registros periodicos de estado del hardware (heartbeat / MQTT / HTTP)
-- Permite graficar bateria y nivel de tolva en el tiempo
-- ==========================

CREATE TABLE IF NOT EXISTS telemetria_dispensador (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    dispensador_id  INT UNSIGNED    NOT NULL,
    bateria_percent TINYINT UNSIGNED NOT NULL,
    nivel_tolva_pct TINYINT UNSIGNED NOT NULL,
    estado          VARCHAR(20)     NOT NULL DEFAULT 'activo',
    registrado_en   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    KEY idx_telemetria_disp (dispensador_id, registrado_en),
    CONSTRAINT fk_telemetria_dispensador FOREIGN KEY (dispensador_id)
        REFERENCES dispensadores (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ==========================
-- TABLA: notificaciones
-- Alertas del sistema (tolva baja, bateria critica, etc.) [placeholder en Perfil.kt]
-- ==========================

CREATE TABLE IF NOT EXISTS notificaciones (
    id              INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    usuario_id      INT UNSIGNED    NOT NULL,
    dispensador_id  INT UNSIGNED        NULL,
    tipo            ENUM('tolva_baja','bateria_critica','dispensa_ok',
                         'dispensa_fallida','dispositivo_offline','otro')
                                    NOT NULL DEFAULT 'otro',
    titulo          VARCHAR(200)    NOT NULL,
    mensaje         TEXT                NULL,
    leida           TINYINT(1)      NOT NULL DEFAULT 0,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    KEY idx_notif_usuario (usuario_id, leida),
    CONSTRAINT fk_notif_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id) ON DELETE CASCADE,
    CONSTRAINT fk_notif_dispensador FOREIGN KEY (dispensador_id)
        REFERENCES dispensadores (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =============================================================================
-- DATOS DE PRUEBA (SEED)
-- =============================================================================

-- Usuario de prueba
INSERT INTO usuarios (nombre, email, password_hash, verificado) VALUES
('Carlos', 'carlosriosrmz17@gmail.com', '$2b$12$HASH_EJEMPLO_NO_USAR_EN_PROD', 1);

-- Mascota de prueba (Golden Retriever, grande)
INSERT INTO mascotas (usuario_id, nombre, raza, edad_anos, peso_kg, tamano, activa) VALUES
(1, 'Max', 'Golden Retriever', 2, 28.50, 'grande', 1);

-- Dispensador de prueba
INSERT INTO dispensadores (usuario_id, nombre, codigo_unico, firmware_version, estado, bateria_percent, nivel_tolva_pct, ssid_wifi) VALUES
(1, 'PawFeeder Casa', 'PF-2024-001', 'v1.0.0', 'activo', 85, 65, 'MiWifi_Casa');

-- Horarios de prueba (Desayuno y Cena, Lunes a Viernes)
INSERT INTO horarios (usuario_id, mascota_id, dispensador_id, nombre, icono, hora, lunes, martes, miercoles, jueves, viernes, sabado, domingo, porcion_gramos) VALUES
(1, 1, 1, 'Desayuno', 'sun',  '07:30 AM', 1, 1, 1, 1, 1, 1, 1, 140.0),
(1, 1, 1, 'Cena',     'moon', '06:00 PM', 1, 1, 1, 1, 1, 1, 1, 140.0);


-- =============================================================================
-- VISTAS UTILES
-- =============================================================================

-- Resumen del dashboard para el home (Principal.kt)
CREATE OR REPLACE VIEW v_dashboard_usuario AS
SELECT
    u.id                                            AS usuario_id,
    u.nombre                                        AS usuario_nombre,
    m.id                                            AS mascota_id,
    m.nombre                                        AS mascota_nombre,
    m.raza,
    m.tamano,
    m.peso_kg,
    m.edad_anos,
    d.id                                            AS dispensador_id,
    d.estado                                        AS dispensador_estado,
    d.bateria_percent,
    d.nivel_tolva_pct,
    ROUND(d.nivel_tolva_pct / 100.0 * 4, 2)        AS tolva_kg,
    CASE m.tamano
        WHEN 'pequeño' THEN 80
        WHEN 'mediano' THEN 180
        WHEN 'grande'  THEN 280
        WHEN 'gigante' THEN 450
        ELSE 180
    END                                             AS porcion_recomendada_g
FROM usuarios u
LEFT JOIN mascotas m      ON m.usuario_id = u.id AND m.activa = 1
LEFT JOIN dispensadores d ON d.usuario_id = u.id AND d.activo = 1;


-- Resumen de dispensaciones del dia actual
CREATE OR REPLACE VIEW v_dispensaciones_hoy AS
SELECT
    usuario_id,
    COUNT(*)                    AS total_dispensaciones,
    SUM(porcion_gramos)         AS total_gramos_hoy,
    MIN(fecha_hora)             AS primera_dispensa,
    MAX(fecha_hora)             AS ultima_dispensa
FROM dispensaciones
WHERE DATE(fecha_hora) = CURDATE()
  AND estado = 'ejecutada'
GROUP BY usuario_id;


-- Proxima dispensa programada por usuario
CREATE OR REPLACE VIEW v_proxima_dispensa AS
SELECT
    h.usuario_id,
    h.id        AS horario_id,
    h.nombre    AS nombre_horario,
    h.hora,
    h.porcion_gramos,
    CASE DAYOFWEEK(NOW())
        WHEN 1 THEN h.domingo
        WHEN 2 THEN h.lunes
        WHEN 3 THEN h.martes
        WHEN 4 THEN h.miercoles
        WHEN 5 THEN h.jueves
        WHEN 6 THEN h.viernes
        WHEN 7 THEN h.sabado
    END         AS activo_hoy
FROM horarios h
WHERE h.activo = 1;


-- =============================================================================
-- SQLITE LOCAL (pawfeeder.db) - Replica simplificada para la app Android
-- =============================================================================
-- El app usa esta version local (sin usuario_id porque la sesion esta en
-- SharedPreferences). Se sincroniza con el servidor cuando hay conexion.
-- NOTA: SQLite no tiene ENUM; se usan TEXT con CHECK constraints.
-- =============================================================================

/*

CREATE TABLE mascotas (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre      TEXT    NOT NULL,
    raza        TEXT    NOT NULL,
    edad_anos   INTEGER NOT NULL DEFAULT 0,
    peso_kg     REAL    NOT NULL DEFAULT 0,
    tamano      TEXT    NOT NULL DEFAULT 'mediano'
                        CHECK (tamano IN ('pequeño','mediano','grande','gigante')),
    activa      INTEGER NOT NULL DEFAULT 0 CHECK (activa IN (0,1)),
    foto_uri    TEXT             DEFAULT '',
    created_at  INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000)
);

CREATE TABLE dispensadores (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre           TEXT    NOT NULL,
    codigo_unico     TEXT    NOT NULL UNIQUE,
    firmware_version TEXT    NOT NULL DEFAULT 'v1.0.0',
    estado           TEXT    NOT NULL DEFAULT 'offline'
                             CHECK (estado IN ('activo','offline','emparejando')),
    bateria_percent  INTEGER NOT NULL DEFAULT 100,
    nivel_tolva_pct  INTEGER NOT NULL DEFAULT 60,
    ssid_wifi        TEXT             DEFAULT '',
    activo           INTEGER NOT NULL DEFAULT 1 CHECK (activo IN (0,1)),
    created_at       INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000)
);

CREATE TABLE horarios (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre         TEXT    NOT NULL,
    icono          TEXT    NOT NULL DEFAULT 'sun',
    hora           TEXT    NOT NULL,
    lunes          INTEGER NOT NULL DEFAULT 0 CHECK (lunes IN (0,1)),
    martes         INTEGER NOT NULL DEFAULT 0 CHECK (martes IN (0,1)),
    miercoles      INTEGER NOT NULL DEFAULT 0 CHECK (miercoles IN (0,1)),
    jueves         INTEGER NOT NULL DEFAULT 0 CHECK (jueves IN (0,1)),
    viernes        INTEGER NOT NULL DEFAULT 0 CHECK (viernes IN (0,1)),
    sabado         INTEGER NOT NULL DEFAULT 0 CHECK (sabado IN (0,1)),
    domingo        INTEGER NOT NULL DEFAULT 0 CHECK (domingo IN (0,1)),
    porcion_gramos REAL    NOT NULL DEFAULT 100,
    activo         INTEGER NOT NULL DEFAULT 1 CHECK (activo IN (0,1)),
    created_at     INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000)
);

CREATE TABLE dispensaciones (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    tipo           TEXT    NOT NULL CHECK (tipo IN ('programada','manual')),
    nombre         TEXT    NOT NULL DEFAULT 'Manual',
    porcion_gramos REAL    NOT NULL,
    fecha_hora     INTEGER NOT NULL,
    estado         TEXT    NOT NULL DEFAULT 'ejecutada'
                           CHECK (estado IN ('ejecutada','fallida','pendiente')),
    horario_id     INTEGER          REFERENCES horarios(id) ON DELETE SET NULL,
    mascota_id     INTEGER          REFERENCES mascotas(id) ON DELETE SET NULL,
    created_at     INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000)
);

-- Indices para queries frecuentes del app
CREATE INDEX idx_mascotas_activa        ON mascotas(activa);
CREATE INDEX idx_horarios_activo        ON horarios(activo);
CREATE INDEX idx_dispensaciones_fecha   ON dispensaciones(fecha_hora DESC);
CREATE INDEX idx_dispensador_activo     ON dispensadores(activo);

*/


SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- FIN DEL ESQUEMA
-- =============================================================================
