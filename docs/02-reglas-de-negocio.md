# StockIQ — Reglas de negocio

Los identificadores de las reglas se mantienen estables para no romper las referencias desde otros documentos. Las reglas agregadas al cerrar el análisis usan números nuevos (RN-36 a RN-47).

## 1. Movimientos

| ID | Regla |
|---|---|
| RN-01 | Todo movimiento tiene tipo (`ENTRADA`, `SALIDA`, `TRANSFERENCIA`), producto, cantidad, fecha y usuario responsable. |
| RN-02 | La cantidad de un movimiento es siempre mayor a 0. |
| RN-03 | Una `ENTRADA` tiene solo depósito destino. |
| RN-04 | Una `SALIDA` tiene solo depósito origen. |
| RN-05 | Una `TRANSFERENCIA` tiene depósito origen y destino, y deben ser distintos. |
| RN-06 | Una `SALIDA` o `TRANSFERENCIA` no puede dejar el stock del depósito origen en negativo. |
| RN-07 | Los movimientos no se editan ni se eliminan. Un error se corrige con un movimiento compensatorio. |
| RN-08 | No se pueden registrar movimientos sobre productos o depósitos inactivos. |
| RN-09 | El motivo es obligatorio en salidas (ej.: venta, rotura, ajuste) y opcional en entradas y transferencias. |
| RN-41 | La fecha de un movimiento la asigna siempre el servidor, en el momento de crearlo (`CURRENT_TIMESTAMP` en UTC); la API nunca acepta una fecha enviada por el cliente. Las ventanas de la capa analítica (`ventana_consumo_dias`, `periodo_analisis_dias`) también se calculan en UTC. Para los datos de prueba con fechas pasadas que requiere la etapa 5, los movimientos se insertan directamente en la base (script de carga), fuera de la API. La fecha se almacena en UTC; para mostrarla, para filtrar el historial por fecha (`desde` / `hasta`) y para agrupar por día en el dashboard, se convierte a la hora de Argentina (`America/Argentina/Buenos_Aires`). |
| RN-44 | La cantidad de un movimiento tiene como máximo dos decimales. Además, si el producto no admite fracciones (`producto.fraccionable = false`), la cantidad debe ser un número entero. |

## 2. Transferencias

| ID | Regla |
|---|---|
| RN-10 | Al crearse, una transferencia queda `PENDIENTE` y la mercadería se descuenta del origen (stock en tránsito). |
| RN-11 | Al confirmarse (`CONFIRMADA`), la mercadería se suma al depósito destino. |
| RN-12 | Una transferencia pendiente puede cancelarse (`CANCELADA`); la mercadería vuelve al origen. |
| RN-13 | Una transferencia confirmada o cancelada no puede cambiar de estado. |
| RN-14 | Se registran el usuario y la fecha de la confirmación o cancelación. |

## 3. Cálculo de stock

| ID | Regla |
|---|---|
| RN-15 | El stock no se guarda como campo; se deriva de los movimientos. |
| RN-16 | Stock de un producto en un depósito = entradas al depósito + transferencias confirmadas hacia el depósito − salidas del depósito − transferencias (pendientes o confirmadas) desde el depósito. |
| RN-17 | Stock en tránsito = suma de las transferencias pendientes. |
| RN-18 | Stock total disponible = suma del stock de todos los depósitos activos (no incluye el tránsito). |
| RN-36 | **Stock a una fecha F** (usado en la rotación): se aplica RN-16 considerando solo los movimientos con fecha menor o igual a F, sobre **todos** los depósitos (activos e inactivos a la fecha de la consulta). Una transferencia descuenta del origen si se creó hasta F y no fue cancelada hasta F; suma al destino solo si fue confirmada hasta F (según su `fecha_resolucion`). |

> **Nota:** RN-18 (stock disponible hoy, para alertas) y RN-36 (stock histórico, para rotación) usan criterios distintos a propósito: RN-18 responde "cuánto tengo disponible para operar ahora", por lo que excluye depósitos dados de baja; RN-36 responde "cuánto había en un momento del pasado", y un depósito que se desactivó después de ese momento no cambia lo que realmente había entonces. Ver D-17 en `03-diseno-base-de-datos.md`.

## 4. Capa analítica

| ID | Regla |
|---|---|
| RN-19 | **Consumo promedio diario** = salidas de los últimos N días / N. N es el parámetro `ventana_consumo_dias` (por defecto 30). Las transferencias no cuentan como consumo. |
| RN-20 | **Punto de reposición** = (consumo promedio diario × lead time del proveedor) + stock de seguridad. |
| RN-21 | Si el producto no tiene movimientos de salida en la ventana, el consumo es 0 y el punto de reposición es igual al stock de seguridad. |
| RN-22 | Si el lead time del proveedor es 0, el punto de reposición es igual al stock de seguridad. |
| RN-23 | Hay alerta cuando el stock total disponible es menor o igual al punto de reposición. |
| RN-24 | **Cantidad sugerida** = punto de reposición + (consumo promedio diario × `dias_cobertura`) − stock total disponible − stock en tránsito. Nunca menor a 0. `dias_cobertura` por defecto: 30. |
| RN-25 | **Rotación** = salidas del período / stock promedio del período. El stock promedio es el promedio entre el stock al inicio y al final del período (calculados según RN-36). Si el stock promedio es 0, la rotación es 0. |
| RN-26 | La rotación por categoría usa la suma de salidas y de stock promedio de sus productos, considerando solo los productos activos — igual criterio que el ABC (RN-28). |
| RN-27 | **ABC**: se calcula el valor de consumo de cada producto (salidas del período × precio de referencia) y se ordena de mayor a menor. Para cada producto se calcula el porcentaje acumulado *previo*, es decir, la suma de los productos que lo anteceden sobre el total. Si ese acumulado previo es menor a `corte_abc_a` (80%), el producto es clase A; si es menor a `corte_abc_b` (95%), clase B; si no, clase C. El producto que cruza un corte queda en la clase superior. Ante igual valor se desempata por id. |
| RN-28 | En el ABC se consideran solo los productos activos. Un producto sin consumo queda en clase C; si ningún producto tuvo consumo en el período, todos son clase C. Con un solo producto con consumo, ese producto es clase A. Con catálogo vacío, el resultado es vacío. |
| RN-29 | El ABC y el punto de reposición se calculan a nivel global, no por depósito. |

> **Limitación conocida:** en el MVP, todas las salidas cuentan como consumo (RN-19), incluidas las de motivo "ajuste" usadas para corregir errores de registro (RN-07). Esto puede sobrestimar temporalmente el consumo, el punto de reposición, la cantidad sugerida, la rotación y el valor ABC de un producto mientras el ajuste esté dentro de la ventana de cálculo. Lo mismo ocurre cuando se corrige una salida registrada de más con una entrada de ajuste: la salida errónea sigue contando como consumo, porque las entradas no lo descuentan. Como mejora se propone clasificar los motivos e indicar cuáles cuentan como consumo (por ejemplo, una tabla `motivo` con un campo `cuenta_como_consumo`), a incorporar en una migración posterior.

> **Limitación conocida:** el ABC valoriza todas las salidas del período con el precio de referencia *actual* del producto (RN-27). Si el precio cambia, el ranking se recalcula como si el producto siempre hubiera tenido ese precio. Como mejora se propone guardar el precio en cada movimiento de salida.

## 5. Catálogo y usuarios

| ID | Regla |
|---|---|
| RN-30 | Todo producto tiene una categoría y un proveedor principal. |
| RN-31 | Un proveedor con productos asociados no puede eliminarse; solo desactivarse. |
| RN-32 | El precio de referencia y el stock de seguridad no pueden ser negativos. |
| RN-33 | El nombre de usuario es único. |
| RN-34 | Solo el `ENCARGADO` puede gestionar productos, categorías, proveedores, depósitos, usuarios y parámetros. |
| RN-35 | El `OPERARIO` puede registrar movimientos y consultar stock, pero no ve la capa analítica. |
| RN-38 | Un depósito no puede desactivarse mientras tenga stock distinto de cero en algún producto (RN-16) o transferencias pendientes con origen o destino en él. |
| RN-39 | Un producto no puede desactivarse mientras tenga stock distinto de cero en algún depósito, stock en tránsito (RN-17), o transferencias pendientes asociadas. |
| RN-40 | No se puede desactivar ni cambiar a `OPERARIO` al último usuario `ENCARGADO` activo del sistema. Ningún usuario puede desactivarse ni cambiarse el rol a sí mismo, sin importar su rol ni cuántos `ENCARGADO` queden activos. |
| RN-42 | Un producto activo puede tener como proveedor principal a un proveedor inactivo (no se fuerza su baja en cascada ni se bloquea la reactivación). El lead time usado en el punto de reposición (RN-20) es el que tenga cargado ese proveedor, esté activo o no. |
| RN-43 | No se puede asignar una categoría ni un proveedor inactivo al crear o editar un producto. Los productos que ya los tenían asignados los conservan (RN-42) y siguen participando de los cálculos con normalidad; la rotación por categoría incluye también a las categorías inactivas que tengan productos activos. |
| RN-45 | Una categoría con productos asociados no puede eliminarse; solo desactivarse. |
| RN-46 | Criterio unificado de baja: **categoría** y **proveedor** admiten eliminación física (`DELETE`) únicamente cuando no tienen productos asociados (RN-45, RN-31); si tienen, solo se desactivan. **Producto**, **depósito** y **usuario** solo se dan de baja de forma lógica (RF-08): el MVP no expone un `DELETE` físico para estas tres entidades. |
| RN-47 | Un producto solo puede pasar a `fraccionable = false` si su stock en cada depósito y su stock en tránsito son números enteros. |

## 6. Parámetros configurables

| ID | Regla |
|---|---|
| RN-37 | Los parámetros de días (`ventana_consumo_dias`, `dias_cobertura`, `periodo_analisis_dias`) son enteros mayores a 0. Los cortes ABC son números entre 0 y 100, y `corte_abc_a` debe ser menor que `corte_abc_b`. |

| Clave | Valor por defecto | Uso |
|---|---|---|
| `ventana_consumo_dias` | 30 | RN-19 |
| `dias_cobertura` | 30 | RN-24 |
| `periodo_analisis_dias` | 90 | RN-25 y RN-27 |
| `corte_abc_a` | 80 | RN-27 |
| `corte_abc_b` | 95 | RN-27 |
