# StockIQ

## Sistema de Gestión de Inventario con Reposición Inteligente

### Tecnicatura Universitaria en Programación — UTN

Repositorio correspondiente al Trabajo Práctico Final de la **Tecnicatura Universitaria en Programación de la Universidad Tecnológica Nacional (UTN)**.

---

## Integrantes

- **Jennifer Franco**
- **Jonathan Franco**

## Tutor/a

**Sofia Raia**

---

## Descripción

En depósitos que operan con múltiples ubicaciones físicas, el control de stock mínimo y máximo suele gestionarse mediante planillas de cálculo desconectadas entre sí, con el punto de pedido definido "a ojo" según la experiencia del encargado. Esto genera quiebres de stock inesperados, capital inmovilizado en productos de baja rotación y nula trazabilidad cuando se mueve mercadería entre depósitos.

**StockIQ** es una aplicación web de gestión de inventario que registra cada movimiento de stock (entrada, salida y transferencia entre depósitos) con trazabilidad completa, y calcula de forma automática los indicadores clave para la toma de decisiones: **punto de reposición**, **rotación por producto** y **clasificación ABC por valorización**, reemplazando el criterio manual por una recomendación basada en datos reales de consumo.

📄 **[Ver propuesta completa](propuestas/propuesta-stockiq.pdf)**

---

## Objetivos

### Objetivo general

Desarrollar un sistema web para gestionar el inventario de uno o más depósitos, controlar los movimientos y transferencias de stock, y generar de forma automática alertas y recomendaciones de reposición basadas en el comportamiento real de consumo.

### Objetivos específicos

- Centralizar la información de productos, categorías y proveedores.
- Registrar entradas, salidas y transferencias entre depósitos con trazabilidad completa.
- Calcular automáticamente el stock actual a partir del historial de movimientos.
- Calcular el punto de reposición en base al consumo promedio, el lead time del proveedor y un stock de seguridad.
- Clasificar el catálogo mediante análisis ABC por valorización de consumo.
- Medir la rotación de inventario por producto y por categoría.
- Visualizar los indicadores mediante un dashboard analítico con alertas de reposición.

---

## Actores involucrados

| Actor | Rol en el sistema |
|---|---|
| Jefe de Depósito / Encargado de Stock | Usuario principal. Registra movimientos y transferencias, gestiona productos y proveedores, consulta alertas de reposición y el dashboard analítico. |
| Operario de Depósito | Permisos acotados a registrar movimientos de entrada, salida y transferencia, sin acceso a la configuración de productos ni proveedores. |
| Gerencia / Dirección | Consumidor indirecto del dashboard analítico para decisiones de compra y evaluación de proveedores. |

---

## Modelo de datos

El modelo se apoya en el registro de movimientos como fuente única de verdad: el stock no se guarda como un campo mutable, sino que se deriva de la suma de movimientos. Esto da trazabilidad completa y evita inconsistencias entre lo registrado y lo real.

- **Producto**: nombre, categoría, unidad de medida, estado, precio de referencia.
- **Categoría**: agrupa productos para el análisis de rotación y ABC.
- **Proveedor**: datos de contacto y lead time (tiempo de entrega prometido), usado para calcular el punto de reposición.
- **Depósito**: id, nombre y ubicación. Permite que el stock disponible se calcule por depósito además de a nivel global.
- **Movimiento**: tipo (entrada, salida o transferencia), producto, cantidad, fecha, motivo, usuario responsable y, para transferencias, depósito de origen, depósito de destino y estado (pendiente / confirmada).

📄 **[Ver diagrama Entidad-Relación (DER)](docs/der-stockiq.png)**

---

## Alcance del proyecto (MVP)

### Incluye

- Módulo de autenticación con roles (encargado, operario), implementado con Spring Security.
- Gestión de productos, categorías y proveedores (con lead time por proveedor).
- Registro de movimientos: entrada, salida y transferencia entre depósitos, con estado pendiente/confirmada para las transferencias.
- Cálculo de stock actual derivado de movimientos, discriminado por depósito.
- Punto de reposición calculado automáticamente por producto.
- Indicador de rotación por producto y por categoría.
- Clasificación ABC por valorización de consumo acumulado (cortes en 80/95%).
- Dashboard analítico con alertas de reposición, ranking ABC y rotación.

### Fuera de alcance (trabajo futuro)

- Gestión de devoluciones de clientes o proveedores.
- Multi-usuario con permisos segmentados por depósito.
- Integración con sistemas de facturación o ERP externos.
- Notificaciones automáticas por correo electrónico.
- Mapa del depósito con ubicación de productos en el rack.
- ABC y punto de reposición desagregados por depósito (se calculan a nivel global de la empresa).

---

## Capa analítica: el diferencial del proyecto

A diferencia de un ABM de stock tradicional, StockIQ no se limita a alertar cuando el stock cae por debajo de un mínimo cargado manualmente: calcula ese mínimo a partir del comportamiento real de consumo y del lead time de cada proveedor, y clasifica automáticamente el catálogo por su impacto económico real.

**Rotación (por período)**
```
Rotación = Salidas del período / Stock promedio del período
```
Se calcula sobre el stock promedio del período, no sobre el stock actual, para que el indicador no quede distorsionado por una foto puntual.

**Punto de reposición**
```
Punto de reposición = (Consumo promedio diario × Lead time del proveedor) + Stock de seguridad
```
El sistema propone el mínimo en lugar de que el usuario lo tipee a mano.

**Clasificación ABC por valorización**

| Clase | Criterio | Interpretación |
|---|---|---|
| A | Acumulado hasta el 80% del valor total | Productos críticos: bajo volumen, alto impacto económico. |
| B | Entre 80% y 95% del valor acumulado | Impacto intermedio, control moderado. |
| C | Restante 5% del valor acumulado | Muchos productos, bajo impacto económico individual. |

---

## Dashboard

- Alertas de productos que requieren reposición, con cantidad sugerida.
- Ranking ABC del catálogo por valorización.
- Rotación por producto y por categoría.
- Stock disponible por depósito y estado de transferencias.
- Evolución de entradas, salidas y transferencias en el tiempo.
- Historial de movimientos con filtros por producto, depósito y usuario.

---

## Stack tecnológico

| Componente | Tecnología |
|---|---|
| Backend | Spring Boot (Java 17), arquitectura en capas Controller → Service → Repository, DTOs y mappers. |
| Persistencia | Spring Data JPA + Hibernate. |
| Base de datos | MySQL para producción/entrega; H2 en memoria como perfil de desarrollo. |
| Seguridad | Spring Security, autenticación basada en roles (encargado, operario), tokens JWT. |
| Frontend | React.js con Vite, consumiendo la API REST del backend. |
| Build y dependencias | Maven, con Lombok para reducir boilerplate en entidades y DTOs. |
| Control de versiones | GitHub (repositorio único centralizado). |
| Contenedores | Docker para el backend; docker-compose para levantar backend + MySQL en un solo comando. |

---

## Plataforma de Despliegue

| Componente | Plataforma | Detalle |
|---|---|---|
| Backend | Render (Web Service con Docker) | Despliegue del contenedor Spring Boot; variables de entorno para credenciales de base de datos y JWT secret. |
| Frontend | Vercel | Build automático de la app React/Vite en cada push a `main`; dominio HTTPS gratuito. |
| Base de datos | Railway (MySQL) | Instancia MySQL gestionada, conectada al backend vía variables de entorno. |

CORS se habilita en Spring Security para aceptar el dominio del frontend desplegado en Vercel. Con el free tier de Render, el backend puede tardar unos segundos en responder tras un período de inactividad (cold start).

---

## Estrategia de testing

La lógica analítica (punto de reposición, rotación, clasificación ABC) es la parte del sistema con mayor riesgo de errores silenciosos, así que concentra la mayor parte del esfuerzo de testing.

**Tests unitarios (capa Service)** — JUnit 5 + Mockito, mockeando repositorios para aislar la lógica de negocio: cálculo estándar de punto de reposición, proveedor con lead time 0, producto sin movimientos previos, rotación con stock promedio = 0, catálogo vacío o de un solo producto en ABC, transferencia con origen igual a destino, salida que dejaría stock negativo.

**Tests de integración (capa Repository / API)** — Testcontainers con MySQL real y MockMvc: persistencia y agregación de movimientos, endpoints protegidos por rol (403 para OPERARIO en configuración de proveedores), flujo completo de transferencia (pendiente → confirmada → impacto en ambos depósitos).

**Cobertura** — meta del 80% de línea en la capa Service, con foco en los cálculos de punto de reposición, rotación y ABC, testeados de forma parametrizada.

---

## Metodología de trabajo

El equipo organiza el desarrollo en sprints quincenales bajo un esquema Kanban, con tablero en GitHub Projects vinculado a issues del repositorio. Cada etapa del plan de trabajo se descompone en issues etiquetados por capa (backend, frontend, analítica, testing), con seguimiento semanal entre los integrantes y la tutoría.

- Ramas de trabajo por feature (`feature/punto-reposicion`, `feature/transferencias`) con integración a `main` vía pull request.
- Convención de commits (`feat:`, `fix:`, `test:`, `docs:`).
- Checklist de Definition of Done por issue: código + test unitario + revisión por el otro integrante antes de mergear.

---

## Plan de trabajo por etapas

| Etapa | Objetivo |
|---|---|
| 1. Análisis, modelado y estructura base | Modelo de datos, DER, estructura del proyecto Maven y del repositorio, definición formal de las fórmulas de rotación, punto de reposición y ventana de cálculo del consumo promedio. |
| 2. Desarrollo del backend y persistencia | Entidades JPA y repositorios Spring Data, perfiles H2/MySQL, servicios y endpoints CRUD, lógica de transferencia con estado pendiente/confirmada. |
| 3. Seguridad y desarrollo del frontend | Autenticación y autorización por roles, maquetación en React con navegación protegida, formularios de movimientos y transferencias. |
| 4. Capa analítica y dashboard | Cálculos de punto de reposición, rotación y ABC mediante queries JPQL/agregaciones, gráficos interactivos en el dashboard. |
| 5. Pruebas, validación y despliegue final | Datos de prueba realistas, ejecución de la estrategia de testing, dockerización del backend, documentación técnica y presentación final. |

---

## Estructura del repositorio

```text
StockIQ/
│
├── README.md
│
├── propuestas/
│   └── propuesta-stockiq.pdf
│
├── docs/
│   └── der-stockiq.png
│
├── backend/
│   ├── src/main/java/...
│   ├── src/test/java/...
│   └── pom.xml
│
├── frontend/
│   ├── src/
│   └── package.json
│
└── docker-compose.yml
```

---

## Cómo levantar el proyecto localmente

```bash
# Clonar el repositorio
git clone https://github.com/[usuario]/StockIQ.git
cd StockIQ

# Backend + base de datos MySQL
docker-compose up -d

# Frontend
cd frontend
npm install
npm run dev
```

El backend queda disponible en `http://localhost:8080` y el frontend en `http://localhost:5173`.

### Entornos desplegados

- Frontend: `[Completar URL de Vercel]`
- Backend: `[Completar URL de Render]`

---

## Enlace al repositorio

https://github.com/Yonathannf/TPI_FINAL_UTN_StockIQ.git
