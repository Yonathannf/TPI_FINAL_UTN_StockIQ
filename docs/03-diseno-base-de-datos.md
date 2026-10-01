# StockIQ — Diseño de la base de datos

Motor: **MySQL 8.0.16+**, InnoDB, charset `utf8mb4`. El esquema se versiona en dos migraciones de Flyway: `V1__esquema_inicial.sql` (esquema base) y `V2__ajustes_cierre_analisis.sql` (columnas agregadas al cerrar los puntos abiertos de la segunda entrega).

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
| D-15 | No se puede desactivar un depósito con stock distinto de cero o transferencias pendientes (RN-38), ni un producto en la misma situación (RN-39). Se valida en el Service antes del `UPDATE ... SET activo = false`, no con una restricción de la base. | Evita que el stock total disponible (RN-18) subestime mercadería real y que una transferencia quede con un depósito/producto inactivo en uno de sus extremos. No se puede expresar como `CHECK` porque depende de agregar sobre `movimiento`. |
| D-16 | La fecha de un movimiento la asigna siempre el servidor (`CURRENT_TIMESTAMP`, RN-41); la API no admite que el cliente la indique. | Simplicidad y consistencia de la capa analítica. Los movimientos con fecha pasada que se necesitan para probar rotación y ABC en la etapa 5 se cargan con un script de inserción directa a la base, no a través de la API. Para garantizar que todas las fechas queden en UTC sin importar dónde corra el backend, se configura la JVM en UTC (`-Duser.timezone=UTC`) y Hibernate con `spring.jpa.properties.hibernate.jdbc.time_zone=UTC`; la conversión a hora de Argentina se hace solo al mostrar, filtrar o agrupar por día. |
| D-17 | La consulta de "stock a una fecha" (RN-36), usada para el stock inicial y final de la rotación, suma todos los depósitos, incluidos los inactivos; en cambio RN-18 (stock disponible hoy, para alertas) suma solo los depósitos activos. | Son preguntas distintas: RN-18 es operativa ("qué tengo disponible ahora"), RN-36 es histórica ("qué había en un momento dado"). Desactivar un depósito hoy no debe cambiar retroactivamente cuánto stock participó del consumo durante un período pasado. |
| D-18 | `producto.fraccionable` (BOOLEAN) indica si sus movimientos admiten cantidad no entera (RN-44). Se valida en el Service, no con `CHECK`, porque `cantidad` es una columna de `movimiento` y la condición depende de otra tabla (`producto`); MySQL no permite `CHECK` entre tablas. | Un campo explícito es más robusto que inferir la regla del texto libre de `unidad_medida`. |
| D-19 | `categoria.activo` (BOOLEAN) se agrega para unificar el criterio de baja: las 5 entidades maestras (categoría, proveedor, producto, depósito, usuario) tienen el mismo mecanismo de baja lógica (RN-46). | Antes, categoría era la única entidad maestra sin estado, lo que generaba el criterio inconsistente que señaló la revisión: existía `DELETE` para categoría pero ninguna baja lógica cuando tiene historial. |

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
| activo | BOOLEAN | NOT NULL, DEFAULT TRUE | Baja lógica (RN-45, RN-46). Una categoría inactiva no puede asignarse a productos (RN-43). Agregada en `V2`. |

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
| fraccionable | BOOLEAN | NOT NULL, DEFAULT TRUE | Si es FALSE, la cantidad de sus movimientos debe ser entera (RN-44); solo puede pasar a FALSE si su stock es entero (RN-47). Agregada en `V2`. |
| activo | BOOLEAN | NOT NULL, DEFAULT TRUE | Baja lógica; no se puede pasar a FALSE si tiene stock o transferencias pendientes (RN-39). |

### 4.4 `deposito`
| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | Identificador. |
| nombre | VARCHAR(100) | NOT NULL, UNIQUE | Nombre del depósito. |
| ubicacion | VARCHAR(255) | NULL | Dirección o descripción. |
| activo | BOOLEAN | NOT NULL, DEFAULT TRUE | Baja lógica; no se puede pasar a FALSE si tiene stock o transferencias pendientes (RN-38). |

### 4.5 `usuario`
| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | Identificador. |
| username | VARCHAR(60) | NOT NULL, UNIQUE | Nombre de usuario. |
| password_hash | VARCHAR(100) | NOT NULL | Hash BCrypt. |
| nombre | VARCHAR(100) | NOT NULL | Nombre. |
| apellido | VARCHAR(100) | NOT NULL | Apellido. |
| rol | ENUM('ENCARGADO','OPERARIO') | NOT NULL | Rol del usuario. No se puede cambiar a `OPERARIO` al último `ENCARGADO` activo ni el propio rol (RN-40). |
| activo | BOOLEAN | NOT NULL, DEFAULT TRUE | Baja lógica; no se puede desactivar al último `ENCARGADO` activo ni un usuario a sí mismo (RN-40). Un usuario inactivo pierde el acceso aunque tenga un token vigente (RF-02). |

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

Ejemplo de validación antes de desactivar un depósito (RN-38) — si devuelve alguna fila, se rechaza la baja con 409:

```sql
SELECT 1
  FROM movimiento
 WHERE (deposito_origen_id = :depositoId OR deposito_destino_id = :depositoId)
   AND tipo = 'TRANSFERENCIA' AND estado_transferencia = 'PENDIENTE'
 LIMIT 1;
-- + comprobar que v_stock_deposito no tenga stock > 0 para ese depósito.
```

La validación de un producto (RN-39) es análoga, reemplazando el filtro por `producto_id = :productoId` y comprobando también `v_stock_transito`.

## 7. Datos iniciales

Para arrancar el sistema hacen falta: los 5 parámetros (insertados en la migración `V1__esquema_inicial.sql`) y un usuario `ENCARGADO` inicial, que se crea por un seeder de Spring al primer arranque (con la contraseña hasheada con BCrypt).
