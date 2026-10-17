import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import cliente, { CLAVE_TOKEN, EVENTO_SESION_EXPIRADA } from '../shared/api/cliente.js';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [usuario, setUsuario] = useState(null);
  const [cargando, setCargando] = useState(true);

  // Al abrir la app, si hay un token guardado, se valida contra el backend
  useEffect(() => {
    const token = localStorage.getItem(CLAVE_TOKEN);
    if (!token) {
      setCargando(false);
      return;
    }
    cliente
      .get('/auth/me')
      .then(({ data }) => setUsuario(data))
      .catch(() => localStorage.removeItem(CLAVE_TOKEN))
      .finally(() => setCargando(false));
  }, []);

  // Si cualquier pedido devuelve 401 (token vencido o usuario desactivado), se cierra la sesión
  useEffect(() => {
    const alExpirar = () => setUsuario(null);
    window.addEventListener(EVENTO_SESION_EXPIRADA, alExpirar);
    return () => window.removeEventListener(EVENTO_SESION_EXPIRADA, alExpirar);
  }, []);

  const login = useCallback(async (username, password) => {
    const { data } = await cliente.post('/auth/login', { username, password });
    localStorage.setItem(CLAVE_TOKEN, data.token);
    setUsuario(data.usuario);
    return data.usuario;
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem(CLAVE_TOKEN);
    setUsuario(null);
  }, []);

  const valor = useMemo(
    () => ({
      usuario,
      cargando,
      login,
      logout,
      esEncargado: usuario?.rol === 'ENCARGADO',
    }),
    [usuario, cargando, login, logout]
  );

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const contexto = useContext(AuthContext);
  if (!contexto) {
    throw new Error('useAuth debe usarse dentro de <AuthProvider>');
  }
  return contexto;
}
