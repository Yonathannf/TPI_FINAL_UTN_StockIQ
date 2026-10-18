import { Navigate, Route, Routes } from 'react-router-dom';
import LoginPage from './auth/LoginPage.jsx';
import RutaProtegida from './auth/RutaProtegida.jsx';
import Layout from './shared/layout/Layout.jsx';
import InicioPage from './shared/pages/InicioPage.jsx';
import PaginaPendiente from './shared/pages/PaginaPendiente.jsx';
import NoEncontrada from './shared/pages/NoEncontrada.jsx';
import { SECCIONES } from './shared/layout/navegacion.js';
import DepositosPage from './depositos/DepositosPage.jsx';

/** Pantallas ya desarrolladas, por ruta. Las que faltan muestran "en desarrollo". */
const PANTALLAS = {
  '/depositos': <DepositosPage />,
};

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />

      <Route
        element={
          <RutaProtegida>
            <Layout />
          </RutaProtegida>
        }
      >
        <Route index element={<InicioPage />} />

        {/* Cada sección se reemplaza por su pantalla real a medida que se desarrolla (ver PANTALLAS) */}
        {SECCIONES.flatMap((grupo) => grupo.items).map((item) => (
          <Route
            key={item.ruta}
            path={item.ruta}
            element={
              <RutaProtegida roles={item.roles}>
                {PANTALLAS[item.ruta] ?? <PaginaPendiente titulo={item.titulo} descripcion={item.descripcion} />}
              </RutaProtegida>
            }
          />
        ))}
      </Route>

      <Route path="/inicio" element={<Navigate to="/" replace />} />
      <Route path="*" element={<NoEncontrada />} />
    </Routes>
  );
}
