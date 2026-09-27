# StockIQ — Requerimientos

Documento de la segunda entrega (TFI). Cierra el análisis previo a la codificación.

## 1. Alcance

StockIQ es una aplicación web para gestionar el inventario de uno o más depósitos. Registra movimientos de stock (entrada, salida, transferencia) con trazabilidad completa y calcula punto de reposición, rotación y clasificación ABC.

## 2. Actores

| Actor | Rol técnico | Descripción |
|---|---|---|
| Jefe de Depósito / Encargado de Stock | `ENCARGADO` | Usuario principal. Administra catálogo, proveedores y depósitos; registra movimientos; consulta el dashboard. |
| Operario de Depósito | `OPERARIO` | Registra movimientos de entrada, salida y transferencia. Sin acceso a configuración. |
| Gerencia / Dirección | (usa el rol `ENCARGADO`) | Consume el dashboard analítico. No tiene un rol propio en el MVP. |

## 3. Requerimientos funcionales

### 3.1 Autenticación y usuarios
| ID | Requerimiento | Rol |
|---|---|---|
| RF-01 | El sistema permite iniciar sesión con usuario y contraseña y devuelve un token JWT. | Todos |
| RF-02 | El sistema restringe el acceso a cada endpoint y pantalla según el rol. | Todos |
| RF-03 | El encargado puede crear, editar y desactivar usuarios y asignarles un rol. | ENCARGADO |

### 3.2 Catálogo
| ID | Requerimiento | Rol |
|---|---|---|
| RF-04 | ABM de categorías. | ENCARGADO |
| RF-05 | ABM de proveedores con datos de contacto y lead time en días. | ENCARGADO |
| RF-06 | ABM de productos: nombre, categoría, proveedor principal, unidad de medida, precio de referencia, stock de seguridad y estado. | ENCARGADO |
| RF-07 | ABM de depósitos: nombre y ubicación. | ENCARGADO |
| RF-08 | Los productos y depósitos se dan de baja de forma lógica (estado), sin borrar el historial. | ENCARGADO |

### 3.3 Movimientos
| ID | Requerimiento | Rol |
|---|---|---|
| RF-09 | Registrar una entrada de un producto en un depósito destino. | ENCARGADO, OPERARIO |
| RF-10 | Registrar una salida de un producto desde un depósito origen. | ENCARGADO, OPERARIO |
| RF-11 | Registrar una transferencia entre dos depósitos; nace en estado PENDIENTE. | ENCARGADO, OPERARIO |
| RF-12 | Confirmar o cancelar una transferencia pendiente. | ENCARGADO, OPERARIO |
| RF-13 | Cada movimiento guarda fecha, motivo, cantidad y usuario responsable. | Todos |
| RF-14 | Los movimientos no se editan ni se eliminan; una corrección se registra como un movimiento nuevo. | Todos |
| RF-15 | Consultar el historial de movimientos filtrando por producto, depósito, tipo, usuario y rango de fechas, con resultados paginados. | Todos |

### 3.4 Stock
| ID | Requerimiento | Rol |
|---|---|---|
| RF-16 | Calcular el stock actual de cada producto a partir de los movimientos, por depósito y en total. | Todos |
| RF-17 | Mostrar el stock en tránsito (transferencias pendientes) por producto. | Todos |

### 3.5 Capa analítica
| ID | Requerimiento | Rol |
|---|---|---|
| RF-18 | Calcular el punto de reposición por producto: (consumo promedio diario × lead time) + stock de seguridad. | ENCARGADO |
| RF-19 | Generar alertas para los productos cuyo stock total es menor o igual al punto de reposición, con cantidad sugerida a pedir. | ENCARGADO |
| RF-20 | Calcular la rotación por producto y por categoría para un período dado. | ENCARGADO |
| RF-21 | Clasificar los productos activos en A, B y C según el valor acumulado de consumo (cortes 80% / 95%). | ENCARGADO |
| RF-22 | Los parámetros de cálculo (ventana de consumo, cortes ABC, días de cobertura) son configurables. | ENCARGADO |

### 3.6 Dashboard
| ID | Requerimiento | Rol |
|---|---|---|
| RF-23 | Mostrar alertas de reposición, ranking ABC y rotación por producto y categoría. | ENCARGADO |
| RF-24 | Mostrar stock por depósito y transferencias pendientes. | Todos |
| RF-25 | Mostrar la evolución de entradas, salidas y transferencias en el tiempo. | ENCARGADO |

## 4. Requerimientos no funcionales

| ID | Categoría | Requerimiento |
|---|---|---|
| RNF-01 | Seguridad | Contraseñas almacenadas con hash BCrypt; comunicación por HTTPS en el entorno desplegado. |
| RNF-02 | Seguridad | Autenticación stateless con JWT; CORS habilitado solo para el dominio del frontend. |
| RNF-03 | Integridad | Toda operación que modifica stock se ejecuta en una transacción. |
| RNF-04 | Trazabilidad | Ningún movimiento se borra ni se edita. |
| RNF-05 | Arquitectura | Backend en capas Controller → Service → Repository, con DTOs y mappers. |
| RNF-06 | Persistencia | MySQL 8.x, motor InnoDB, charset utf8mb4. |
| RNF-07 | Calidad | Cobertura de líneas mínima del 80% en la capa Service. |
| RNF-08 | Rendimiento | Las consultas del dashboard responden en menos de 3 segundos con hasta 50.000 movimientos. |
| RNF-09 | Portabilidad | Backend dockerizado; `docker-compose` levanta backend y MySQL con un solo comando. |
| RNF-10 | Usabilidad | Frontend responsive, con validaciones de formulario y mensajes de error claros. |

## 5. Fuera de alcance

Devoluciones, permisos por depósito, integración con ERP o facturación, notificaciones por correo, mapa del depósito, y ABC / punto de reposición por depósito (se calculan a nivel global).
