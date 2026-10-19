import { Boxes } from 'lucide-react';

/** Marca de StockIQ: ícono en cuadro celeste, nombre y subtítulo. */
export default function Logo({ subtitulo = 'Control de inventario' }) {
  return (
    <div className="logo">
      <span className="logo__icono" aria-hidden="true">
        <Boxes size={22} strokeWidth={2} />
      </span>
      <span className="logo__texto">
        <span className="logo__nombre">StockIQ</span>
        <span className="logo__sub">{subtitulo}</span>
      </span>
    </div>
  );
}
