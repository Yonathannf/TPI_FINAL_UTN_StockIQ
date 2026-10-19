import { Link } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext.jsx';
import { NOMBRE_ROL, seccionesPara } from '../layout/navegacion.js';

export default function InicioPage() {
  const { usuario } = useAuth();
  const grupos = seccionesPara(usuario.rol);

  return (
    <div className="pagina">
      <header className="pagina__encabezado">
        <div>
          <h1>Hola, {usuario.nombre}</h1>
          <p className="pagina__bajada">
            Ingresaste como {NOMBRE_ROL[usuario.rol].toLowerCase()}. Estas son las secciones que podés usar.
          </p>
        </div>
      </header>

      {grupos.map((g) => (
        <section key={g.grupo} className="inicio__grupo">
          <h2 className="inicio__grupo-titulo">{g.grupo}</h2>
          <ul className="inicio__lista">
            {g.items.map((item) => {
              const Icono = item.icono;
              return (
                <li key={item.ruta}>
                  <Link to={item.ruta} className="tarjeta tarjeta--enlace inicio__item">
                    <span className="icono-cuadro" aria-hidden="true">
                      <Icono size={20} strokeWidth={1.9} />
                    </span>
                    <span className="inicio__titulo">{item.titulo}</span>
                    <span className="inicio__descripcion">{item.descripcion}</span>
                  </Link>
                </li>
              );
            })}
          </ul>
        </section>
      ))}
    </div>
  );
}
