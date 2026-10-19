const FORMATO_DECIMAL = /^\d+(\.\d{1,2})?$/;

/**
 * Convierte lo que se tipea ("1234,5" o "1234.50") en número con hasta dos decimales.
 * Devuelve null si el texto no es válido (RN-44: como máximo dos decimales).
 */
export function leerDecimal(texto) {
  const limpio = texto.trim().replace(',', '.');
  return FORMATO_DECIMAL.test(limpio) ? Number(limpio) : null;
}

const formatoPrecio = new Intl.NumberFormat('es-AR', {
  style: 'currency',
  currency: 'ARS',
  minimumFractionDigits: 2,
});

const formatoCantidad = new Intl.NumberFormat('es-AR', { minimumFractionDigits: 0, maximumFractionDigits: 2 });

export const formatearPrecio = (valor) => formatoPrecio.format(valor ?? 0);
export const formatearCantidad = (valor) => formatoCantidad.format(valor ?? 0);

/** Número a texto para precargar un campo de formulario. */
export const aTextoDecimal = (valor) => (valor == null ? '' : String(valor).replace('.', ','));
