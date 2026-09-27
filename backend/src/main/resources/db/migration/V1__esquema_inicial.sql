-- StockIQ - Migración V1: esquema inicial
-- Motor: MySQL 8.0.16+ (necesario para que CHECK se aplique), InnoDB, utf8mb4
-- Se ejecuta con Flyway al iniciar el backend. La base "stockiq" la crea
-- docker-compose (MYSQL_DATABASE) o el proveedor de hosting; por eso este
-- script no incluye CREATE DATABASE ni USE.

-- ---------------------------------------------------------------
-- Categoría
-- ---------------------------------------------------------------
CREATE TABLE categoria (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  nombre      VARCHAR(100) NOT NULL,
  descripcion VARCHAR(255) NULL,
  CONSTRAINT pk_categoria PRIMARY KEY (id),
  CONSTRAINT uq_categoria_nombre UNIQUE (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------
-- Proveedor
-- ---------------------------------------------------------------
CREATE TABLE proveedor (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  nombre         VARCHAR(150) NOT NULL,
  contacto       VARCHAR(150) NULL,
  telefono       VARCHAR(50)  NULL,
  email          VARCHAR(150) NULL,
  lead_time_dias INT          NOT NULL DEFAULT 0,
  activo         BOOLEAN      NOT NULL DEFAULT TRUE,
  CONSTRAINT pk_proveedor PRIMARY KEY (id),
  CONSTRAINT uq_proveedor_nombre UNIQUE (nombre),
  CONSTRAINT ck_proveedor_lead_time CHECK (lead_time_dias >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------
-- Producto
-- ---------------------------------------------------------------
CREATE TABLE producto (
  id                BIGINT         NOT NULL AUTO_INCREMENT,
  codigo            VARCHAR(50)    NOT NULL,
  nombre            VARCHAR(150)   NOT NULL,
  categoria_id      BIGINT         NOT NULL,
  proveedor_id      BIGINT         NOT NULL,
  unidad_medida     VARCHAR(20)    NOT NULL,
  precio_referencia DECIMAL(12,2)  NOT NULL,
  stock_seguridad   DECIMAL(12,2)  NOT NULL DEFAULT 0,
  activo            BOOLEAN        NOT NULL DEFAULT TRUE,
  CONSTRAINT pk_producto PRIMARY KEY (id),
  CONSTRAINT uq_producto_codigo UNIQUE (codigo),
  CONSTRAINT fk_producto_categoria FOREIGN KEY (categoria_id) REFERENCES categoria (id),
  CONSTRAINT fk_producto_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedor (id),
  CONSTRAINT ck_producto_precio CHECK (precio_referencia >= 0),
  CONSTRAINT ck_producto_stock_seg CHECK (stock_seguridad >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_producto_categoria ON producto (categoria_id);
CREATE INDEX idx_producto_proveedor ON producto (proveedor_id);

-- ---------------------------------------------------------------
-- Depósito
-- ---------------------------------------------------------------
CREATE TABLE deposito (
  id        BIGINT       NOT NULL AUTO_INCREMENT,
  nombre    VARCHAR(100) NOT NULL,
  ubicacion VARCHAR(255) NULL,
  activo    BOOLEAN      NOT NULL DEFAULT TRUE,
  CONSTRAINT pk_deposito PRIMARY KEY (id),
  CONSTRAINT uq_deposito_nombre UNIQUE (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------
-- Usuario
-- ---------------------------------------------------------------
CREATE TABLE usuario (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  username      VARCHAR(60)  NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  nombre        VARCHAR(100) NOT NULL,
  apellido      VARCHAR(100) NOT NULL,
  rol           ENUM('ENCARGADO','OPERARIO') NOT NULL,
  activo        BOOLEAN      NOT NULL DEFAULT TRUE,
  CONSTRAINT pk_usuario PRIMARY KEY (id),
  CONSTRAINT uq_usuario_username UNIQUE (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------
-- Movimiento (fuente única de verdad del stock)
-- ---------------------------------------------------------------
CREATE TABLE movimiento (
  id                     BIGINT        NOT NULL AUTO_INCREMENT,
  tipo                   ENUM('ENTRADA','SALIDA','TRANSFERENCIA') NOT NULL,
  producto_id            BIGINT        NOT NULL,
  cantidad               DECIMAL(12,2) NOT NULL,
  fecha                  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  motivo                 VARCHAR(255)  NULL,
  usuario_id             BIGINT        NOT NULL,
  deposito_origen_id     BIGINT        NULL,
  deposito_destino_id    BIGINT        NULL,
  estado_transferencia   ENUM('PENDIENTE','CONFIRMADA','CANCELADA') NULL,
  fecha_resolucion       DATETIME      NULL,
  usuario_resolucion_id  BIGINT        NULL,
  CONSTRAINT pk_movimiento PRIMARY KEY (id),
  CONSTRAINT fk_mov_producto   FOREIGN KEY (producto_id)           REFERENCES producto (id),
  CONSTRAINT fk_mov_usuario    FOREIGN KEY (usuario_id)            REFERENCES usuario (id),
  CONSTRAINT fk_mov_origen     FOREIGN KEY (deposito_origen_id)    REFERENCES deposito (id),
  CONSTRAINT fk_mov_destino    FOREIGN KEY (deposito_destino_id)   REFERENCES deposito (id),
  CONSTRAINT fk_mov_resolucion FOREIGN KEY (usuario_resolucion_id) REFERENCES usuario (id),
  CONSTRAINT ck_mov_cantidad CHECK (cantidad > 0),
  -- RN-03, RN-04, RN-05: coherencia entre tipo y depósitos
  CONSTRAINT ck_mov_depositos CHECK (
       (tipo = 'ENTRADA'       AND deposito_origen_id IS NULL     AND deposito_destino_id IS NOT NULL)
    OR (tipo = 'SALIDA'        AND deposito_origen_id IS NOT NULL AND deposito_destino_id IS NULL)
    OR (tipo = 'TRANSFERENCIA' AND deposito_origen_id IS NOT NULL AND deposito_destino_id IS NOT NULL
                               AND deposito_origen_id <> deposito_destino_id)
  ),
  -- El estado solo aplica a transferencias
  CONSTRAINT ck_mov_estado CHECK (
       (tipo = 'TRANSFERENCIA' AND estado_transferencia IS NOT NULL)
    OR (tipo <> 'TRANSFERENCIA' AND estado_transferencia IS NULL)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_mov_producto_fecha ON movimiento (producto_id, fecha);
CREATE INDEX idx_mov_fecha          ON movimiento (fecha);
CREATE INDEX idx_mov_origen         ON movimiento (deposito_origen_id);
CREATE INDEX idx_mov_destino        ON movimiento (deposito_destino_id);
CREATE INDEX idx_mov_usuario        ON movimiento (usuario_id);
CREATE INDEX idx_mov_tipo_estado    ON movimiento (tipo, estado_transferencia);

-- ---------------------------------------------------------------
-- Parámetros configurables de la capa analítica
-- ---------------------------------------------------------------
CREATE TABLE parametro (
  clave       VARCHAR(50)  NOT NULL,
  valor       VARCHAR(50)  NOT NULL,
  descripcion VARCHAR(255) NULL,
  CONSTRAINT pk_parametro PRIMARY KEY (clave)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO parametro (clave, valor, descripcion) VALUES
  ('ventana_consumo_dias',  '30', 'Días hacia atrás para calcular el consumo promedio diario'),
  ('dias_cobertura',        '30', 'Días de consumo que se quiere cubrir al reponer'),
  ('periodo_analisis_dias', '90', 'Período para rotación y clasificación ABC'),
  ('corte_abc_a',           '80', 'Porcentaje acumulado máximo de la clase A'),
  ('corte_abc_b',           '95', 'Porcentaje acumulado máximo de la clase B');

-- ---------------------------------------------------------------
-- Vista de stock por producto y depósito (RN-16)
-- Las transferencias pendientes ya descuentan del origen (en tránsito);
-- solo las confirmadas suman al destino. Las canceladas no descuentan.
-- ---------------------------------------------------------------
CREATE VIEW v_stock_deposito AS
SELECT producto_id, deposito_id, SUM(delta) AS stock
FROM (
  SELECT producto_id, deposito_destino_id AS deposito_id, cantidad AS delta
    FROM movimiento
   WHERE tipo = 'ENTRADA'
  UNION ALL
  SELECT producto_id, deposito_destino_id, cantidad
    FROM movimiento
   WHERE tipo = 'TRANSFERENCIA' AND estado_transferencia = 'CONFIRMADA'
  UNION ALL
  SELECT producto_id, deposito_origen_id, -cantidad
    FROM movimiento
   WHERE tipo = 'SALIDA'
  UNION ALL
  SELECT producto_id, deposito_origen_id, -cantidad
    FROM movimiento
   WHERE tipo = 'TRANSFERENCIA' AND estado_transferencia IN ('PENDIENTE','CONFIRMADA')
) t
GROUP BY producto_id, deposito_id;

-- Vista de stock en tránsito por producto (RN-17)
CREATE VIEW v_stock_transito AS
SELECT producto_id, SUM(cantidad) AS en_transito
  FROM movimiento
 WHERE tipo = 'TRANSFERENCIA' AND estado_transferencia = 'PENDIENTE'
 GROUP BY producto_id;
