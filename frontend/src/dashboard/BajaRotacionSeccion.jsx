import { useEffect, useMemo, useState } from 'react';
import { mensajeDeError } from '../shared/api/cliente.js';
import { formatearCantidad } from '../catalogo/productos/decimales.js';
import BarrasHorizontales from './BarrasHorizontales.jsx';
import { COLOR_CAPITAL } from './colores.js';
import { dashboardApi } from './dashboardApi.js';

/** Rotación por debajo de este valor: el stock no se renovó ni una vez en el período. */
const UMBRAL_BAJA_ROTACION = 1;
const CANTIDAD_EN_GRAFICO = 10;

const pesos = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS', maximumFractionDigits: 0 });
const n = (valor) => Number(valor ?? 0);

/**
 * Productos con stock que rotaron menos de una vez en el período, y cuánta plata tienen parada.
 * Capital inmovilizado = stock disponible actual × precio de referencia (stock de hoy, no el promedio del período).
 * Cruza /analitica/rotacion/productos (rotación), /dashboard/stock (stock actual) y /analitica/abc (precios).
 */
export default function BajaRotacionSeccion({ abc, stock }) {
  const [rotacion, setRotacion] = useState(null);
  const [error, setError] = useState('');
  const [verTodos, setVerTodos] = useState(false);

  useEffect(() => {
    let vigente = true;
    dashboardApi
      .rotacionProductos()
      .then((r) => vigente && setRotacion(r))
      .catch((err) => vigente && setError(mensajeDeError(err)));
    return () => {
      vigente = false;
    };
  }, []);

  const { filas, capitalBaja, capitalTotal } = useMemo(() => {
    if (!rotacion) return { filas: [], capitalBaja: 0, capitalTotal: 0 };
    const precios = new Map(abc.productos.map((p) => [p.producto.id, n(p.precioReferencia)]));
    const rotaciones = new Map(rotacion.productos.map((r) => [r.producto.id, r]));

    let total = 0;
    const lista = [];
    for (const s of stock.stock) {
      const disponible = n(s.totalDisponible);
      if (disponible <= 0) continue;
      const capital = disponible * (precios.get(s.producto.id) ?? 0);
      total += capital;
      const r = rotaciones.get(s.producto.id);
      if (!r || n(r.rotacion) >= UMBRAL_BAJA_ROTACION) continue;
      lista.push({
        producto: s.producto,
        categoria: r.categoria,
        disponible,
        salidas: n(r.salidas),
        rotacion: n(r.rotacion),
        capital,
      });
    }
    lista.sort((a, b) => b.capital - a.capital);
    return { filas: lista, capitalBaja: lista.reduce((suma, f) => suma + f.capital, 0), capitalTotal: total };
  }, [rotacion, abc, stock]);

  const porcentaje = capitalTotal > 0 ? (capitalBaja / capitalTotal) * 100 : 0;
  const principales = filas.slice(0, CANTIDAD_EN_GRAFICO);

  return (
    <section className="dashboard__seccion" aria-labelledby="titulo-baja-rotacion">
      <div className="dashboard__titulo">
        <h2 id="titulo-baja-rotacion">Baja rotación y capital inmovilizado</h2>
        <span className="dashboard__nota">
          Productos con stock y rotación menor a {UMBRAL_BAJA_ROTACION} en el período.
        </span>
      </div>

      {error && (
        <p className="alerta alerta--error" role="alert">
          {error}
        </p>
      )}

      {!rotacion && !error && <p className="tabla__vacio">Cargando…</p>}

      {rotacion && filas.length === 0 && (
        <div className="tarjeta grafico">
          <p className="tabla__vacio">Ningún producto con stock tiene rotación menor a {UMBRAL_BAJA_ROTACION} en el período.</p>
        </div>
      )}

      {rotacion && filas.length > 0 && (
        <>
          <div className="tarjeta indicador indicador--ancho">
            <div className="indicador__texto">
              <span className="indicador__valor">{pesos.format(capitalBaja)}</span>
              <span className="indicador__etiqueta">
                Capital inmovilizado en {filas.length} {filas.length === 1 ? 'producto' : 'productos'} de baja
                rotación ({formatearCantidad(porcentaje)}% del valor del stock actual)
              </span>
            </div>
          </div>

          <BarrasHorizontales
            titulo={`Los ${principales.length} con más capital inmovilizado`}
            nota="Stock disponible actual × precio de referencia. Es el stock de hoy, no el promedio del período."
            datos={principales.map((f) => ({
              id: f.producto.id,
              nombre: f.producto.nombre,
              valor: f.capital,
              detalle: [
                { etiqueta: 'Categoría', valor: f.categoria.nombre },
                { etiqueta: 'Stock', valor: `${formatearCantidad(f.disponible)} ${f.producto.unidadMedida}` },
                { etiqueta: 'Salidas', valor: formatearCantidad(f.salidas) },
                { etiqueta: 'Rotación', valor: `${formatearCantidad(f.rotacion)}×` },
              ],
            }))}
            color={COLOR_CAPITAL}
            etiquetaValor="Capital inmovilizado"
            formatear={(v) => pesos.format(v)}
            vacio=""
          />

          {filas.length > CANTIDAD_EN_GRAFICO && (
            <div className="tarjeta grafico">
              <button type="button" className="boton boton--secundario" onClick={() => setVerTodos((v) => !v)}>
                {verTodos ? 'Ocultar el detalle' : `Ver los ${filas.length} productos`}
              </button>
              {verTodos && (
                <div className="tabla__contenedor grafico__tabla">
                  <table className="tabla">
                    <thead>
                      <tr>
                        <th>Producto</th>
                        <th>Categoría</th>
                        <th className="tabla__numero">Stock</th>
                        <th className="tabla__numero">Salidas</th>
                        <th className="tabla__numero">Rotación</th>
                        <th className="tabla__numero">Capital inmovilizado</th>
                      </tr>
                    </thead>
                    <tbody>
                      {filas.map((f) => (
                        <tr key={f.producto.id}>
                          <td>
                            <strong>{f.producto.nombre}</strong>
                            <div className="campo__ayuda">{f.producto.codigo}</div>
                          </td>
                          <td>{f.categoria.nombre}</td>
                          <td className="tabla__numero">
                            {formatearCantidad(f.disponible)} {f.producto.unidadMedida}
                          </td>
                          <td className="tabla__numero">{formatearCantidad(f.salidas)}</td>
                          <td className="tabla__numero">{formatearCantidad(f.rotacion)}×</td>
                          <td className="tabla__numero">{pesos.format(f.capital)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}
        </>
      )}
    </section>
  );
}
