import { Navigate, Route, Routes } from 'react-router-dom';
import LoginPage from './auth/LoginPage.jsx';
import RutaProtegida from './auth/RutaProtegida.jsx';
import Layout from './shared/layout/Layout.jsx';
import InicioPage from './shared/pages/InicioPage.jsx';
import PaginaPendiente from './shared/pages/PaginaPendiente.jsx';
import NoEncontrada from './shared/pages/NoEncontrada.jsx';
import { SECCIONES } from './shared/layout/navegacion.js';

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

        {/* Cada sección se reemplaza por su pantalla real a medida que se desarrolla */}
        {SECCIONES.flatMap((grupo) => grupo.items).map((item) => (
          <Route
            key={item.ruta}
            path={item.ruta}
            element={
              <RutaProtegida roles={item.roles}>
                <PaginaPendiente titulo={item.titulo} descripcion={item.descripcion} />
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
