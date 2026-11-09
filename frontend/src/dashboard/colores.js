/**
 * Colores de los gráficos. Las clases A, B y C son ordinales (importancia decreciente), así que van
 * en una sola gama de azul de más oscuro a más claro (pasos 600, 400 y 250 de la paleta de referencia);
 * el validador de paletas la aprueba como rampa ordinal.
 */
export const COLOR_CLASE = { A: '#184f95', B: '#3987e5', C: '#86b6ef' };

/** Texto dentro de un bloque de color: blanco sobre el más oscuro, casi negro sobre los más claros. */
export const TINTA_CLASE = { A: '#ffffff', B: '#0b0b0b', C: '#0b0b0b' };

export const TINTA = '#172036';
export const TINTA_SUAVE = '#66708a';
export const GRILLA = '#e4e8ef';

/** Gráficos de una sola serie: un matiz por medida, tomados de la paleta categórica de referencia. */
export const COLOR_ROTACION = '#eb6834'; // naranja (slot 2)
export const COLOR_DEPOSITO = '#6250d6'; // violeta (slot 7)

/** Series de la evolución de movimientos: los tres primeros matices de la paleta categórica de referencia. */
export const COLOR_MOVIMIENTO = { entradas: '#2a78d6', salidas: '#eb6834', transferencias: '#1baf7a' };
