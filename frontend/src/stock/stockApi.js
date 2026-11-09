import cliente from '../shared/api/cliente.js';

/** /stock (docs/06-api-endpoints.md, sección 7). El stock no se guarda: el backend lo calcula de los movimientos. */
export const stockApi = {
  /** Una fila por producto activo. Filtros opcionales: productoId, depositoId. */
  listar: async (params = {}) => {
    const limpios = Object.fromEntries(Object.entries(params).filter(([, v]) => v !== '' && v != null));
    return (await cliente.get('/stock', { params: limpios })).data;
  },

  /** Stock de un producto: por depósito, total disponible y en tránsito. */
  deProducto: async (productoId) => (await cliente.get(`/stock/productos/${productoId}`)).data,
};
