import { useSearchParams } from 'react-router-dom';
import CategoriasTab from './categorias/CategoriasTab.jsx';
import ProductosTab from './productos/ProductosTab.jsx';
import ProveedoresTab from './proveedores/ProveedoresTab.jsx';
import './catalogo.css';

/** Pestañas del catálogo; la primera es la que se abre por defecto. */
const PESTANAS = [
  { id: 'productos', titulo: 'Productos', Componente: ProductosTab },
  { id: 'categorias', titulo: 'Categorías', Componente: CategoriasTab },
  { id: 'proveedores', titulo: 'Proveedores', Componente: ProveedoresTab },
];

/** Datos maestros del catálogo (RF-04 a RF-06). Solo ENCARGADO; la ruta ya está protegida por rol. */
export default function CatalogoPage() {
  const [params, setParams] = useSearchParams();
  const actual = PESTANAS.find((p) => p.id === params.get('seccion')) ?? PESTANAS[0];
  const { Componente } = actual;

  return (
    <div className="pagina">
      <header className="pagina__encabezado">
        <div>
          <h1>Catálogo</h1>
          <p className="pagina__bajada">Productos, categorías y proveedores del inventario.</p>
        </div>
      </header>

      <div className="pestanas" role="tablist" aria-label="Secciones del catálogo">
        {PESTANAS.map((p) => (
          <button
            key={p.id}
            type="button"
            role="tab"
            id={`pestana-${p.id}`}
            aria-selected={p.id === actual.id}
            aria-controls="panel-catalogo"
            className={`pestana ${p.id === actual.id ? 'pestana--activa' : ''}`}
            onClick={() => setParams({ seccion: p.id }, { replace: true })}
          >
            {p.titulo}
          </button>
        ))}
      </div>

      <div id="panel-catalogo" role="tabpanel" aria-labelledby={`pestana-${actual.id}`}>
        {/* La key vuelve a montar la pestaña al cambiar, así los selects traen categorías y proveedores actualizados */}
        <Componente key={actual.id} />
      </div>
    </div>
  );
}
