import { useEffect, useState } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { LogOut, Menu, X } from 'lucide-react';
import { useAuth } from '../../auth/AuthContext.jsx';
import Logo from './Logo.jsx';
import { INICIO, NOMBRE_ROL, iniciales, seccionesPara, ubicacionDe } from './navegacion.js';
import './layout.css';

function EnlaceMenu({ item, end = false }) {
  const Icono = item.icono;
  return (
    <NavLink to={item.ruta} end={end} className="menu__link">
      <Icono size={19} strokeWidth={1.8} aria-hidden="true" />
      <span>{item.titulo}</span>
    </NavLink>
  );
}

export default function Layout() {
  const { usuario, logout } = useAuth();
  const [menuAbierto, setMenuAbierto] = useState(false);
  const location = useLocation();

  // En celular, el menú se cierra al cambiar de pantalla
  useEffect(() => setMenuAbierto(false), [location.pathname]);

  const grupos = seccionesPara(usuario.rol);
  const { grupo, titulo } = ubicacionDe(location.pathname);

  return (
    <div className="app">
      <aside id="menu-principal" className={`lateral ${menuAbierto ? 'lateral--abierto' : ''}`}>
        <div className="lateral__marca">
          <Logo />
        </div>

        <nav className="menu" aria-label="Secciones">
          <EnlaceMenu item={INICIO} end />
          {grupos.map((g) => (
            <div key={g.grupo} className="menu__grupo">
              <p className="menu__grupo-titulo">{g.grupo}</p>
              {g.items.map((item) => (
                <EnlaceMenu key={item.ruta} item={item} />
              ))}
            </div>
          ))}
        </nav>

        <p className="lateral__pie">Stock real, calculado a partir de cada movimiento registrado.</p>
      </aside>

      {menuAbierto && <div className="velo" onClick={() => setMenuAbierto(false)} aria-hidden="true" />}

      <div className="principal">
        <header className="barra">
          <button
            type="button"
            className="barra__menu"
            aria-label={menuAbierto ? 'Cerrar menú' : 'Abrir menú'}
            aria-expanded={menuAbierto}
            aria-controls="menu-principal"
            onClick={() => setMenuAbierto((v) => !v)}
          >
            {menuAbierto ? <X size={20} /> : <Menu size={20} />}
          </button>

          <div className="barra__ubicacion">
            <p className="barra__grupo">{grupo}</p>
            <p className="barra__titulo">{titulo}</p>
          </div>

          <div className="barra__usuario">
            <span className="avatar" aria-hidden="true">
              {iniciales(usuario)}
            </span>
            <span className="barra__datos">
              <span className="barra__nombre">
                {usuario.nombre} {usuario.apellido}
              </span>
              <span className="barra__rol">{NOMBRE_ROL[usuario.rol]}</span>
            </span>
            <button type="button" className="boton-icono" onClick={logout} title="Cerrar sesión">
              <LogOut size={18} aria-hidden="true" />
              <span className="solo-lectores">Cerrar sesión</span>
            </button>
          </div>
        </header>

        <main className="contenido">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
