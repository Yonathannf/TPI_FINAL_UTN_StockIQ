import { crearApiRecurso } from '../../shared/api/recurso.js';

/**
 * /productos (docs/06-api-endpoints.md, sección 4.3).
 * `listar` acepta `categoriaId`, `proveedorId`, `activo` y `texto`. No hay baja física.
 */
export const productosApi = crearApiRecurso('/productos');
