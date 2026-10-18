import cliente from '../shared/api/cliente.js';

/** Llamadas a /depositos (docs/06-api-endpoints.md, sección 5). */
export const depositosApi = {
  /** `activo`: true, false o undefined (todos). */
  listar: async (activo) => {
    const { data } = await cliente.get('/depositos', { params: { activo } });
    return data;
  },
  crear: async (deposito) => (await cliente.post('/depositos', deposito)).data,
  editar: async (id, deposito) => (await cliente.put(`/depositos/${id}`, deposito)).data,
  desactivar: async (id) => (await cliente.patch(`/depositos/${id}/desactivar`)).data,
  activar: async (id) => (await cliente.patch(`/depositos/${id}/activar`)).data,
};
