import { useCallback, useEffect, useState } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { useAuth } from '../auth/AuthContext.jsx';
import { mensajeDeError } from '../shared/api/cliente.js';
import { formatearFechaHora } from '../shared/format/fechas.js';
import { formatearCantidad } from '../catalogo/productos/decimales.js';
import { productosApi } from '../catalogo/productos/productosApi.js';
import { depositosApi } from '../depositos/depositosApi.js';
import { usuariosApi } from '../usuarios/usuariosApi.js';
import { movimientosApi } from './movimientosApi.js';
import { InsigniaTipo, Recorrido, TIPOS, nombreCompleto } from './tipos.jsx';

const TAMANIO_PAGINA = 20;
const FILTROS_VACIOS = { productoId: '', depositoId: '', tipo: '', usuarioId: '', desde: '', hasta: '' };

/** Historial de movimientos con filtros y paginación (RF-15). `version` cambia cuando hay que recargar. */
export default function HistorialTab({ version = 0 }) {
  const { esEncargado } = useAuth();

  const [filtros, setFiltros] = useState(FILTROS_VACIOS);
  const [pagina, setPagina] = useState(0);
  const [resultado, setResultado] = useState(null); // { contenido, pagina, totalPaginas, totalElementos }
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');

  const [productos, setProductos] = useState([]);
  const [depositos, setDepositos] = useState([]);
  const [usuarios, setUsuarios] = useState([]);

  // Opciones de los filtros: todos, activos o no, porque el historial incluye movimientos viejos
  useEffect(() => {
    productosApi.listar({}).then(setProductos).catch(() => setProductos([]));
    depositosApi.listar(undefined).then(setDepositos).catch(() => setDepositos([]));
    // El listado de usuarios es solo del ENCARGADO (RN-34): el operario no tiene este filtro
    if (esEncargado) usuariosApi.listar({}).then(setUsuarios).catch(() => setUsuarios([]));
  }, [esEncargado]);

  const rangoInvalido = filtros.desde && filtros.hasta && filtros.desde > filtros.hasta;

  const cargar = useCallback(async () => {
    if (rangoInvalido) return;
    setCargando(true);
    setError('');
    try {
      setResultado(await movimientosApi.historial({ ...filtros, page: pagina, size: TAMANIO_PAGINA }));
    } catch (err) {
      setError(mensajeDeError(err));
    } finally {
      setCargando(false);
    }
  }, [filtros, pagina, rangoInvalido]);

  useEffect(() => {
    cargar();
  }, [cargar, version]);

  function cambiarFiltro(campo, valor) {
    setFiltros((anteriores) => ({ ...anteriores, [campo]: valor }));
    setPagina(0);
  }

  const hayFiltros = Object.values(filtros).some((v) => v !== '');
  const movimientos = resultado?.contenido ?? [];
  const totalPaginas = resultado?.totalPaginas ?? 0;

  return (
    <>
      {error && (
        <p className="alerta alerta--error" role="alert">
          {error}
        </p>
      )}

      <div className="tarjeta herramientas herramientas--envolver">
        <label className="filtro">
          <span className="filtro__etiqueta">Producto</span>
          <select value={filtros.productoId} onChange={(e) => cambiarFiltro('productoId', e.target.value)}>
            <option value="">Todos</option>
            {productos.map((p) => (
              <option key={p.id} value={p.id}>
                {p.codigo} — {p.nombre}
              </option>
            ))}
          </select>
        </label>

        <label className="filtro">
          <span className="filtro__etiqueta">Depósito</span>
          <select value={filtros.depositoId} onChange={(e) => cambiarFiltro('depositoId', e.target.value)}>
            <option value="">Todos</option>
            {depositos.map((d) => (
              <option key={d.id} value={d.id}>
                {d.nombre}
              </option>
            ))}
          </select>
        </label>

        <label className="filtro">
          <span className="filtro__etiqueta">Tipo</span>
          <select value={filtros.tipo} onChange={(e) => cambiarFiltro('tipo', e.target.value)}>
            <option value="">Todos</option>
            {Object.entries(TIPOS).map(([clave, t]) => (
              <option key={clave} value={clave}>
                {t.etiqueta}
              </option>
            ))}
          </select>
        </label>

        {esEncargado && (
          <label className="filtro">
            <span className="filtro__etiqueta">Usuario</span>
            <select value={filtros.usuarioId} onChange={(e) => cambiarFiltro('usuarioId', e.target.value)}>
              <option value="">Todos</option>
              {usuarios.map((u) => (
                <option key={u.id} value={u.id}>
                  {nombreCompleto(u)}
                </option>
              ))}
            </select>
          </label>
        )}

        <label className="filtro">
          <span className="filtro__etiqueta">Desde</span>
          <input type="date" value={filtros.desde} onChange={(e) => cambiarFiltro('desde', e.target.value)} />
        </label>

        <label className="filtro">
          <span className="filtro__etiqueta">Hasta</span>
          <input
            type="date"
            value={filtros.hasta}
            onChange={(e) => cambiarFiltro('hasta', e.target.value)}
            aria-invalid={Boolean(rangoInvalido)}
          />
        </label>

        {hayFiltros && (
          <button type="button" className="boton boton--secundario" onClick={() => { setFiltros(FILTROS_VACIOS); setPagina(0); }}>
            Limpiar filtros
          </button>
        )}
      </div>

      {rangoInvalido && (
        <p className="alerta alerta--error" role="alert">
          La fecha "desde" no puede ser posterior a la fecha "hasta".
        </p>
      )}

      <div className="tarjeta tabla__contenedor">
        <table className="tabla">
          <thead>
            <tr>
              <th>Fecha</th>
              <th>Tipo</th>
              <th>Producto</th>
              <th className="tabla__numero">Cantidad</th>
              <th>Depósito</th>
              <th>Motivo</th>
              <th>Usuario</th>
            </tr>
          </thead>
          <tbody>
            {cargando && (
              <tr>
                <td colSpan={7} className="tabla__vacio">
                  Cargando…
                </td>
              </tr>
            )}
            {!cargando && movimientos.length === 0 && (
              <tr>
                <td colSpan={7} className="tabla__vacio">
                  {hayFiltros ? 'Ningún movimiento coincide con los filtros.' : 'Todavía no hay movimientos registrados.'}
                </td>
              </tr>
            )}
            {!cargando &&
              movimientos.map((m) => (
                <tr key={m.id}>
                  <td className="mov-fecha">{formatearFechaHora(m.fecha)}</td>
                  <td>
                    <InsigniaTipo movimiento={m} />
                  </td>
                  <td>
                    <strong>{m.producto.nombre}</strong>
                    <div className="tabla__secundario">{m.producto.codigo}</div>
                  </td>
                  <td className="tabla__numero">
                    {formatearCantidad(m.cantidad)} <span className="tabla__secundario">{m.producto.unidadMedida}</span>
                  </td>
                  <td>
                    <Recorrido movimiento={m} />
                  </td>
                  <td>{m.motivo || '—'}</td>
                  <td>{nombreCompleto(m.usuario)}</td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>

      {totalPaginas > 1 && (
        <nav className="paginacion" aria-label="Páginas del historial">
          <button
            type="button"
            className="boton boton--secundario"
            onClick={() => setPagina((p) => p - 1)}
            disabled={pagina === 0 || cargando}
          >
            <ChevronLeft size={16} aria-hidden="true" />
            Anterior
          </button>
          <span className="paginacion__estado">
            Página {pagina + 1} de {totalPaginas} · {resultado.totalElementos} movimientos
          </span>
          <button
            type="button"
            className="boton boton--secundario"
            onClick={() => setPagina((p) => p + 1)}
            disabled={pagina + 1 >= totalPaginas || cargando}
          >
            Siguiente
            <ChevronRight size={16} aria-hidden="true" />
          </button>
        </nav>
      )}
    </>
  );
}
