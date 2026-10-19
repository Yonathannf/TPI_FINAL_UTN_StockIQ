import { Hammer } from 'lucide-react';

/** Lugar reservado para las secciones que todavía no se desarrollaron. */
export default function PaginaPendiente({ titulo, descripcion }) {
  return (
    <div className="pagina">
      <header className="pagina__encabezado">
        <div>
          <h1>{titulo}</h1>
          <p className="pagina__bajada">{descripcion}</p>
        </div>
      </header>
      <div className="tarjeta vacio">
        <span className="icono-cuadro" aria-hidden="true">
          <Hammer size={20} strokeWidth={1.9} />
        </span>
        <p className="vacio__titulo">Esta sección todavía está en desarrollo</p>
        <p className="vacio__texto">Cuando esté lista, vas a poder usarla desde este mismo lugar.</p>
      </div>
    </div>
  );
}
