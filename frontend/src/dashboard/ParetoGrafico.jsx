import { useEffect, useMemo, useState } from 'react';
import { CartesianGrid, Line, LineChart, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { formatearCantidad, formatearPrecio } from '../catalogo/productos/decimales.js';
import { parametrosApi } from '../configuracion/parametrosApi.js';
import { COLOR_CLASE, GRILLA, TINTA_SUAVE } from './colores.js';

const porcentaje = (valor) => `${formatearCantidad(valor)}%`;

function TooltipPareto({ active, payload }) {
  if (!active || !payload?.length) return null;
  const p = payload[0].payload;
  if (!p.producto) return null; // el punto de origen (0 productos, 0 %)
  return (
    <div className="grafico__tooltip">
      <strong>{p.producto.nombre}</strong>
      <span className="grafico__tooltip-sub">
        Puesto {p.puesto} · {p.categoria.nombre}
      </span>
      <dl>
        <dt>Clase</dt>
        <dd>
          <span className="clase-punto" style={{ background: COLOR_CLASE[p.clase] }} aria-hidden="true" />
          {p.clase}
        </dd>
        <dt>Valor de consumo</dt>
        <dd>{formatearPrecio(p.valorConsumo)}</dd>
        <dt>Acumulado</dt>
        <dd>{porcentaje(p.porcentajeAcumulado)}</dd>
      </dl>
    </div>
  );
}

/**
 * Curva de Pareto: qué parte del valor de consumo total concentran los primeros N productos (ordenados de mayor a
 * menor). Una sola escala (% acumulado) con los cortes A y B como líneas de referencia; cada punto toma el color de
 * su clase. El acumulado viene calculado por el backend (incluye al propio producto).
 */
export default function ParetoGrafico({ productos }) {
  const [cortes, setCortes] = useState({ a: 80, b: 95, leidos: false });

  useEffect(() => {
    let vigente = true;
    parametrosApi
      .listar()
      .then((lista) => {
        const valor = (clave) => Number(lista.find((p) => p.clave === clave)?.valor);
        const a = valor('corte_abc_a');
        const b = valor('corte_abc_b');
        if (vigente && Number.isFinite(a) && Number.isFinite(b)) setCortes({ a, b, leidos: true });
      })
      .catch(() => {}); // sin parámetros se usan los valores por defecto (80 y 95)
    return () => {
      vigente = false;
    };
  }, []);

  const datos = useMemo(
    () => [{ puesto: 0, porcentajeAcumulado: 0 }, ...productos.map((p, i) => ({ ...p, puesto: i + 1 }))],
    [productos]
  );

  const radio = productos.length > 60 ? 2.5 : 4;

  return (
    <>
      <div className="grafico__encabezado">
        <h3>Curva de Pareto: valor acumulado</h3>
      </div>
      <p className="campo__ayuda">
        Qué parte del valor de consumo total concentran los primeros productos. La clase se asigna según este acumulado
        (A hasta {cortes.a}%, B hasta {cortes.b}%, C el resto).
        {!cortes.leidos && ' No se pudieron leer los cortes configurados: se muestran los valores por defecto.'}
      </p>
      <div
        role="img"
        aria-label="Curva de Pareto del valor de consumo acumulado por producto. Los mismos datos están en la tabla completa del ranking."
      >
        <ResponsiveContainer width="100%" height={300}>
          <LineChart data={datos} margin={{ top: 12, right: 20, bottom: 24, left: 0 }}>
            <CartesianGrid vertical={false} stroke={GRILLA} strokeWidth={1} />
            <XAxis
              type="number"
              dataKey="puesto"
              domain={[0, productos.length]}
              allowDecimals={false}
              tickLine={false}
              axisLine={{ stroke: GRILLA }}
              tick={{ fill: TINTA_SUAVE, fontSize: 12 }}
              label={{
                value: 'Productos, de mayor a menor valor de consumo',
                position: 'insideBottom',
                offset: -14,
                fill: TINTA_SUAVE,
                fontSize: 12,
              }}
            />
            <YAxis
              domain={[0, 100]}
              ticks={[0, 25, 50, 75, 100]}
              tickFormatter={(v) => `${v}%`}
              axisLine={false}
              tickLine={false}
              width={48}
              tick={{ fill: TINTA_SUAVE, fontSize: 12 }}
            />
            <ReferenceLine
              y={cortes.a}
              stroke={TINTA_SUAVE}
              strokeDasharray="4 4"
              label={{ value: `Corte A · ${cortes.a}%`, position: 'insideTopLeft', fill: TINTA_SUAVE, fontSize: 12 }}
            />
            <ReferenceLine
              y={cortes.b}
              stroke={TINTA_SUAVE}
              strokeDasharray="4 4"
              label={{ value: `Corte B · ${cortes.b}%`, position: 'insideTopLeft', fill: TINTA_SUAVE, fontSize: 12 }}
            />
            <Tooltip content={<TooltipPareto />} cursor={{ stroke: TINTA_SUAVE, strokeWidth: 1 }} />
            <Line
              type="linear"
              dataKey="porcentajeAcumulado"
              stroke={TINTA_SUAVE}
              strokeWidth={2}
              isAnimationActive={false}
              dot={({ cx, cy, payload, key }) =>
                payload.clase ? (
                  <circle key={key} cx={cx} cy={cy} r={radio} fill={COLOR_CLASE[payload.clase]} stroke="#ffffff" strokeWidth={1.5} />
                ) : (
                  <g key={key} />
                )
              }
              activeDot={{ r: radio + 1, stroke: '#ffffff', strokeWidth: 2 }}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </>
  );
}
