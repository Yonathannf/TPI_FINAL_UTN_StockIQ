import axios from 'axios';

export const CLAVE_TOKEN = 'stockiq.token';
export const EVENTO_SESION_EXPIRADA = 'stockiq:sesion-expirada';

/**
 * Cliente HTTP único para toda la app. Agrega el token en cada pedido y,
 * si el backend responde 401, avisa para cerrar la sesión.
 * El timeout es largo porque Render (plan gratuito) puede tardar en "despertar".
 */
const cliente = axios.create({
  baseURL: (import.meta.env.VITE_API_URL || 'http://localhost:8080').replace(/\/$/, ''),
  timeout: 60000,
});

cliente.interceptors.request.use((config) => {
  const token = localStorage.getItem(CLAVE_TOKEN);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

cliente.interceptors.response.use(
  (respuesta) => respuesta,
  (error) => {
    const esLogin = error.config?.url?.includes('/auth/login');
    if (error.response?.status === 401 && !esLogin) {
      localStorage.removeItem(CLAVE_TOKEN);
      window.dispatchEvent(new Event(EVENTO_SESION_EXPIRADA));
    }
    return Promise.reject(error);
  }
);

/** Devuelve un mensaje para mostrar al usuario a partir de un error de la API. */
export function mensajeDeError(error) {
  if (error.response?.data?.mensaje) {
    return error.response.data.mensaje;
  }
  if (error.code === 'ECONNABORTED') {
    return 'El servidor tardó demasiado en responder. Intentá de nuevo en unos segundos.';
  }
  if (!error.response) {
    return 'No se pudo conectar con el servidor. Revisá tu conexión.';
  }
  return 'Ocurrió un error inesperado. Intentá de nuevo.';
}

/** Errores por campo que devuelve el backend en las validaciones (código VALIDACION). */
export function erroresDeCampos(error) {
  return error.response?.data?.errores ?? {};
}

export default cliente;
