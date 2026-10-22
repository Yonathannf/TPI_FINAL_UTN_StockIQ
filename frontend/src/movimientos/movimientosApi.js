import cliente from '../shared/api/cliente.js';

/** Quita los filtros vacíos para no mandarlos como parámetros. */
const sinVacios = (params = {}) =>
  Object.fromEntries(Object.entries(params).filter(([, valor]) => valor !== '' && valor != null));

/** /movimientos (docs/06-api-endpoints.md, sección 6). Los movimientos no se editan ni se borran (RF-14). */
export const movimientosApi = {
  registrarEntrada: async (cuerpo) => (await cliente.post('/movimientos/entradas', cuerpo)).data,
  registrarSalida: async (cuerpo) => (await cliente.post('/movimientos/salidas', cuerpo)).data,
  registrarTransferencia: async (cuerpo) => (await cliente.post('/movimientos/transferencias', cuerpo)).data,

  confirmar: async (id) => (await cliente.patch(`/movimientos/${id}/confirmar`)).data,
  cancelar: async (id) => (await cliente.patch(`/movimientos/${id}/cancelar`)).data,

  /** Historial paginado. Filtros: productoId, depositoId, tipo, usuarioId, desde, hasta, page, size. */
  historial: async (filtros) => (await cliente.get('/movimientos', { params: sinVacios(filtros) })).data,

  /** Transferencias pendientes. Filtro opcional: depositoDestinoId. */
  pendientes: async (filtros) =>
    (await cliente.get('/movimientos/transferencias/pendientes', { params: sinVacios(filtros) })).data,
};
