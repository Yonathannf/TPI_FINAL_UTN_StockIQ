import cliente from '../shared/api/cliente.js';

/** Llamadas a /parametros (docs/06-api-endpoints.md, sección 9). Solo ENCARGADO. */
export const parametrosApi = {
  /** Lista de { clave, valor, descripcion }. */
  listar: async () => (await cliente.get('/parametros')).data,
  /** El valor se envía como texto: en la base es VARCHAR y el Service lo convierte y valida (RN-37). */
  modificar: async (clave, valor) => (await cliente.put(`/parametros/${clave}`, { valor: String(valor) })).data,
};
