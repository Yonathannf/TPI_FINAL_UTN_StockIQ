# StockIQ — Módulos a desarrollar

Cada módulo agrupa una responsabilidad del sistema y se desarrolla en el repositorio como un paquete del backend y, cuando corresponde, una sección del frontend. El backend sigue la arquitectura en capas Controller → Service → Repository, con DTOs y mappers. Los endpoints de cada módulo están detallados en `06-api-endpoints.md`.

## 1. Backend (`backend/src/main/java/com/stockiq/`)

| Módulo (paquete) | Responsabilidad | Contenido principal | Requerimientos |
|---|---|---|---|
| `config` | Configuración transversal. | CORS, beans generales, seeder del usuario `ENCARGADO` inicial. | RNF-02, RNF-06 |
| `security` | Autenticación y autorización. | Filtro JWT, `SecurityConfig`, servicio de tokens, `UserDetailsService`. | RF-01, RF-02, RNF-01 |
| `usuario` | Gestión de usuarios y roles. | Entidad `Usuario`, enum `Rol`, CRUD, controller de auth (`/auth/login`). | RF-01, RF-03 |
| `catalogo` | Datos maestros de productos. | Entidades `Producto`, `Categoria`, `Proveedor`; CRUD de cada una. | RF-04 a RF-06, RF-08 |
| `deposito` | Gestión de depósitos. | Entidad `Deposito`, CRUD. | RF-07, RF-08 |
| `movimiento` | Registro de movimientos y transferencias. | Entidad `Movimiento`, enums `TipoMovimiento` y `EstadoTransferencia`, servicio con validaciones de negocio y bloqueo transaccional (D-13), confirmación y cancelación de transferencias, historial paginado con filtros. | RF-09 a RF-15 |
| `stock` | Cálculo del stock derivado. | Consultas de stock por depósito, total, en tránsito y a una fecha dada. | RF-16, RF-17 |
| `analitica` | Capa analítica. | Servicios de punto de reposición, rotación y ABC; DTOs de alertas y ranking. | RF-18 a RF-21 |
| `parametro` | Parámetros configurables. | Entidad `Parametro`, servicio de lectura y edición con validación (RN-37). | RF-22 |
| `dashboard` | Agregación para el dashboard. | Endpoints que combinan stock, alertas, ABC, rotación y evolución temporal. | RF-23 a RF-25 |
| `common` | Utilidades compartidas. | Manejo global de excepciones, excepciones de negocio, formato de errores. | RNF-05 |

Cada paquete de negocio se organiza internamente en `controller`, `service`, `repository`, `dto`, `mapper` y `model`.

## 2. Frontend (`frontend/src/`)

| Módulo | Responsabilidad | Pantallas | Rol |
|---|---|---|---|
| `auth` | Login, manejo del token y rutas protegidas. | Login | Todos |
| `catalogo` | ABM de productos, categorías y proveedores. | Listados y formularios | ENCARGADO |
| `depositos` | ABM de depósitos. | Listado y formulario | ENCARGADO |
| `usuarios` | ABM de usuarios. | Listado y formulario | ENCARGADO |
| `movimientos` | Registro y consulta de movimientos. | Formularios de entrada, salida y transferencia; transferencias pendientes; historial con filtros | Todos |
| `stock` | Consulta de stock. | Stock por depósito, en tránsito | Todos |
| `dashboard` | Indicadores analíticos. | Alertas, ranking ABC, rotación, evolución temporal | ENCARGADO |
| `configuracion` | Parámetros de cálculo. | Formulario de parámetros | ENCARGADO |
| `shared` | Componentes y utilidades comunes. | Layout, tablas, formularios, cliente HTTP, gráficos | — |

## 3. Transversales

| Elemento | Ubicación | Descripción |
|---|---|---|
| Script de base de datos | `backend/src/main/resources/db/migration/V1__esquema_inicial.sql` | Esquema de MySQL versionado con Flyway (ver `03-diseno-base-de-datos.md`). |
| Docker | `backend/Dockerfile`, `docker-compose.yml` | Backend + MySQL en un solo comando. |
| Tests | `backend/src/test/java/...` | Unitarios (JUnit 5 + Mockito) en `Service`, integración con Testcontainers y MockMvc. |
| Documentación | `docs/` | Requerimientos, reglas, diseño de BD, módulos, diagramas y API. |

## 4. Orden de desarrollo sugerido

1. `config`, `common`, `usuario` y `security` (base para todo lo demás).
2. `catalogo` y `deposito` (datos maestros).
3. `movimiento` y `stock` (núcleo del sistema).
4. `parametro` y `analitica` (diferencial del proyecto).
5. `dashboard` y frontend en paralelo a partir del módulo 3.
6. Testing continuo por módulo; despliegue al final.
