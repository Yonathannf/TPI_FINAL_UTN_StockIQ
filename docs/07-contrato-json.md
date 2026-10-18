# StockIQ — Contrato JSON de los datos maestros

Complementa `06-api-endpoints.md` (rutas, roles y reglas) con la **forma de los cuerpos** que intercambian el backend y el frontend en categorías, proveedores, productos, depósitos y usuarios. Sirve de acuerdo entre quien desarrolla el backend y quien desarrolla el frontend.

## 1. Criterios generales

| Aspecto | Criterio |
|---|---|
| Nombres de campos | `camelCase`, derivados de las columnas de `V1__esquema_inicial.sql` y `V2__ajustes_cierre_analisis.sql` (`lead_time_dias` → `leadTimeDias`). Es el mismo criterio de los DTOs de `usuario`. |
| Listados | `GET` devuelve un array JSON simple, sin paginar, con el filtro `activo` descrito en `06-api-endpoints.md`. |
| Detalle, alta y edición | `GET /{id}`, `POST` y `PUT` devuelven el recurso con la misma forma que el listado. |
| Baja y reactivación | `PATCH /{id}/desactivar` y `PATCH /{id}/activar` devuelven el recurso actualizado. |
| Cuerpos de pedido | No incluyen `id` ni `activo`: el alta siempre crea el registro activo y el estado solo cambia con `desactivar` / `activar` (RF-08). |
| Opcionales | Los campos opcionales pueden venir `null` u omitirse; el backend los guarda como `NULL`. |
| Errores | Formato único de `06-api-endpoints.md`, sección 11. En las validaciones (400, código `VALIDACION`) la respuesta suma `errores`: mapa **nombre de campo → mensaje**, con los mismos nombres de este documento. |

## 2. Depósito (`/depositos`)

| Campo | Tipo | En pedido | En respuesta | Restricciones |
|---|---|---|---|---|
| `id` | número | — | sí | — |
| `nombre` | texto | sí | sí | Obligatorio, máx. 100, único. |
| `ubicacion` | texto | opcional | sí | Máx. 255. |
| `activo` | booleano | — | sí | — |

## 3. Categoría (`/categorias`)

| Campo | Tipo | En pedido | En respuesta | Restricciones |
|---|---|---|---|---|
| `id` | número | — | sí | — |
| `nombre` | texto | sí | sí | Obligatorio, máx. 100, único. |
| `descripcion` | texto | opcional | sí | Máx. 255. |
| `activo` | booleano | — | sí | — |

## 4. Proveedor (`/proveedores`)

| Campo | Tipo | En pedido | En respuesta | Restricciones |
|---|---|---|---|---|
| `id` | número | — | sí | — |
| `nombre` | texto | sí | sí | Obligatorio, máx. 150, único. |
| `contacto` | texto | opcional | sí | Máx. 150. |
| `telefono` | texto | opcional | sí | Máx. 50. |
| `email` | texto | opcional | sí | Máx. 150. |
| `leadTimeDias` | entero | sí | sí | Obligatorio, ≥ 0. Entra en el punto de reposición. |
| `activo` | booleano | — | sí | — |

## 5. Producto (`/productos`)

| Campo | Tipo | En pedido | En respuesta | Restricciones |
|---|---|---|---|---|
| `id` | número | — | sí | — |
| `codigo` | texto | sí | sí | Obligatorio, máx. 50, único. |
| `nombre` | texto | sí | sí | Obligatorio, máx. 150. |
| `categoriaId` | número | sí | sí | Obligatorio; la categoría debe estar activa (RN-43). |
| `categoriaNombre` | texto | — | sí | Solo lectura, para mostrar en listados. |
| `proveedorId` | número | sí | sí | Obligatorio; el proveedor debe estar activo (RN-43). |
| `proveedorNombre` | texto | — | sí | Solo lectura, para mostrar en listados. |
| `unidadMedida` | texto | sí | sí | Obligatorio, máx. 20. |
| `precioReferencia` | decimal | sí | sí | Obligatorio, ≥ 0, hasta 2 decimales. |
| `stockSeguridad` | decimal | sí | sí | ≥ 0, hasta 2 decimales (por defecto 0). |
| `fraccionable` | booleano | sí | sí | Si es `false`, las cantidades del producto son enteras (RN-44). Pasarlo a `false` con stock no entero se rechaza con 409 (RN-47). |
| `activo` | booleano | — | sí | — |

Filtros de `GET /productos`: `categoriaId`, `proveedorId`, `activo` y `texto` (código o nombre).

## 6. Usuario (`/usuarios`)

Ya implementado en el módulo `usuario` (ver `UsuarioRequest`, `UsuarioUpdateRequest` y `UsuarioResponse`).

| Campo | Tipo | Alta (`POST`) | Edición (`PUT`) | Respuesta | Restricciones |
|---|---|---|---|---|---|
| `id` | número | — | — | sí | — |
| `username` | texto | sí | no se modifica | sí | Obligatorio, máx. 60, único (RN-33). |
| `password` | texto | sí | opcional | nunca | Entre 8 y 72 caracteres. En la edición, si no viene, no se cambia. |
| `nombre` | texto | sí | sí | sí | Obligatorio, máx. 100. |
| `apellido` | texto | sí | sí | sí | Obligatorio, máx. 100. |
| `rol` | `ENCARGADO` \| `OPERARIO` | sí | sí | sí | Obligatorio. |
| `activo` | booleano | — | — | sí | — |
