-- StockIQ - Migración V2: ajustes al cerrar el análisis (devolución de la segunda entrega)
-- Agrega las columnas que faltaban para resolver RN-38 a RN-47.
-- No hay datos cargados todavía, por lo que los DEFAULT alcanzan para dejar
-- las filas existentes (si hubiera) en un estado consistente.

-- RN-45, RN-46: categoría pasa a tener baja lógica, igual que las demás
-- entidades maestras.
ALTER TABLE categoria
  ADD COLUMN activo BOOLEAN NOT NULL DEFAULT TRUE AFTER descripcion;

-- RN-44, RN-47: si un producto no es fraccionable, sus movimientos deben tener
-- cantidad entera. Se valida en el Service (no con CHECK, porque la
-- condición compara con otra tabla, y MySQL no admite CHECK entre tablas).
ALTER TABLE producto
  ADD COLUMN fraccionable BOOLEAN NOT NULL DEFAULT TRUE AFTER stock_seguridad;
