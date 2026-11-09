# StockIQ — API REST

Contrato de la API del backend. Todas las rutas, salvo el login, requieren el header `Authorization: Bearer <token>`. Los cuerpos de pedido y respuesta se envían en JSON.

## 1. Convenciones

| Aspecto | Criterio |
|---|---|
| Baja lógica | Categorías, productos, proveedores, depósitos y usuarios se dan de baja con `PATCH /{id}/desactivar` y se reactivan con `PATCH /{id}/activar` (RF-08). Categoría y proveedor, además, admiten `DELETE` cuando no tienen productos asociados (RN-46). |
| Usuarios inactivos | Un usuario desactivado recibe 401 en cualquier pedido, aunque su token no haya vencido (RF-02). |
| Fechas en movimientos | El campo `fecha` nunca se envía en el cuerpo del pedido: siempre lo asigna el servidor (RN-41). |
| Cantidades | Como máximo dos decimales; si el producto tiene `fraccionable = false`, debe ser un entero (RN-44). |
| Listados | Los listados de catálogo aceptan el filtro `activo` (`true`, `false` u omitido para todos). |
| Paginación | El historial de movimientos usa `page` (desde 0) y `size` (por defecto 20, máximo 100) y devuelve el total de elementos y de páginas. |
| Fechas | Formato ISO 8601 (`2026-09-27` o `2026-09-27T14:30:00`). |
| Usuario responsable | Se toma del token; nunca se envía en el cuerpo del pedido. |
| Cuerpos de pedido | Solo admiten los campos indicados en cada sección. Cualquier otro (por ejemplo `id`, `activo` o `fecha`) se rechaza con 400 y el código `CAMPO_NO_PERMITIDO`. Los campos marcados con `?` son opcionales. |
| Respuestas | Cada sección indica la forma del JSON que devuelve el recurso. Un `DELETE` exitoso responde 204 sin cuerpo. |

## 2. Autenticación

| Método | Ruta | Rol | Descripción | Req. |
|---|---|---|---|---|
| POST | `/auth/login` | Público | Recibe usuario y contraseña; devuelve el token JWT y los datos del usuario. | RF-01 |
| GET | `/auth/me` | Todos | Devuelve los datos del usuario autenticado. El frontend lo usa para recuperar la sesión al recargar la página. | RF-01, RF-02 |

**Cuerpos (JSON).**

- Respuesta de `POST /auth/login`: `{ token, usuario: { id, username, nombre, apellido, rol } }`.
- Respuesta de `GET /auth/me`: `{ id, username, nombre, apellido, rol }` (el mismo objeto `usuario` del login).

## 3. Usuarios

| Método | Ruta | Rol | Descripción | Req. |
|---|---|---|---|---|
| GET | `/usuarios` | ENCARGADO | Lista los usuarios. | RF-03 |
| GET | `/usuarios/{id}` | ENCARGADO | Detalle de un usuario. | RF-03 |
| POST | `/usuarios` | ENCARGADO | Crea un usuario con su rol (username único, RN-33). | RF-03 |
| PUT | `/usuarios/{id}` | ENCARGADO | Edita nombre, apellido, rol y, opcionalmente, la contraseña; rechaza con 409 si cambia a `OPERARIO` al último `ENCARGADO` activo o si el usuario autenticado cambia su propio rol. | RF-03, RN-40 |
| PATCH | `/usuarios/{id}/desactivar` | ENCARGADO | Baja lógica; rechaza con 409 si es el propio usuario autenticado o el último `ENCARGADO` activo. | RF-03, RN-40 |
| PATCH | `/usuarios/{id}/activar` | ENCARGADO | Reactiva el usuario. | RF-03 |

**Cuerpos (JSON).**

- Pedido de `POST`: `{ username, password, nombre, apellido, rol }`.
- Pedido de `PUT`: `{ nombre, apellido, rol, password? }`. El `username` no se modifica; si `password` no viene, la contraseña no cambia.
- Respuesta: `{ id, username, nombre, apellido, rol, activo }`. Nunca incluye la contraseña.

## 4. Catálogo

### 4.1 Categorías
| Método | Ruta | Rol | Descripción | Req. |
|---|---|---|---|---|
| GET | `/categorias` | Todos | Lista las categorías; filtro `activo`. | RF-04 |
| GET | `/categorias/{id}` | Todos | Detalle de una categoría. | RF-04 |
| POST | `/categorias` | ENCARGADO | Crea una categoría. | RF-04 |
| PUT | `/categorias/{id}` | ENCARGADO | Edita una categoría. | RF-04 |
| PATCH | `/categorias/{id}/desactivar` | ENCARGADO | Baja lógica. | RF-08, RN-45 |
| PATCH | `/categorias/{id}/activar` | ENCARGADO | Reactiva la categoría. | RF-08 |
| DELETE | `/categorias/{id}` | ENCARGADO | Elimina físicamente una categoría sin productos asociados. | RN-45, RN-46 |

**Cuerpos (JSON).**

- Pedido de `POST` y `PUT`: `{ nombre, descripcion? }`.
- Respuesta: `{ id, nombre, descripcion, activo }`.

### 4.2 Proveedores
| Método | Ruta | Rol | Descripción | Req. |
|---|---|---|---|---|
| GET | `/proveedores` | ENCARGADO | Lista los proveedores. | RF-05 |
| GET | `/proveedores/{id}` | ENCARGADO | Detalle de un proveedor. | RF-05 |
| POST | `/proveedores` | ENCARGADO | Crea un proveedor con su lead time. | RF-05 |
| PUT | `/proveedores/{id}` | ENCARGADO | Edita un proveedor. | RF-05 |
| PATCH | `/proveedores/{id}/desactivar` | ENCARGADO | Baja lógica. Un producto activo puede seguir teniendo este proveedor inactivo como principal (RN-42). | RF-05, RN-31 |
| PATCH | `/proveedores/{id}/activar` | ENCARGADO | Reactiva el proveedor. | RF-05 |
| DELETE | `/proveedores/{id}` | ENCARGADO | Elimina físicamente un proveedor sin productos asociados. | RN-31, RN-46 |

**Cuerpos (JSON).**

- Pedido de `POST` y `PUT`: `{ nombre, contacto?, telefono?, email?, leadTimeDias }`.
- Respuesta: `{ id, nombre, contacto, telefono, email, leadTimeDias, activo }`.

### 4.3 Productos
| Método | Ruta | Rol | Descripción | Req. |
|---|---|---|---|---|
| GET | `/productos` | Todos | Lista los productos; filtros `categoriaId`, `proveedorId`, `activo` y `texto` (código o nombre). | RF-06 |
| GET | `/productos/{id}` | Todos | Detalle de un producto. | RF-06 |
| POST | `/productos` | ENCARGADO | Crea un producto; rechaza con 409 si la categoría o el proveedor están inactivos. | RF-06, RN-43 |
| PUT | `/productos/{id}` | ENCARGADO | Edita un producto; rechaza con 409 si se le asigna una categoría o un proveedor inactivo, o si se lo pasa a no fraccionable con stock no entero. | RF-06, RN-43, RN-47 |
| PATCH | `/productos/{id}/desactivar` | ENCARGADO | Baja lógica; rechaza con 409 si el producto tiene stock, stock en tránsito o transferencias pendientes (RN-39). | RF-08, RN-39 |
| PATCH | `/productos/{id}/activar` | ENCARGADO | Reactiva el producto. | RF-08 |

**Cuerpos (JSON).**

- Pedido de `POST` y `PUT`: `{ codigo, nombre, categoriaId, proveedorId, unidadMedida, precioReferencia, stockSeguridad, fraccionable }`.
- Respuesta: `{ id, codigo, nombre, categoria: { id, nombre, activo }, proveedor: { id, nombre, activo }, unidadMedida, precioReferencia, stockSeguridad, fraccionable, activo }`.

En el pedido, la categoría y el proveedor se indican por id (`categoriaId`, `proveedorId`); en la respuesta vienen **anidados**, con su nombre y su estado, para que el frontend pueda mostrarlos sin otra consulta y marcar los inactivos (RN-42, RN-43).

## 5. Depósitos

| Método | Ruta | Rol | Descripción | Req. |
|---|---|---|---|---|
| GET | `/depositos` | Todos | Lista los depósitos. | RF-07 |
| GET | `/depositos/{id}` | Todos | Detalle de un depósito. | RF-07 |
| POST | `/depositos` | ENCARGADO | Crea un depósito. | RF-07 |
| PUT | `/depositos/{id}` | ENCARGADO | Edita un depósito. | RF-07 |
| PATCH | `/depositos/{id}/desactivar` | ENCARGADO | Baja lógica; rechaza con 409 si el depósito tiene stock o transferencias pendientes (RN-38). | RF-08, RN-38 |
| PATCH | `/depositos/{id}/activar` | ENCARGADO | Reactiva el depósito. | RF-08 |

**Cuerpos (JSON).**

- Pedido de `POST` y `PUT`: `{ nombre, ubicacion? }`.
- Respuesta: `{ id, nombre, ubicacion, activo }`.

## 6. Movimientos

| Método | Ruta | Rol | Descripción | Req. |
|---|---|---|---|---|
| POST | `/movimientos/entradas` | Todos | Registra una entrada (producto, depósito destino, cantidad, motivo opcional). | RF-09 |
| POST | `/movimientos/salidas` | Todos | Registra una salida (producto, depósito origen, cantidad, motivo obligatorio). | RF-10 |
| POST | `/movimientos/transferencias` | Todos | Registra una transferencia en estado `PENDIENTE`. | RF-11 |
| PATCH | `/movimientos/{id}/confirmar` | Todos | Confirma una transferencia pendiente. | RF-12 |
| PATCH | `/movimientos/{id}/cancelar` | Todos | Cancela una transferencia pendiente. | RF-12 |
| GET | `/movimientos` | Todos | Historial paginado; filtros `productoId`, `depositoId`, `tipo`, `usuarioId`, `desde` y `hasta`. | RF-15 |
| GET | `/movimientos/{id}` | Todos | Detalle de un movimiento. | RF-15 |
| GET | `/movimientos/transferencias/pendientes` | Todos | Lista las transferencias pendientes; filtro opcional `depositoDestinoId`. | RF-12, RF-24 |

No existen `PUT` ni `DELETE` sobre movimientos (RF-14, RN-07).

**Cuerpos (JSON).**

- Pedido de `POST /movimientos/entradas`: `{ productoId, depositoDestinoId, cantidad, motivo? }`.
- Pedido de `POST /movimientos/salidas`: `{ productoId, depositoOrigenId, cantidad, motivo }`.
- Pedido de `POST /movimientos/transferencias`: `{ productoId, depositoOrigenId, depositoDestinoId, cantidad, motivo? }`.
- `PATCH /movimientos/{id}/confirmar` y `PATCH /movimientos/{id}/cancelar` no llevan cuerpo.
- Respuesta de un movimiento (en el registro, el detalle, la confirmación, la cancelación, las pendientes y cada elemento del historial):

```json
{
  "id": 15,
  "tipo": "TRANSFERENCIA",
  "producto": { "id": 10, "codigo": "BEB-001", "nombre": "Agua mineral 2 L", "unidadMedida": "un" },
  "cantidad": 24,
  "fecha": "2026-10-22T21:40:00Z",
  "motivo": null,
  "usuario": { "id": 1, "nombre": "Jennifer", "apellido": "Franco" },
  "depositoOrigen": { "id": 1, "nombre": "Central" },
  "depositoDestino": { "id": 2, "nombre": "Sucursal Norte" },
  "estadoTransferencia": "PENDIENTE",
  "fechaResolucion": null,
  "usuarioResolucion": null
}
```

  `depositoOrigen` es `null` en las entradas y `depositoDestino` es `null` en las salidas (RN-03, RN-04). `estadoTransferencia`, `fechaResolucion` y `usuarioResolucion` son `null` salvo en las transferencias resueltas (RN-14).
- Respuesta de `GET /movimientos` (historial paginado), ordenado del más reciente al más antiguo:

```
{ "contenido": [ ...movimientos... ], "pagina": 0, "tamanio": 20, "totalElementos": 45, "totalPaginas": 3 }
```

- Respuesta de `GET /movimientos/transferencias/pendientes`: lista de movimientos, del más antiguo al más reciente.

**Fechas (RN-41).** Las fechas de las respuestas se envían en UTC con el sufijo `Z` (por ejemplo `2026-10-22T21:40:00Z`); el frontend las muestra en hora de Argentina. Los filtros `desde` y `hasta` del historial son días (`AAAA-MM-DD`) en hora de Argentina y ambos son inclusivos: el backend los convierte a UTC (desde las 00:00 del día `desde` hasta las 23:59:59 del día `hasta`).

**Códigos de error propios.**

| Código | HTTP | Cuándo |
|---|---|---|
| `STOCK_INSUFICIENTE` | 409 | La salida o la transferencia dejaría negativo el stock del depósito origen (RN-06). |
| `TRANSFERENCIA_NO_PENDIENTE` | 409 | Se intenta confirmar o cancelar una transferencia ya resuelta (RN-13). |
| `PRODUCTO_INACTIVO` / `DEPOSITO_INACTIVO` | 409 | Movimiento sobre un producto o un depósito inactivo (RN-08). |
| `DEPOSITOS_IGUALES` | 400 | Transferencia con el mismo depósito de origen y destino (RN-05). |
| `MOTIVO_OBLIGATORIO` | 400 | Salida sin motivo (RN-09). |
| `CANTIDAD_NO_ENTERA` | 400 | Cantidad con decimales para un producto no fraccionable (RN-44). |


## 7. Stock

| Método | Ruta | Rol | Descripción | Req. |
|---|---|---|---|---|
| GET | `/stock` | Todos | Stock por producto y depósito; filtros `productoId` y `depositoId`. | RF-16 |
| GET | `/stock/productos/{id}` | Todos | Stock de un producto: por depósito, total disponible y en tránsito. | RF-16, RF-17 |
| GET | `/stock/transito` | Todos | Stock en tránsito por producto. | RF-17 |

**Cuerpos (JSON).**

- Respuesta de `GET /stock`: una fila por **producto activo**, aunque no tenga stock, ordenada por nombre:

```json
[
  {
    "producto": { "id": 10, "codigo": "BEB-001", "nombre": "Agua mineral 2 L", "unidadMedida": "un", "fraccionable": false },
    "porDeposito": [
      { "deposito": { "id": 1, "nombre": "Central", "activo": true }, "cantidad": 120 },
      { "deposito": { "id": 2, "nombre": "Sucursal Norte", "activo": true }, "cantidad": 36 }
    ],
    "totalDisponible": 156,
    "enTransito": 24
  }
]
```

  `porDeposito` incluye solo los depósitos donde el stock es distinto de cero (RN-16). `totalDisponible` suma solo los depósitos activos (RN-18) y `enTransito` es la suma de las transferencias pendientes del producto (RN-17). Con `productoId` devuelve solo ese producto; con `depositoId`, `porDeposito` incluye solo ese depósito.
- Respuesta de `GET /stock/productos/{id}`: el mismo objeto, para un solo producto (activo o no).
- Respuesta de `GET /stock/transito`: `[{ producto: { id, codigo, nombre, unidadMedida }, enTransito }]`, solo los productos con stock en tránsito.

## 8. Capa analítica

| Método | Ruta | Rol | Descripción | Req. |
|---|---|---|---|---|
| GET | `/analitica/reposicion` | ENCARGADO | Punto de reposición de cada producto activo; con `soloAlertas=true` devuelve solo los productos en alerta, con la cantidad sugerida. | RF-18, RF-19 |
| GET | `/analitica/rotacion/productos` | ENCARGADO | Rotación por producto; parámetros opcionales `desde` y `hasta` (por defecto, los últimos `periodo_analisis_dias`). | RF-20 |
| GET | `/analitica/rotacion/categorias` | ENCARGADO | Rotación por categoría, con los mismos parámetros. | RF-20 |
| GET | `/analitica/abc` | ENCARGADO | Clasificación ABC con valor, porcentaje acumulado y clase de cada producto. | RF-21 |

**Período de análisis.** `desde` y `hasta` son días de Argentina (`AAAA-MM-DD`), ambos inclusivos. Si no se envían, el período son los últimos `periodo_analisis_dias` días, terminando hoy. La fecha de referencia ("hoy" y el inicio de la ventana de consumo) la calcula el backend con su reloj, en UTC (D-16).

**Cuerpos (JSON).**

- Respuesta de `GET /analitica/reposicion` (lista; primero los productos en alerta, después por nombre):

```json
[
  {
    "producto": { "id": 10, "codigo": "CER-002", "nombre": "Cerveza negra 473 ml (pack x6)", "unidadMedida": "pack", "fraccionable": false },
    "proveedor": { "id": 3, "nombre": "Cervecería del Sur", "leadTimeDias": 7, "activo": true },
    "consumoDiario": 2.10,
    "stockSeguridad": 6.00,
    "puntoReposicion": 20.70,
    "stockDisponible": 0.00,
    "enTransito": 0.00,
    "alerta": true,
    "cantidadSugerida": 84
  }
]
```

  `consumoDiario` usa la ventana de `ventana_consumo_dias` (RN-19); `alerta` es `stockDisponible <= puntoReposicion` (RN-23); `cantidadSugerida` sigue RN-24 y se redondea hacia arriba (a entero si el producto no es fraccionable). Los demás valores tienen dos decimales.
- Respuesta de `GET /analitica/rotacion/productos` (de mayor a menor rotación):

```
{ "desde": "2026-08-05", "hasta": "2026-11-02", "productos": [
  { "producto": { id, codigo, nombre, unidadMedida, fraccionable }, "categoria": { id, nombre, activo },
    "salidas": 540.00, "stockInicial": 180.00, "stockFinal": 220.00, "stockPromedio": 200.00, "rotacion": 2.70 } ] }
```

- Respuesta de `GET /analitica/rotacion/categorias`:

```
{ "desde": "...", "hasta": "...", "categorias": [
  { "categoria": { id, nombre, activo }, "productos": 3, "salidas": 980.00, "stockPromedio": 410.00, "rotacion": 2.39 } ] }
```

- Respuesta de `GET /analitica/abc` (de mayor a menor valor de consumo):

```
{ "desde": "...", "hasta": "...", "valorTotal": 34500000.00,
  "productos": [ { "producto": { ... }, "categoria": { ... }, "cantidadSalidas": 540.00, "precioReferencia": 18500.00,
                   "valorConsumo": 9990000.00, "porcentaje": 28.96, "porcentajeAcumulado": 28.96, "clase": "A" } ],
  "resumen": [ { "clase": "A", "productos": 6, "valorConsumo": 30000000.00, "porcentajeValor": 86.96 },
               { "clase": "B", ... }, { "clase": "C", ... } ] }
```

  `porcentaje` es la participación del producto en el valor total y `porcentajeAcumulado` incluye al producto y a los anteriores del ranking; la clase se decide con el acumulado previo (D-12). `resumen` siempre trae las tres clases.


## 9. Parámetros

| Método | Ruta | Rol | Descripción | Req. |
|---|---|---|---|---|
| GET | `/parametros` | ENCARGADO | Lista los parámetros con su valor y descripción. | RF-22 |
| PUT | `/parametros/{clave}` | ENCARGADO | Modifica el valor de un parámetro, validado según RN-37. | RF-22 |

**Cuerpos (JSON).**

- Respuesta de `GET /parametros`: `[{ clave, valor, descripcion }]`, ordenada por clave.
- Pedido de `PUT /parametros/{clave}`: `{ valor }`. Respuesta: `{ clave, valor, descripcion }` con el valor normalizado (por ejemplo, `"75,5"` se guarda como `"75.5"`).
- Errores: `PARAMETRO_INVALIDO` (400) si el valor no cumple RN-37; `CORTES_ABC_INVALIDOS` (400) si `corte_abc_a` quedaría mayor o igual que `corte_abc_b`; 404 si la clave no existe.


## 10. Dashboard

| Método | Ruta | Rol | Descripción | Req. |
|---|---|---|---|---|
| GET | `/dashboard/analitica` | ENCARGADO | Alertas de reposición, resumen ABC (cantidad de productos y valor por clase) y rotación por categoría. | RF-23 |
| GET | `/dashboard/stock` | Todos | Stock por depósito y transferencias pendientes. | RF-24 |
| GET | `/dashboard/evolucion` | ENCARGADO | Cantidades de entradas, salidas y transferencias agrupadas por `DIA`, `SEMANA` o `MES`, entre `desde` y `hasta`. | RF-25 |

**Cuerpos (JSON).**

- Respuesta de `GET /dashboard/analitica`: `{ desde, hasta, alertas, valorConsumoTotal, resumenAbc, rotacionCategorias }`. `alertas` tiene el formato de `/analitica/reposicion` (solo los productos en alerta), `resumenAbc` el de `resumen` de `/analitica/abc` y `rotacionCategorias` el de `categorias` de `/analitica/rotacion/categorias`, todos sobre el período de análisis.
- Respuesta de `GET /dashboard/stock`: `{ stock, pendientes }`, con el formato de `GET /stock` y de `GET /movimientos/transferencias/pendientes`.
- Respuesta de `GET /dashboard/evolucion` (parámetros opcionales `desde`, `hasta` y `agrupacion`; por defecto, el período de análisis agrupado por `SEMANA`):

```
{ "desde": "2026-08-05", "hasta": "2026-11-02", "agrupacion": "SEMANA",
  "puntos": [ { "periodo": "2026-08-03", "entradas": 12, "salidas": 61, "transferencias": 4 } ] }
```

  Cada punto cuenta **movimientos registrados** (no unidades, porque los productos tienen distintas unidades de medida). `periodo` es el primer día del intervalo: el lunes en `SEMANA` y el día 1 en `MES`. Se incluyen los intervalos sin movimientos, con ceros. Si el rango tiene más de 400 puntos para la agrupación elegida, responde 400 con `RANGO_DEMASIADO_GRANDE`.


## 11. Respuestas de error

| Código | Cuándo |
|---|---|
| 400 Bad Request | Datos inválidos: cantidad ≤ 0, cantidad no entera para un producto no fraccionable (RN-44), origen igual a destino, motivo faltante en una salida, parámetro fuera de rango, intento de enviar `fecha` en el cuerpo del pedido. |
| 401 Unauthorized | Token ausente, vencido o inválido; usuario desactivado; credenciales incorrectas en el login. |
| 403 Forbidden | El rol del usuario no tiene permiso sobre el endpoint. |
| 404 Not Found | El recurso no existe. |
| 409 Conflict | Stock insuficiente, transferencia que no está pendiente, producto o depósito inactivo, proveedor o categoría con productos asociados, nombre o código duplicado, baja de un depósito o producto con stock o transferencias pendientes (RN-38, RN-39), baja o cambio a `OPERARIO` del último `ENCARGADO` activo, autodesactivación o cambio del propio rol (RN-40), categoría o proveedor inactivo asignado a un producto (RN-43), paso a no fraccionable con stock no entero (RN-47). |

Todas las respuestas de error usan el mismo formato, generado por el manejador global de excepciones del módulo `common`:

```json
{
  "timestamp": "2026-09-27T14:30:00",
  "status": 409,
  "error": "Conflict",
  "codigo": "STOCK_INSUFICIENTE",
  "mensaje": "El depósito origen tiene 12,00 un disponibles y se intentan mover 20,00.",
  "ruta": "/movimientos/salidas"
}
```
