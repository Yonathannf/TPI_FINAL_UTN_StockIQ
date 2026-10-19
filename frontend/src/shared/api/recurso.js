import cliente from './cliente.js';

/**
 * Llamadas CRUD de un recurso con baja lógica (docs/06-api-endpoints.md).
 * `ruta` es la base, por ejemplo '/categorias'. Se usa para categorías, proveedores y productos.
 */
export function crearApiRecurso(ruta) {
  return {
    /** `params` puede incluir `activo` (true, false o undefined) y los filtros propios del recurso. */
    listar: async (params) => (await cliente.get(ruta, { params })).data,
    obtener: async (id) => (await cliente.get(`${ruta}/${id}`)).data,
    crear: async (cuerpo) => (await cliente.post(ruta, cuerpo)).data,
    editar: async (id, cuerpo) => (await cliente.put(`${ruta}/${id}`, cuerpo)).data,
    desactivar: async (id) => (await cliente.patch(`${ruta}/${id}/desactivar`)).data,
    activar: async (id) => (await cliente.patch(`${ruta}/${id}/activar`)).data,
    /** Solo categorías y proveedores sin productos asociados (RN-46). */
    eliminar: async (id) => {
      await cliente.delete(`${ruta}/${id}`);
    },
  };
}
