import cliente from '../shared/api/cliente.js';

/** /dashboard (docs/06-api-endpoints.md, sección 10). */
export const dashboardApi = {
  /** ENCARGADO. { desde, hasta, alertas, valorConsumoTotal, resumenAbc, rotacionCategorias } del período de análisis. */
  analitica: async () => (await cliente.get('/dashboard/analitica')).data,

  /** ENCARGADO. Clasificación ABC del período de análisis: { desde, hasta, valorTotal, productos, resumen }. */
  abc: async () => (await cliente.get('/analitica/abc')).data,

  /** ENCARGADO. Rotación por producto del período de análisis: { desde, hasta, productos }. */
  rotacionProductos: async () => (await cliente.get('/analitica/rotacion/productos')).data,

  /** Todos los roles. { stock, pendientes }: mismo formato que GET /stock y que las transferencias pendientes. */
  stock: async () => (await cliente.get('/dashboard/stock')).data,

  /**
   * ENCARGADO. Movimientos registrados por período. `agrupacion`: DIA, SEMANA o MES; `desde` y `hasta`: AAAA-MM-DD.
   * Los parámetros vacíos no se envían (el backend usa el período de análisis agrupado por semana).
   */
  evolucion: async (params = {}) => {
    const limpios = Object.fromEntries(Object.entries(params).filter(([, v]) => v !== '' && v != null));
    return (await cliente.get('/dashboard/evolucion', { params: limpios })).data;
  },
};
