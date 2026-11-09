import { useState } from 'react';
import { Bar, BarChart, CartesianGrid, LabelList, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { GRILLA, TINTA, TINTA_SUAVE } from './colores.js';

const ALTO_BARRA = 38;
const MAX_ETIQUETA = 24;

const recortar = (texto, max) => (texto.length > max ? `${texto.slice(0, max - 1)}…` : texto);

function EtiquetaEje({ x, y, payload }) {
  return (
    <text x={x} y={y} dy={4} textAnchor="end" fill={TINTA} fontSize={12}>
      {payload.value}
    </text>
  );
}

function TooltipBarras({ active, payload, etiquetaValor, formatear }) {
  if (!active || !payload?.length) return null;
  const d = payload[0].payload;
  return (
    <div className="grafico__tooltip">
      <strong>{d.nombre}</strong>
      <dl>
        <dt>{etiquetaValor}</dt>
        <dd>{formatear(d.valor)}</dd>
        {d.detalle.map((x) => (
          <FilaDetalle key={x.etiqueta} {...x} />
        ))}
      </dl>
    </div>
  );
}

function FilaDetalle({ etiqueta, valor }) {
  return (
    <>
      <dt>{etiqueta}</dt>
      <dd>{valor}</dd>
    </>
  );
}

/**
 * Gráfico de barras horizontales de una sola serie, con tabla alternativa.
 * `datos`: [{ id, nombre, valor, detalle: [{ etiqueta, valor }] }], ya ordenados.
 */
export default function BarrasHorizontales({ titulo, nota, datos, color, etiquetaValor, formatear, vacio }) {
  const [verTabla, setVerTabla] = useState(false);
  const filas = datos.map((d) => ({ ...d, etiqueta: recortar(d.nombre, MAX_ETIQUETA) }));

  return (
    <section className="tarjeta grafico" aria-label={titulo}>
      <div className="grafico__encabezado grafico__encabezado--arriba">
        <div>
          <h3>{titulo}</h3>
          {nota && <span className="dashboard__nota">{nota}</span>}
        </div>
        {datos.length > 0 && (
          <button type="button" className="boton boton--secundario" onClick={() => setVerTabla((v) => !v)}>
            {verTabla ? 'Ver gráfico' : 'Ver tabla'}
          </button>
        )}
      </div>

      {datos.length === 0 && <p className="tabla__vacio">{vacio}</p>}

      {datos.length > 0 && !verTabla && (
        <div role="img" aria-label={`${titulo}. La tabla con los mismos datos está disponible con el botón Ver tabla.`}>
          <ResponsiveContainer width="100%" height={filas.length * ALTO_BARRA + 36}>
            <BarChart data={filas} layout="vertical" margin={{ top: 4, right: 56, bottom: 4, left: 8 }}>
              <CartesianGrid horizontal={false} stroke={GRILLA} strokeWidth={1} />
              <XAxis
                type="number"
                tickFormatter={formatear}
                axisLine={false}
                tickLine={false}
                tick={{ fill: TINTA_SUAVE, fontSize: 12 }}
                allowDecimals={false}
              />
              <YAxis
                type="category"
                dataKey="etiqueta"
                width={150}
                axisLine={false}
                tickLine={false}
                tick={<EtiquetaEje />}
              />
              <Tooltip
                content={<TooltipBarras etiquetaValor={etiquetaValor} formatear={formatear} />}
                cursor={{ fill: '#f4f6fa' }}
              />
              <Bar dataKey="valor" fill={color} barSize={18} radius={[0, 4, 4, 0]} isAnimationActive={false}>
                <LabelList dataKey="valor" position="right" formatter={formatear} style={{ fill: TINTA, fontSize: 12 }} />
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}

      {datos.length > 0 && verTabla && (
        <div className="tabla__contenedor grafico__tabla">
          <table className="tabla">
            <thead>
              <tr>
                <th>Nombre</th>
                <th className="tabla__numero">{etiquetaValor}</th>
                {datos[0].detalle.map((x) => (
                  <th key={x.etiqueta} className="tabla__numero">
                    {x.etiqueta}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {datos.map((d) => (
                <tr key={d.id}>
                  <td>
                    <strong>{d.nombre}</strong>
                  </td>
                  <td className="tabla__numero">{formatear(d.valor)}</td>
                  {d.detalle.map((x) => (
                    <td key={x.etiqueta} className="tabla__numero">
                      {x.valor}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
