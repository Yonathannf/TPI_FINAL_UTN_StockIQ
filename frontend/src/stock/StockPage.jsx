import { useCallback, useEffect, useMemo, useState } from 'react';
import { ArrowLeftRight, Boxes, Warehouse } from 'lucide-react';
import { Link } from 'react-router-dom';
import { mensajeDeError } from '../shared/api/cliente.js';
import { formatearCantidad } from '../catalogo/productos/decimales.js';
import { depositosApi } from '../depositos/depositosApi.js';
import PendientesTab from '../movimientos/PendientesTab.jsx';
import { stockApi } from './stockApi.js';
import '../catalogo/catalogo.css';
import '../movimientos/movimientos.css';
import './stock.css';

/**
 * Stock por depósito, total disponible y en tránsito (RF-16, RF-17), y transferencias pendientes (RF-24).
 * Es la pantalla donde el OPERARIO ve el stock; el dashboard analítico es solo del ENCARGADO.
 */
export default function StockPage() {
  const [filas, setFilas] = useState([]);
  const [depositos, setDepositos] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const [texto, setTexto] = useState('');
  const [soloConStock, setSoloConStock] = useState(true);
  const [version, setVersion] = useState(0);

  const cargar = useCallback(async () => {
    setCargando(true);
    setError('');
    try {
      const [stock, deps] = await Promise.all([stockApi.listar(), depositosApi.listar(true)]);
      setFilas(stock);
      setDepositos(deps);
    } catch (err) {
      setError(mensajeDeError(err));
    } finally {
      setCargando(false);
    }
  }, []);

  useEffect(() => {
    cargar();
  }, [cargar, version]);

  const visibles = useMemo(() => {
    const buscado = texto.trim().toLowerCase();
    return filas.filter((f) => {
      const tieneAlgo = Number(f.totalDisponible) !== 0 || Number(f.enTransito) !== 0;
      if (soloConStock && !tieneAlgo) return false;
      if (!buscado) return true;
      return [f.producto.codigo, f.producto.nombre].some((c) => c.toLowerCase().includes(buscado));
    });
  }, [filas, texto, soloConStock]);

  const conStock = filas.filter((f) => Number(f.totalDisponible) > 0).length;
  const enTransito = filas.filter((f) => Number(f.enTransito) > 0).length;

  /** Cantidad del producto en un depósito; 0 si no figura (el backend solo manda los depósitos con stock). */
  const cantidadEn = (fila, depositoId) =>
    Number(fila.porDeposito.find((d) => d.deposito.id === depositoId)?.cantidad ?? 0);

  return (
    <div className="pagina">
      <header className="pagina__encabezado">
        <div>
          <h1>Stock</h1>
          <p className="pagina__bajada">
            Calculado a partir de los movimientos. El total disponible suma los depósitos activos y no incluye lo que
            está en tránsito.
          </p>
        </div>
      </header>

      {error && (
        <p className="alerta alerta--error" role="alert">
          {error}
        </p>
      )}

      <div className="metricas">
        <div className="tarjeta metrica">
          <span className="icono-cuadro" aria-hidden="true">
            <Boxes size={20} />
          </span>
          <span className="metrica__valor">{cargando ? '…' : conStock}</span>
          <span className="metrica__etiqueta">productos con stock disponible</span>
        </div>
        <div className="tarjeta metrica">
          <span className="icono-cuadro" aria-hidden="true">
            <Warehouse size={20} />
          </span>
          <span className="metrica__valor">{cargando ? '…' : depositos.length}</span>
          <span className="metrica__etiqueta">depósitos activos</span>
        </div>
        <div className="tarjeta metrica">
          <span className="icono-cuadro icono-cuadro--alerta" aria-hidden="true">
            <ArrowLeftRight size={20} />
          </span>
          <span className="metrica__valor">{cargando ? '…' : enTransito}</span>
          <span className="metrica__etiqueta">productos con stock en tránsito</span>
        </div>
      </div>

      <div className="tarjeta herramientas">
        <label className="filtro filtro--ancho">
          <span className="filtro__etiqueta">Buscar</span>
          <input
            type="search"
            placeholder="Código o nombre"
            value={texto}
            onChange={(e) => setTexto(e.target.value)}
          />
        </label>
        <label className="filtro filtro--casilla">
          <input type="checkbox" checked={soloConStock} onChange={(e) => setSoloConStock(e.target.checked)} />
          <span>Ocultar productos sin stock</span>
        </label>
      </div>

      <div className="tarjeta tabla__contenedor">
        <table className="tabla tabla--stock">
          <thead>
            <tr>
              <th>Producto</th>
              {depositos.map((d) => (
                <th key={d.id} className="tabla__numero">
                  {d.nombre}
                </th>
              ))}
              <th className="tabla__numero stock__total">Total disponible</th>
              <th className="tabla__numero">En tránsito</th>
            </tr>
          </thead>
          <tbody>
            {cargando && (
              <tr>
                <td colSpan={depositos.length + 3} className="tabla__vacio">
                  Cargando…
                </td>
              </tr>
            )}
            {!cargando && visibles.length === 0 && (
              <tr>
                <td colSpan={depositos.length + 3} className="tabla__vacio">
                  {filas.length === 0
                    ? 'Todavía no hay productos activos.'
                    : texto
                      ? 'Ningún producto coincide con la búsqueda.'
                      : 'No hay productos con stock. Registrá una entrada en Movimientos.'}
                </td>
              </tr>
            )}
            {!cargando &&
              visibles.map((f) => (
                <tr key={f.producto.id}>
                  <td>
                    <strong>{f.producto.nombre}</strong>
                    <div className="tabla__secundario">
                      {f.producto.codigo} · {f.producto.unidadMedida}
                    </div>
                  </td>
                  {depositos.map((d) => {
                    const cantidad = cantidadEn(f, d.id);
                    return (
                      <td key={d.id} className={`tabla__numero ${cantidad === 0 ? 'stock__cero' : ''}`}>
                        {cantidad === 0 ? '—' : formatearCantidad(cantidad)}
                      </td>
                    );
                  })}
                  <td className="tabla__numero stock__total">{formatearCantidad(f.totalDisponible)}</td>
                  <td className="tabla__numero">
                    {Number(f.enTransito) > 0 ? (
                      <span className="insignia insignia--pendiente">{formatearCantidad(f.enTransito)}</span>
                    ) : (
                      <span className="stock__cero">—</span>
                    )}
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>

      <section className="stock__pendientes" aria-labelledby="titulo-pendientes">
        <div className="stock__pendientes-encabezado">
          <h2 id="titulo-pendientes">Transferencias pendientes</h2>
          <Link to="/movimientos?seccion=historial" className="stock__enlace">
            Ver historial de movimientos
          </Link>
        </div>
        <PendientesTab version={version} onCambio={() => setVersion((v) => v + 1)} />
      </section>
    </div>
  );
}
