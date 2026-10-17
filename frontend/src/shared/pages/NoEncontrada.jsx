import { Link } from 'react-router-dom';
import Logo from '../layout/Logo.jsx';

export default function NoEncontrada() {
  return (
    <main className="pantalla-suelta">
      <div className="tarjeta vacio vacio--suelto">
        <Logo />
        <p className="vacio__titulo">No encontramos esta página</p>
        <p className="vacio__texto">Puede que la dirección esté mal escrita o que la página ya no exista.</p>
        <Link to="/" className="boton boton--primario">
          Ir al inicio
        </Link>
      </div>
    </main>
  );
}
