import { crearApiRecurso } from '../shared/api/recurso.js';

/** /usuarios (docs/06-api-endpoints.md, sección 3). No hay baja física: solo desactivar y activar. */
export const usuariosApi = crearApiRecurso('/usuarios');
