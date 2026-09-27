# StockIQ — Diseño de la base de datos

Motor: **MySQL 8.0.16+**, InnoDB, charset `utf8mb4`. El script completo está en `backend/src/main/resources/db/migration/V1__esquema_inicial.sql` y se ejecuta con Flyway al iniciar el backend.

## 1. Decisiones de diseño

| # | Decisión | Justificación |
|---|---|---|
| D-01 | El stock no se guarda; se deriva de la tabla `movimiento`. | Trazabilidad completa y ausencia de inconsistencias entre lo registrado y lo real. |
| D-02 | `movimiento` tiene `deposito_origen_id` y `deposito_destino_id`, ambos opcionales según el tipo. | Permite ubicar entradas y salidas en un depósito y modelar transferencias en una sola tabla. |
| D-03 | Un producto tiene un único proveedor principal (N:1). | El lead time sale de ese proveedor. La relación N:M se deja como trabajo futuro. |
| D-04 | `stock_seguridad` es un atributo de `producto`. | Es un valor propio de cada producto, definido por el encargado. |
| D-05 | Se agrega la tabla `usuario` con un campo `rol` (ENUM). | Con solo dos roles fijos no hace falta una tabla de roles separada. |
| D-06 | Las transferencias pendientes descuentan del origen y quedan "en tránsito"; al confirmarse suman al destino. | Evita contar la mercadería dos veces o perderla de vista. |
| D-07 | Se agrega el estado `CANCELADA` a las transferencias. | Una transferencia pendiente debe poder deshacerse sin borrar el registro. |
| D-08 | Los parámetros de cálculo viven en la tabla `parametro`. | Ventana de consumo, cortes ABC y días de cobertura se ajustan sin tocar el código. |
| D-09 | Baja lógica (`activo`) en producto, proveedor, depósito y usuario. | Se conserva el historial de movimientos. |
| D-10 | Las reglas de coherencia entre tipo de movimiento y depósitos se aplican con `CHECK` en la base. | Defensa adicional a las validaciones del Service. Requiere MySQL 8.0.16 o superior. |
| D-11 | Cantidades y precios usan `DECIMAL`. | Evita errores de redondeo de los tipos flotantes y admite unidades como kg o litros. |
| D-12 | En el ABC, la clase se asigna según el porcentaje acumulado previo al producto, por lo que el producto que cruza un corte queda en la clase superior. | Es el criterio habitual del análisis de Pareto, garantiza que el producto de mayor valor sea siempre clase A y cumple RN-28 (con un solo producto con consumo, este es clase A). |
| D-13 | Al registrar una salida o transferencia, el Service bloquea la fila del producto (`SELECT ... FOR UPDATE`, `@Lock(PESSIMISTIC_WRITE)` en JPA) dentro de la transacción, antes de validar el stock. Al confirmar o cancelar, bloquea la fila de la transferencia. | Como el stock se deriva, dos operaciones simultáneas podrían validar el mismo stock y dejarlo negativo (RN-06), o resolver dos veces una transferencia (RN-13). El bloqueo las serializa. |
| D-14 | El esquema se versiona con Flyway (`db/migration/V{n}__descripcion.sql`) y Hibernate se configura con `ddl-auto=validate`. | Los cambios de esquema quedan registrados en el repositorio y Hibernate solo verifica que las entidades coincidan con la base, sin modificarla. |

## 2. Normalización

- **1FN**: todos los atributos son atómicos; no hay listas dentro de una columna.
- **2FN**: todas las tablas tienen clave simple (`id`, salvo `parametro` con `clave`), por lo que no hay dependencias parciales.
- **3FN**: los datos del proveedor y la categoría viven en sus propias tablas y se referencian por FK; el stock, que es un dato derivado, no se almacena.

## 3. Relaciones y cardinalidades

| Relación | Cardinalidad |
|---|---|
| Categoría — Producto | 1 : N (una categoría tiene muchos productos; un producto, una categoría) |
| Proveedor — Producto | 1 : N (un proveedor abastece muchos productos; un producto tiene un proveedor principal) |
| Producto — Movimiento | 1 : N |
| Usuario — Movimiento | 1 : N (responsable del registro) |
| Usuario — Movimiento (resolución) | 1 : N (quien confirma o cancela una transferencia) |
| Depósito — Movimiento (origen) | 1 : N, opcional |
| Depósito — Movimiento (destino) | 1 : N, opcional |

## 4. Diccionario de datos

### 4.1 `categoria`
| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | Identificador. |
| nombre | VARCHAR(100) | NOT NULL, UNIQUE | Nombre de la categoría. |
| descripcion | VARCHAR(255) | NULL | Descripción opcional. |

### 4.2 `proveedor`
| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | Identificador. |
| nombre | VARCHAR(150) | NOT NULL, UNIQUE | Razón social o nombre. |
| contacto | VARCHAR(150) | NULL | Persona de contacto. |
| telefono | VARCHAR(50) | NULL | Teléfono. |
| email | VARCHAR(150) | NULL | Correo electrónico. |
| lead_time_dias | INT | NOT NULL, DEFAULT 0, CHECK >= 0 | Días de entrega prometidos. |
| activo | BOOLEAN | NOT NULL, DEFAULT TRUE | Baja lógica. |

### 4.3 `producto`
| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | Identificador. |
| codigo | VARCHAR(50) | NOT NULL, UNIQUE | Código interno (SKU). |
| nombre | VARCHAR(150) | NOT NULL | Nombre del producto. |
| categoria_id | BIGINT | NOT NULL, FK → categoria | Categoría. |
| proveedor_id | BIGINT | NOT NULL, FK → proveedor | Proveedor principal. |
| unidad_medida | VARCHAR(20) | NOT NULL | Unidad (un, kg, lt...). |
| precio_referencia | DECIMAL(12,2) | NOT NULL, CHECK >= 0 | Precio para valorizar el consumo (ABC). |
| stock_seguridad | DECIMAL(12,2) | NOT NULL, DEFAULT 0, CHECK >= 0 | Colchón usado en el punto de reposición. |
| activo | BOOLEAN | NOT NULL, DEFAULT TRUE | Baja lógica. |

### 4.4 `deposito`
| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | Identificador. |
| nombre | VARCHAR(100) | NOT NULL, UNIQUE | Nombre del depósito. |
| ubicacion | VARCHAR(255) | NULL | Dirección o descripción. |
| activo | BOOLEAN | NOT NULL, DEFAULT TRUE | Baja lógica. |

### 4.5 `usuario`
| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | Identificador. |
| username | VARCHAR(60) | NOT NULL, UNIQUE | Nombre de usuario. |
| password_hash | VARCHAR(100) | NOT NULL | Hash BCrypt. |
| nombre | VARCHAR(100) | NOT NULL | Nombre. |
| apellido | VARCHAR(100) | NOT NULL | Apellido. |
| rol | ENUM('ENCARGADO','OPERARIO') | NOT NULL | Rol del usuario. |
| activo | BOOLEAN | NOT NULL, DEFAULT TRUE | Baja lógica. |

### 4.6 `movimiento`
| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | Identificador. |
| tipo | ENUM('ENTRADA','SALIDA','TRANSFERENCIA') | NOT NULL | Tipo de movimiento. |
| producto_id | BIGINT | NOT NULL, FK → producto | Producto movido. |
| cantidad | DECIMAL(12,2) | NOT NULL, CHECK > 0 | Cantidad movida. |
| fecha | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Fecha y hora del registro. |
| motivo | VARCHAR(255) | NULL | Motivo (obligatorio en salidas, validado en el Service). |
| usuario_id | BIGINT | NOT NULL, FK → usuario | Responsable del registro. |
| deposito_origen_id | BIGINT | NULL, FK → deposito | Origen (salidas y transferencias). |
| deposito_destino_id | BIGINT | NULL, FK → deposito | Destino (entradas y transferencias). |
| estado_transferencia | ENUM('PENDIENTE','CONFIRMADA','CANCELADA') | NULL | Solo para transferencias. |
| fecha_resolucion | DATETIME | NULL | Cuándo se confirmó o canceló. |
| usuario_resolucion_id | BIGINT | NULL, FK → usuario | Quién confirmó o canceló. |

Restricciones adicionales: `ck_mov_depositos` (coherencia tipo/depósitos y origen ≠ destino) y `ck_mov_estado` (el estado solo existe en transferencias).

### 4.7 `parametro`
| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| clave | VARCHAR(50) | PK | Nombre del parámetro. |
| valor | VARCHAR(50) | NOT NULL | Valor (se convierte al tipo necesario y se valida según RN-37 en el Service). |
| descripcion | VARCHAR(255) | NULL | Para qué se usa. |

## 5. Índices

| Índice | Tabla | Columnas | Motivo |
|---|---|---|---|
| idx_mov_producto_fecha | movimiento | producto_id, fecha | Cálculo de consumo por producto en una ventana. |
| idx_mov_fecha | movimiento | fecha | Filtros y evolución en el tiempo. |
| idx_mov_origen / idx_mov_destino | movimiento | deposito_origen_id / deposito_destino_id | Stock por depósito. |
| idx_mov_usuario | movimiento | usuario_id | Filtro por usuario en el historial. |
| idx_mov_tipo_estado | movimiento | tipo, estado_transferencia | Transferencias pendientes. |
| idx_producto_categoria / idx_producto_proveedor | producto | categoria_id / proveedor_id | Joins y rotación por categoría. |

## 6. Consultas derivadas

El script incluye dos vistas:
- `v_stock_deposito`: stock por producto y depósito, aplicando la regla RN-16. Incluye todos los depósitos; el filtro por depósitos activos para el stock total disponible (RN-18) se aplica en el Service.
- `v_stock_transito`: cantidad en tránsito por producto (RN-17).

Ejemplo de consumo promedio diario de un producto (RN-19), con ventana de 30 días:

```sql
SELECT COALESCE(SUM(cantidad), 0) / 30 AS consumo_diario
  FROM movimiento
 WHERE producto_id = :productoId
   AND tipo = 'SALIDA'
   AND fecha >= NOW() - INTERVAL 30 DAY;
```

Ejemplo de stock total de un producto a una fecha (RN-36), usado para el stock inicial y final de la rotación. Las transferencias confirmadas o canceladas antes de la fecha no alteran el total; las que seguían pendientes a esa fecha se descuentan porque estaban en tránsito:

```sql
SELECT COALESCE(SUM(CASE
         WHEN tipo = 'ENTRADA' THEN  cantidad
         WHEN tipo = 'SALIDA'  THEN -cantidad
         WHEN estado_transferencia = 'PENDIENTE'
           OR fecha_resolucion > :fecha THEN -cantidad
         ELSE 0
       END), 0) AS stock
  FROM movimiento
 WHERE producto_id = :productoId
   AND fecha <= :fecha;
```

Ejemplo de clasificación ABC (RN-27, RN-28 y D-12), con período de 90 días y cortes 80/95:

```sql
WITH consumo AS (
  SELECT p.id, p.nombre,
         COALESCE(SUM(m.cantidad), 0) * p.precio_referencia AS valor
    FROM producto p
    LEFT JOIN movimiento m
           ON m.producto_id = p.id
          AND m.tipo = 'SALIDA'
          AND m.fecha >= NOW() - INTERVAL 90 DAY
   WHERE p.activo = TRUE
   GROUP BY p.id, p.nombre, p.precio_referencia
), acumulado AS (
  SELECT id, nombre, valor,
         SUM(valor) OVER () AS total,
         COALESCE(SUM(valor) OVER (ORDER BY valor DESC, id
                                   ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING), 0) AS acum_previo
    FROM consumo
)
SELECT id, nombre, valor,
       CASE WHEN valor = 0                      THEN 'C'
            WHEN 100 * acum_previo / total < 80 THEN 'A'
            WHEN 100 * acum_previo / total < 95 THEN 'B'
            ELSE 'C' END AS clase
  FROM acumulado
 ORDER BY valor DESC, id;
```

- El `LEFT JOIN` incluye a los productos sin salidas, que quedan en clase C (RN-28).
- La condición `valor = 0` se evalúa primero, lo que además evita dividir por cero cuando ningún producto tuvo consumo.
- La ventana `ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING` suma solo los productos anteriores (acumulado previo); para el primero es `NULL` y el `COALESCE` lo convierte en 0.
- Los valores 90, 80 y 95 son de ejemplo; en el Service se leen de la tabla `parametro`.

## 7. Datos iniciales

Para arrancar el sistema hacen falta: los 5 parámetros (insertados en la migración `V1__esquema_inicial.sql`) y un usuario `ENCARGADO` inicial, que se crea por un seeder de Spring al primer arranque (con la contraseña hasheada con BCrypt).
