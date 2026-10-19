import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from './AuthContext.jsx';
import SinAcceso from '../shared/pages/SinAcceso.jsx';

/**
 * Sin sesión: redirige al login y recuerda a dónde quería ir el usuario.
 * Con sesión pero sin el rol necesario: muestra la pantalla de acceso denegado (RF-02).
 */
export default function RutaProtegida({ roles, children }) {
  const { usuario, cargando } = useAuth();
  const location = useLocation();

  if (cargando) {
    return (
      <div className="pantalla-carga" role="status">
        Cargando…
      </div>
    );
  }

  if (!usuario) {
    return <Navigate to="/login" replace state={{ desde: location.pathname }} />;
  }

  if (roles && !roles.includes(usuario.rol)) {
    return <SinAcceso />;
  }

  return children;
}
