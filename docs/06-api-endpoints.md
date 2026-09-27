# StockIQ — API REST

Contrato de la API del backend. Todas las rutas, salvo el login, requieren el header `Authorization: Bearer <token>`. Los cuerpos de pedido y respuesta se envían en JSON.

## 1. Convenciones

| Aspecto | Criterio |
|---|---|
| Baja lógica | Productos, proveedores, depósitos y usuarios no se eliminan: se usan `PATCH /{id}/desactivar` y `PATCH /{id}/activar` (RF-08). |
| Listados | Los listados de catálogo aceptan el filtro `activo` (`true`, `false` u omitido para todos). |
| Paginación | El historial de movimientos usa `page` (desde 0) y `size` (por defecto 20, máximo 100) y devuelve el total de elementos y de páginas. |
| Fechas | Formato ISO 8601 (`2026-09-27` o `2026-09-27T14:30:00`). |
| Usuario responsable | Se toma del token; nunca se envía en el cuerpo del pedido. |

## 2. Autenticación

| Método | Ruta | Rol | Descripción | Req. |
|---|---|---|---|---|
| POST | `/auth/login` | Público | Recibe usuario y contraseña; devuelve el token JWT, el rol y el nombre del usuario. | RF-01 |

## 3. Usuarios

| Método | Ruta | Rol | Descripción | Req. |
|---|---|---|---|---|
| GET | `/usuarios` | ENCARGADO | Lista los usuarios. | RF-03 |
| GET | `/usuarios/{id}` | ENCARGADO | Detalle de un usuario. | RF-03 |
| POST | `/usuarios` | ENCARGADO | Crea un usuario con su rol (username único, RN-33). | RF-03 |
| PUT | `/usuarios/{id}` | ENCARGADO | Edita nombre, apellido, rol y, opcionalmente, la contraseña. | RF-03 |
| PATCH | `/usuarios/{id}/desactivar` | ENCARGADO | Baja lógica. | RF-03 |
| PATCH | `/usuarios/{id}/activar` | ENCARGADO | Reactiva el usuario. | RF-03 |

## 4. Catálogo

### 4.1 Categorías
| Método | Ruta | Rol | Descripción | Req. |
|---|---|---|---|---|
| GET | `/categorias` | Todos | Lista las categorías. | RF-04 |
| GET | `/categorias/{id}` | Todos | Detalle de una categoría. | RF-04 |
| POST | `/categorias` | ENCARGADO | Crea una categoría. | RF-04 |
| PUT | `/categorias/{id}` | ENCARGADO | Edita una categoría. | RF-04 |
| DELETE | `/categorias/{id}` | ENCARGADO | Elimina una categoría sin productos asociados. | RF-04 |

### 4.2 Proveedores
| Método | Ruta | Rol | Descripción | Req. |
|---|---|---|---|---|
| GET | `/proveedores` | ENCARGADO | Lista los proveedores. | RF-05 |
| GET | `/proveedores/{id}` | ENCARGADO | Detalle de un proveedor. | RF-05 |
| POST | `/proveedores` | ENCARGADO | Crea un proveedor con su lead time. | RF-05 |
| PUT | `/proveedores/{id}` | ENCARGADO | Edita un proveedor. | RF-05 |
| PATCH | `/proveedores/{id}/desactivar` | ENCARGADO | Baja lógica. | RF-05, RN-31 |
| PATCH | `/proveedores/{id}/activar` | ENCARGADO | Reactiva el proveedor. | RF-05 |
| DELETE | `/proveedores/{id}` | ENCARGADO | Elimina un proveedor sin productos asociados. | RN-31 |

### 4.3 Productos
| Método | Ruta | Rol | Descripción | Req. |
|---|---|---|---|---|
| GET | `/productos` | Todos | Lista los productos; filtros `categoriaId`, `proveedorId`, `activo` y `texto` (código o nombre). | RF-06 |
| GET | `/productos/{id}` | Todos | Detalle de un producto. | RF-06 |
| POST | `/productos` | ENCARGADO | Crea un producto. | RF-06 |
| PUT | `/productos/{id}` | ENCARGADO | Edita un producto. | RF-06 |
| PATCH | `/productos/{id}/desactivar` | ENCARGADO | Baja lógica. | RF-08 |
| PATCH | `/productos/{id}/activar` | ENCARGADO | Reactiva el producto. | RF-08 |

## 5. Depósitos

| Método | Ruta | Rol | Descripción | Req. |
|---|---|---|---|---|
| GET | `/depositos` | Todos | Lista los depósitos. | RF-07 |
| GET | `/depositos/{id}` | Todos | Detalle de un depósito. | RF-07 |
| POST | `/depositos` | ENCARGADO | Crea un depósito. | RF-07 |
| PUT | `/depositos/{id}` | ENCARGADO | Edita un depósito. | RF-07 |
| PATCH | `/depositos/{id}/desactivar` | ENCARGADO | Baja lógica. | RF-08 |
| PATCH | `/depositos/{id}/activar` | ENCARGADO | Reactiva el depósito. | RF-08 |

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

## 7. Stock

| Método | Ruta | Rol | Descripción | Req. |
|---|---|---|---|---|
| GET | `/stock` | Todos | Stock por producto y depósito; filtros `productoId` y `depositoId`. | RF-16 |
| GET | `/stock/productos/{id}` | Todos | Stock de un producto: por depósito, total disponible y en tránsito. | RF-16, RF-17 |
| GET | `/stock/transito` | Todos | Stock en tránsito por producto. | RF-17 |

## 8. Capa analítica

| Método | Ruta | Rol | Descripción | Req. |
|---|---|---|---|---|
| GET | `/analitica/reposicion` | ENCARGADO | Punto de reposición de cada producto activo; con `soloAlertas=true` devuelve solo los productos en alerta, con la cantidad sugerida. | RF-18, RF-19 |
| GET | `/analitica/rotacion/productos` | ENCARGADO | Rotación por producto; parámetros opcionales `desde` y `hasta` (por defecto, los últimos `periodo_analisis_dias`). | RF-20 |
| GET | `/analitica/rotacion/categorias` | ENCARGADO | Rotación por categoría, con los mismos parámetros. | RF-20 |
| GET | `/analitica/abc` | ENCARGADO | Clasificación ABC con valor, porcentaje acumulado y clase de cada producto. | RF-21 |

## 9. Parámetros

| Método | Ruta | Rol | Descripción | Req. |
|---|---|---|---|---|
| GET | `/parametros` | ENCARGADO | Lista los parámetros con su valor y descripción. | RF-22 |
| PUT | `/parametros/{clave}` | ENCARGADO | Modifica el valor de un parámetro, validado según RN-37. | RF-22 |

## 10. Dashboard

| Método | Ruta | Rol | Descripción | Req. |
|---|---|---|---|---|
| GET | `/dashboard/analitica` | ENCARGADO | Alertas de reposición, resumen ABC (cantidad de productos y valor por clase) y rotación por categoría. | RF-23 |
| GET | `/dashboard/stock` | Todos | Stock por depósito y transferencias pendientes. | RF-24 |
| GET | `/dashboard/evolucion` | ENCARGADO | Cantidades de entradas, salidas y transferencias agrupadas por `DIA`, `SEMANA` o `MES`, entre `desde` y `hasta`. | RF-25 |

## 11. Respuestas de error

| Código | Cuándo |
|---|---|
| 400 Bad Request | Datos inválidos: cantidad ≤ 0, origen igual a destino, motivo faltante en una salida, parámetro fuera de rango. |
| 401 Unauthorized | Token ausente, vencido o inválido; credenciales incorrectas en el login. |
| 403 Forbidden | El rol del usuario no tiene permiso sobre el endpoint. |
| 404 Not Found | El recurso no existe. |
| 409 Conflict | Stock insuficiente, transferencia que no está pendiente, producto o depósito inactivo, proveedor o categoría con productos asociados, nombre o código duplicado. |

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
