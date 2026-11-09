import { useMemo, useState } from 'react';
import { Bar, BarChart, CartesianGrid, Cell, LabelList, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { formatearCantidad, formatearPrecio } from '../catalogo/productos/decimales.js';
import ParetoGrafico from './ParetoGrafico.jsx';
import { COLOR_CLASE, GRILLA, TINTA, TINTA_CLASE, TINTA_SUAVE } from './colores.js';

const CANTIDAD_EN_GRAFICO = 10;
const ALTO_BARRA = 34;
const MIN_ANCHO_ETIQUETA = 10; // % del ancho: por debajo de esto, el bloque no imprime su texto

const compacto = new Intl.NumberFormat('es-AR', {
  style: 'currency',
  currency: 'ARS',
  notation: 'compact',
  maximumFractionDigits: 1,
});

const recortar = (texto, max) => (texto.length > max ? `${texto.slice(0, max - 1)}…` : texto);
const porcentaje = (valor) => `${formatearCantidad(valor)}%`;

/** Nombre del producto en el eje: una sola línea, recortada (el nombre completo está en el tooltip y en la tabla). */
function EtiquetaProducto({ x, y, payload }) {
  return (
    <text x={x} y={y} dy={4} textAnchor="end" fill={TINTA} fontSize={12}>
      {payload.value}
    </text>
  );
}

function TooltipProducto({ active, payload }) {
  if (!active || !payload?.length) return null;
  const p = payload[0].payload;
  return (
    <div className="grafico__tooltip">
      <strong>{p.producto.nombre}</strong>
      <span className="grafico__tooltip-sub">
        {p.producto.codigo} · {p.categoria.nombre}
      </span>
      <dl>
        <dt>Clase</dt>
        <dd>
          <span className="clase-punto" style={{ background: COLOR_CLASE[p.clase] }} aria-hidden="true" />
          {p.clase}
        </dd>
        <dt>Valor de consumo</dt>
        <dd>{formatearPrecio(p.valorConsumo)}</dd>
        <dt>Participación</dt>
        <dd>{porcentaje(p.porcentaje)}</dd>
        <dt>Acumulado</dt>
        <dd>{porcentaje(p.porcentajeAcumulado)}</dd>
      </dl>
    </div>
  );
}

/**
 * Clasificación ABC (RF-21): cómo se reparte el valor de consumo entre las tres clases y qué productos
 * pesan más. Barra segmentada para el reparto, barras horizontales para el ranking y tabla como alternativa.
 */
export default function AbcSeccion({ abc }) {
  const [verTabla, setVerTabla] = useState(false);

  const ranking = useMemo(
    () => abc.productos.slice(0, CANTIDAD_EN_GRAFICO).map((p) => ({ ...p, etiqueta: recortar(p.producto.nombre, 30) })),
    [abc.productos]
  );

  const sinConsumo = abc.valorTotal <= 0;

  return (
    <section className="dashboard__seccion" aria-labelledby="titulo-abc">
      <div className="dashboard__titulo">
        <h2 id="titulo-abc">Clasificación ABC</h2>
        <span className="dashboard__nota">Valor de consumo = salidas del período × precio de referencia.</span>
      </div>

      <div className="tarjeta grafico">
        {sinConsumo ? (
          <p className="tabla__vacio">Todavía no hay salidas en el período para clasificar los productos.</p>
        ) : (
          <>
            <div
              className="segmentada"
              role="img"
              aria-label={`Reparto del valor de consumo: ${abc.resumen
                .map((r) => `clase ${r.clase} ${porcentaje(r.porcentajeValor)}`)
                .join(', ')}`}
            >
              {abc.resumen
                .filter((r) => r.porcentajeValor > 0)
                .map((r) => (
                  <div
                    key={r.clase}
                    className="segmentada__bloque"
                    style={{ flexGrow: r.porcentajeValor, background: COLOR_CLASE[r.clase], color: TINTA_CLASE[r.clase] }}
                    title={`Clase ${r.clase}: ${porcentaje(r.porcentajeValor)} del valor`}
                  >
                    {r.porcentajeValor >= MIN_ANCHO_ETIQUETA && (
                      <span>
                        {r.clase} · {porcentaje(r.porcentajeValor)}
                      </span>
                    )}
                  </div>
                ))}
            </div>

            <ul className="leyenda">
              {abc.resumen.map((r) => (
                <li key={r.clase}>
                  <span className="clase-punto" style={{ background: COLOR_CLASE[r.clase] }} aria-hidden="true" />
                  <strong>Clase {r.clase}</strong>
                  <span className="leyenda__detalle">
                    {r.productos} {r.productos === 1 ? 'producto' : 'productos'} · {formatearPrecio(r.valorConsumo)}
                  </span>
                </li>
              ))}
            </ul>

            <div className="grafico__encabezado">
              <h3>Los {ranking.length} productos de mayor valor de consumo</h3>
              <button type="button" className="boton boton--secundario" onClick={() => setVerTabla((v) => !v)}>
                {verTabla ? 'Ver gráfico' : 'Ver tabla completa'}
              </button>
            </div>

            {!verTabla && (
              <div
                role="img"
                aria-label={`Ranking de los ${ranking.length} productos de mayor valor de consumo. La tabla completa está disponible con el botón Ver tabla.`}
              >
                <ResponsiveContainer width="100%" height={ranking.length * ALTO_BARRA + 36}>
                  <BarChart data={ranking} layout="vertical" margin={{ top: 4, right: 76, bottom: 4, left: 8 }}>
                    <CartesianGrid horizontal={false} stroke={GRILLA} strokeWidth={1} />
                    <XAxis
                      type="number"
                      tickFormatter={(v) => compacto.format(v)}
                      axisLine={false}
                      tickLine={false}
                      tick={{ fill: TINTA_SUAVE, fontSize: 12 }}
                    />
                    <YAxis
                      type="category"
                      dataKey="etiqueta"
                      width={210}
                      axisLine={false}
                      tickLine={false}
                      tick={<EtiquetaProducto />}
                    />
                    <Tooltip content={<TooltipProducto />} cursor={{ fill: '#f4f6fa' }} />
                    <Bar dataKey="valorConsumo" barSize={18} radius={[0, 4, 4, 0]} isAnimationActive={false}>
                      {ranking.map((p) => (
                        <Cell key={p.producto.id} fill={COLOR_CLASE[p.clase]} />
                      ))}
                      <LabelList
                        dataKey="valorConsumo"
                        position="right"
                        formatter={(v) => compacto.format(v)}
                        style={{ fill: TINTA, fontSize: 12 }}
                      />
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </div>
            )}

            {verTabla && (
              <div className="tabla__contenedor grafico__tabla">
                <table className="tabla">
                  <thead>
                    <tr>
                      <th>#</th>
                      <th>Producto</th>
                      <th>Categoría</th>
                      <th>Clase</th>
                      <th className="tabla__numero">Valor de consumo</th>
                      <th className="tabla__numero">%</th>
                      <th className="tabla__numero">% acumulado</th>
                    </tr>
                  </thead>
                  <tbody>
                    {abc.productos.map((p, i) => (
                      <tr key={p.producto.id}>
                        <td>{i + 1}</td>
                        <td>
                          <strong>{p.producto.nombre}</strong>
                          <div className="campo__ayuda">{p.producto.codigo}</div>
                        </td>
                        <td>{p.categoria.nombre}</td>
                        <td>
                          <span className="clase-punto" style={{ background: COLOR_CLASE[p.clase] }} aria-hidden="true" />
                          {p.clase}
                        </td>
                        <td className="tabla__numero">{formatearPrecio(p.valorConsumo)}</td>
                        <td className="tabla__numero">{porcentaje(p.porcentaje)}</td>
                        <td className="tabla__numero">{porcentaje(p.porcentajeAcumulado)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            <ParetoGrafico productos={abc.productos} />
          </>
        )}
      </div>
    </section>
  );
}
