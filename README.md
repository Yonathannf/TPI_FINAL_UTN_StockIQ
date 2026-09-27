# StockIQ

**Sistema de Gestión de Inventario con Reposición Inteligente**
Tecnicatura Universitaria en Programación — UTN

Repositorio correspondiente al Trabajo Práctico Final de la Tecnicatura Universitaria en Programación de la Universidad Tecnológica Nacional (UTN).

## Integrantes
- Jennifer Franco
- Jonathan Franco

## Tutor/a
- Sofia Raia

---

## Documentación

| # | Documento | Contenido |
|---|---|---|
| 1 | [Requerimientos](docs/01-requerimientos.md) | Actores, requerimientos funcionales y no funcionales, fuera de alcance |
| 2 | [Reglas de negocio](docs/02-reglas-de-negocio.md) | Reglas de movimientos, transferencias, cálculo de stock, capa analítica y parámetros |
| 3 | [Diseño de la base de datos](docs/03-diseno-base-de-datos.md) | Decisiones de diseño, normalización, diccionario de datos, índices y consultas |
| 4 | [Módulos](docs/04-modulos.md) | Paquetes del backend, secciones del frontend y orden de desarrollo |
| 5 | [Diagramas](docs/05-diagramas.md) | DER, casos de uso, secuencia, estados, arquitectura y clases |
| 6 | [API](docs/06-api-endpoints.md) | Endpoints, roles, requerimientos asociados y formato de errores |

Script de base de datos: [`V1__esquema_inicial.sql`](backend/src/main/resources/db/migration/V1__esquema_inicial.sql)

---

## Descripción

En depósitos que operan con múltiples ubicaciones físicas, el control de stock mínimo y máximo suele gestionarse mediante planillas de cálculo desconectadas entre sí, con el punto de pedido definido "a ojo" según la experiencia del encargado. Esto genera quiebres de stock inesperados, capital inmovilizado en productos de baja rotación y nula trazabilidad cuando se mueve mercadería entre depósitos.

StockIQ es una aplicación web de gestión de inventario que registra cada movimiento de stock (entrada, salida y transferencia entre depósitos) con trazabilidad completa, y calcula de forma automática los indicadores clave para la toma de decisiones: punto de reposición, rotación por producto y clasificación ABC por valorización, reemplazando el criterio manual por una recomendación basada en datos reales de consumo.

## Objetivos

### Objetivo general
Desarrollar un sistema web para gestionar el inventario de uno o más depósitos, controlar los movimientos y transferencias de stock, y generar de forma automática alertas y recomendaciones de reposición basadas en el comportamiento real de consumo.

### Objetivos específicos
- Centralizar la información de productos, categorías, proveedores y depósitos.
- Registrar entradas, salidas y transferencias entre depósitos con trazabilidad completa.
- Calcular automáticamente el stock actual y el stock en tránsito a partir del historial de movimientos.
- Calcular el punto de reposición en base al consumo promedio, el lead time del proveedor y un stock de seguridad.
- Clasificar el catálogo mediante análisis ABC por valorización de consumo.
- Medir la rotación de inventario por producto y por categoría.
- Visualizar los indicadores mediante un dashboard analítico con alertas de reposición.

## Actores involucrados

| Actor | Rol en el sistema | Descripción |
|---|---|---|
| Jefe de Depósito / Encargado de Stock | `ENCARGADO` | Usuario principal. Administra productos, categorías, proveedores, depósitos, usuarios y parámetros de cálculo; registra movimientos y consulta el dashboard analítico. |
| Operario de Depósito | `OPERARIO` | Registra entradas, salidas y transferencias, confirma o cancela transferencias y consulta stock e historial. No accede a la configuración ni a la capa analítica. |
| Gerencia / Dirección | Usa el rol `ENCARGADO` | Consumidor del dashboard analítico para decisiones de compra y evaluación de proveedores. No tiene un rol propio en el MVP. |

## Modelo de datos

El modelo se apoya en el registro de movimientos como **fuente única de verdad**: el stock no se guarda como un campo mutable, sino que se deriva de la suma de movimientos. Esto da trazabilidad completa y evita inconsistencias entre lo registrado y lo real. Los movimientos no se editan ni se eliminan; un error se corrige con un movimiento compensatorio.

- **Producto**: código, nombre, categoría, proveedor principal, unidad de medida, precio de referencia, stock de seguridad y estado.
- **Categoría**: agrupa productos para el análisis de rotación.
- **Proveedor**: datos de contacto y lead time (tiempo de entrega prometido), usado para calcular el punto de reposición.
- **Depósito**: nombre, ubicación y estado. Permite calcular el stock por depósito además de a nivel global.
- **Usuario**: credenciales (contraseña con hash BCrypt), nombre, apellido, rol y estado.
- **Movimiento**: tipo (entrada, salida o transferencia), producto, cantidad, fecha, motivo, usuario responsable y depósitos de origen y/o destino según el tipo. Las transferencias tienen además estado (pendiente, confirmada o cancelada), fecha y usuario de resolución.
- **Parámetro**: valores configurables de la capa analítica (ventana de consumo, días de cobertura, período de análisis y cortes ABC).

Productos, proveedores, depósitos y usuarios se dan de baja de forma lógica, para conservar el historial de movimientos.

📄 [Diseño de la base de datos](docs/03-diseno-base-de-datos.md) · [Diagrama Entidad-Relación](docs/05-diagramas.md)

## Alcance del proyecto (MVP)

### Incluye
- Autenticación con usuario y contraseña (JWT) y autorización por roles (encargado, operario) con Spring Security.
- Gestión de usuarios, productos, categorías, proveedores (con lead time) y depósitos.
- Registro de movimientos: entrada, salida y transferencia entre depósitos. Las transferencias nacen pendientes y pueden confirmarse o cancelarse.
- Historial de movimientos paginado, con filtros por producto, depósito, tipo, usuario y rango de fechas.
- Cálculo de stock actual derivado de movimientos, por depósito y total, y del stock en tránsito.
- Punto de reposición calculado automáticamente por producto, con cantidad sugerida a pedir.
- Indicador de rotación por producto y por categoría.
- Clasificación ABC por valorización de consumo acumulado (cortes en 80/95%).
- Parámetros de cálculo configurables sin modificar el código.
- Dashboard analítico con alertas de reposición, ranking ABC y rotación.

### Fuera de alcance (trabajo futuro)
- Gestión de devoluciones de clientes o proveedores.
- Permisos segmentados por depósito.
- Integración con sistemas de facturación o ERP externos.
- Notificaciones automáticas por correo electrónico.
- Mapa del depósito con ubicación de productos en el rack.
- ABC y punto de reposición desagregados por depósito (se calculan a nivel global de la empresa).
- Múltiples proveedores por producto (relación N:M).

## Capa analítica: el diferencial del proyecto

A diferencia de un ABM de stock tradicional, StockIQ no se limita a alertar cuando el stock cae por debajo de un mínimo cargado manualmente: calcula ese mínimo a partir del comportamiento real de consumo y del lead time de cada proveedor, y clasifica automáticamente el catálogo por su impacto económico real.

**Consumo promedio diario**

```
Consumo promedio diario = Salidas de los últimos N días / N
```
N es la ventana de consumo configurable (30 días por defecto). Las transferencias no cuentan como consumo.

**Punto de reposición**

```
Punto de reposición = (Consumo promedio diario × Lead time del proveedor) + Stock de seguridad
```
El sistema propone el mínimo en lugar de que el usuario lo tipee a mano. Se genera una alerta cuando el stock total disponible es menor o igual al punto de reposición.

**Cantidad sugerida a pedir**

```
Cantidad sugerida = Punto de reposición + (Consumo promedio diario × Días de cobertura)
                    − Stock disponible − Stock en tránsito
```
Nunca es menor a 0.

**Rotación (por período)**

```
Rotación = Salidas del período / Stock promedio del período
```
Se calcula sobre el stock promedio del período (promedio entre el stock inicial y el final), no sobre el stock actual, para que el indicador no quede distorsionado por una foto puntual.

**Clasificación ABC por valorización**

| Clase | Criterio | Interpretación |
|---|---|---|
| A | Acumulado hasta el 80% del valor total | Productos críticos: bajo volumen, alto impacto económico. |
| B | Entre 80% y 95% del valor acumulado | Impacto intermedio, control moderado. |
| C | Restante 5% del valor acumulado | Muchos productos, bajo impacto económico individual. |

El valor de consumo de cada producto se calcula como salidas del período × precio de referencia. El producto que cruza un corte queda en la clase superior: la clase se asigna según el porcentaje acumulado previo al producto, de modo que el producto de mayor valor siempre es clase A. Los productos sin consumo quedan en clase C.

**Parámetros configurables**

| Parámetro | Por defecto | Uso |
|---|---|---|
| Ventana de consumo | 30 días | Consumo promedio diario |
| Días de cobertura | 30 días | Cantidad sugerida |
| Período de análisis | 90 días | Rotación y ABC |
| Corte clase A | 80% | ABC |
| Corte clase B | 95% | ABC |

📄 [Reglas de negocio completas](docs/02-reglas-de-negocio.md)

## Dashboard

- Alertas de productos que requieren reposición, con cantidad sugerida.
- Ranking ABC del catálogo por valorización.
- Rotación por producto y por categoría.
- Stock disponible por depósito y transferencias pendientes.
- Evolución de entradas, salidas y transferencias en el tiempo.

## Stack tecnológico

| Componente | Tecnología |
|---|---|
| Backend | Spring Boot (Java 17), arquitectura en capas Controller → Service → Repository, DTOs y mappers. |
| Persistencia | Spring Data JPA + Hibernate. |
| Base de datos | MySQL 8.0.16+ (InnoDB, utf8mb4), con el esquema versionado mediante Flyway. |
| Seguridad | Spring Security, autenticación stateless con JWT, contraseñas con BCrypt y autorización por roles. |
| Frontend | React.js con Vite, consumiendo la API REST del backend. |
| Build y dependencias | Maven, con Lombok para reducir boilerplate en entidades y DTOs. |
| Control de versiones | GitHub (repositorio único centralizado). |
| Contenedores | Docker para el backend; docker-compose para levantar backend + MySQL en un solo comando. |

## Plataforma de despliegue

| Componente | Plataforma | Detalle |
|---|---|---|
| Backend | Render (Web Service con Docker) | Despliegue del contenedor Spring Boot; variables de entorno para credenciales de base de datos y JWT secret. |
| Frontend | Vercel | Build automático de la app React/Vite en cada push a `main`; dominio HTTPS gratuito. |
| Base de datos | Railway (MySQL) | Instancia MySQL gestionada, conectada al backend vía variables de entorno. |

CORS se habilita en Spring Security solo para el dominio del frontend desplegado en Vercel. Con el free tier de Render, el backend puede tardar unos segundos en responder tras un período de inactividad (cold start).

## Estrategia de testing

La lógica analítica (punto de reposición, rotación, clasificación ABC) es la parte del sistema con mayor riesgo de errores silenciosos, así que concentra la mayor parte del esfuerzo de testing.

**Tests unitarios (capa Service)** — JUnit 5 + Mockito, mockeando repositorios para aislar la lógica de negocio: cálculo estándar de punto de reposición, proveedor con lead time 0, producto sin movimientos previos, cantidad sugerida que daría negativa, rotación con stock promedio = 0, catálogo vacío o de un solo producto en ABC, producto que cruza un corte ABC, período sin consumo, transferencia con origen igual a destino, salida que dejaría stock negativo, movimientos sobre productos o depósitos inactivos, y cambio de estado de una transferencia ya resuelta.

**Tests de integración (capa Repository / API)** — Testcontainers con MySQL real y MockMvc: persistencia y agregación de movimientos, endpoints protegidos por rol (403 para `OPERARIO` en configuración de proveedores), flujo completo de transferencia (pendiente → confirmada → impacto en ambos depósitos) y cancelación (pendiente → cancelada → la mercadería vuelve al origen).

**Cobertura** — meta del 80% de línea en la capa Service, con foco en los cálculos de punto de reposición, rotación y ABC, testeados de forma parametrizada.

## Metodología de trabajo

El equipo organiza el desarrollo en sprints quincenales bajo un esquema Kanban, con tablero en GitHub Projects vinculado a issues del repositorio. Cada etapa del plan de trabajo se descompone en issues etiquetados por capa (backend, frontend, analítica, testing), con seguimiento semanal entre los integrantes y la tutoría.

- Ramas de trabajo por feature (`feature/punto-reposicion`, `feature/transferencias`) con integración a `main` vía pull request.
- Convención de commits (`feat:`, `fix:`, `test:`, `docs:`).
- Checklist de Definition of Done por issue: código + test unitario + revisión por el otro integrante antes de mergear.
- Cada entrega queda marcada con un tag (`entrega-1`, `entrega-2`, ...) para conservar una versión fija de lo presentado.

## Plan de trabajo por etapas

| Etapa | Objetivo |
|---|---|
| 1. Análisis, modelado y estructura base | Requerimientos, reglas de negocio, diseño de la base de datos, DER y diagramas, definición de módulos y de la API, estructura del repositorio y definición formal de las fórmulas de la capa analítica. |
| 2. Desarrollo del backend y persistencia | Entidades JPA y repositorios Spring Data, migraciones con Flyway, servicios y endpoints CRUD, lógica de transferencias (pendiente, confirmada y cancelada). |
| 3. Seguridad y desarrollo del frontend | Autenticación JWT y autorización por roles, maquetación en React con navegación protegida, formularios de movimientos y transferencias. |
| 4. Capa analítica y dashboard | Cálculos de punto de reposición, rotación y ABC mediante consultas de agregación, parámetros configurables y gráficos interactivos en el dashboard. |
| 5. Pruebas, validación y despliegue final | Datos de prueba realistas, ejecución de la estrategia de testing, dockerización del backend, despliegue, documentación técnica y presentación final. |

## Estructura del repositorio

```
TPI_FINAL_UTN_StockIQ/
│
├── README.md
│
├── docs/
│   ├── 01-requerimientos.md
│   ├── 02-reglas-de-negocio.md
│   ├── 03-diseno-base-de-datos.md
│   ├── 04-modulos.md
│   ├── 05-diagramas.md
│   └── 06-api-endpoints.md
│
├── backend/
│   ├── src/main/java/com/stockiq/
│   │   ├── config/
│   │   ├── security/
│   │   ├── common/
│   │   ├── usuario/
│   │   ├── catalogo/
│   │   ├── deposito/
│   │   ├── movimiento/
│   │   ├── stock/
│   │   ├── parametro/
│   │   ├── analitica/
│   │   └── dashboard/
│   ├── src/main/resources/db/migration/
│   │   └── V1__esquema_inicial.sql
│   ├── src/test/java/...
│   ├── Dockerfile
│   └── pom.xml
│
├── frontend/
│   ├── src/
│   │   ├── auth/
│   │   ├── catalogo/
│   │   ├── depositos/
│   │   ├── usuarios/
│   │   ├── movimientos/
│   │   ├── stock/
│   │   ├── dashboard/
│   │   ├── configuracion/
│   │   └── shared/
│   └── package.json
│
└── docker-compose.yml
```

📄 [Detalle de cada módulo](docs/04-modulos.md)

## Cómo levantar el proyecto localmente

> Disponible a partir de la etapa de desarrollo. Requiere Docker y Node.js.

```bash
# Clonar el repositorio
git clone https://github.com/Yonathannf/TPI_FINAL_UTN_StockIQ.git
cd TPI_FINAL_UTN_StockIQ

# Backend + base de datos MySQL
docker-compose up -d

# Frontend
cd frontend
npm install
npm run dev
```

El backend queda disponible en `http://localhost:8080` y el frontend en `http://localhost:5173`. Al primer arranque se crea un usuario `ENCARGADO` inicial.

## Entornos desplegados

- Frontend: [Completar URL de Vercel]
- Backend: [Completar URL de Render]
