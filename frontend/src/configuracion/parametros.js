import { leerDecimal } from '../catalogo/productos/decimales.js';

/**
 * Parámetros de cálculo (RF-22) con su texto para la pantalla, sus valores por defecto
 * (los de la migración V1) y el tipo de validación que les corresponde (RN-37).
 */
export const GRUPOS = [
  {
    titulo: 'Consumo y reposición',
    bajada: 'Definen cuándo un producto entra en alerta y cuánto conviene pedir.',
    claves: ['ventana_consumo_dias', 'dias_cobertura'],
  },
  {
    titulo: 'Rotación y clasificación ABC',
    bajada: 'Definen el período que se analiza y dónde se cortan las clases A, B y C.',
    claves: ['periodo_analisis_dias', 'corte_abc_a', 'corte_abc_b'],
  },
];

export const DEFINICIONES = {
  ventana_consumo_dias: {
    etiqueta: 'Ventana de consumo',
    unidad: 'días',
    tipo: 'dias',
    defecto: '30',
    ayuda: 'Días hacia atrás para calcular el consumo promedio diario.',
  },
  dias_cobertura: {
    etiqueta: 'Días de cobertura',
    unidad: 'días',
    tipo: 'dias',
    defecto: '30',
    ayuda: 'Días de consumo que se quiere cubrir al reponer.',
  },
  periodo_analisis_dias: {
    etiqueta: 'Período de análisis',
    unidad: 'días',
    tipo: 'dias',
    defecto: '90',
    ayuda: 'Período para la rotación y la clasificación ABC.',
  },
  corte_abc_a: {
    etiqueta: 'Corte de la clase A',
    unidad: '%',
    tipo: 'porcentaje',
    defecto: '80',
    ayuda: 'Porcentaje acumulado máximo de la clase A.',
  },
  corte_abc_b: {
    etiqueta: 'Corte de la clase B',
    unidad: '%',
    tipo: 'porcentaje',
    defecto: '95',
    ayuda: 'Porcentaje acumulado máximo de la clase B.',
  },
};

/** Valida los valores del formulario según RN-37. Devuelve { clave: mensaje } (vacío si está todo bien). */
export function validarParametros(valores) {
  const errores = {};

  for (const [clave, def] of Object.entries(DEFINICIONES)) {
    const texto = (valores[clave] ?? '').trim();
    if (!texto) {
      errores[clave] = 'Es obligatorio.';
    } else if (def.tipo === 'dias') {
      if (!/^\d+$/.test(texto) || Number(texto) <= 0) errores[clave] = 'Tiene que ser un número entero mayor a 0.';
    } else {
      const n = leerDecimal(texto);
      if (n === null) errores[clave] = 'Ingresá un número con hasta dos decimales.';
      else if (n < 0 || n > 100) errores[clave] = 'Tiene que estar entre 0 y 100.';
    }
  }

  if (!errores.corte_abc_a && !errores.corte_abc_b) {
    if (leerDecimal(valores.corte_abc_a) >= leerDecimal(valores.corte_abc_b)) {
      errores.corte_abc_b = 'El corte de la clase B tiene que ser mayor al de la clase A.';
    }
  }
  return errores;
}
