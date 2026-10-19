import { Link } from 'react-router-dom';
import { Lock } from 'lucide-react';

export default function SinAcceso() {
  return (
    <div className="pagina">
      <div className="tarjeta vacio">
        <span className="icono-cuadro icono-cuadro--alerta" aria-hidden="true">
          <Lock size={20} strokeWidth={1.9} />
        </span>
        <p className="vacio__titulo">No tenés acceso a esta sección</p>
        <p className="vacio__texto">Tu rol no permite ver esta pantalla. Si la necesitás, pedíselo a un encargado.</p>
        <Link to="/" className="boton boton--secundario">
          Volver al inicio
        </Link>
      </div>
    </div>
  );
}
