import { useEffect, useMemo, useState } from 'react';
import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { mensajeDeError } from '../shared/api/cliente.js';
import { COLOR_MOVIMIENTO, GRILLA, TINTA, TINTA_SUAVE } from './colores.js';
import { dashboardApi } from './dashboardApi.js';

const SERIES = [
  { clave: 'entradas', nombre: 'Entradas' },
  { clave: 'salidas', nombre: 'Salidas' },
  { clave: 'transferencias', nombre: 'Transferencias' },
];

const AGRUPACIONES = [
  { valor: 'DIA', texto: 'Día' },
  { valor: 'SEMANA', texto: 'Semana' },
  { valor: 'MES', texto: 'Mes' },
];

const aFecha = (dia) => new Date(`${dia}T00:00:00Z`);
const formatoMesCorto = new Intl.DateTimeFormat('es-AR', { month: 'short', year: '2-digit', timeZone: 'UTC' });
const formatoMesLargo = new Intl.DateTimeFormat('es-AR', { month: 'long', year: 'numeric', timeZone: 'UTC' });

/** "2026-11-02" → "02/11/2026". */
const diaCompleto = (dia) => dia.split('-').reverse().join('/');
/** "2026-11-02" → "02/11". */
const diaCorto = (dia) => dia.split('-').reverse().slice(0, 2).join('/');

/** Rótulo corto para el eje horizontal. */
function rotuloEje(periodo, agrupacion) {
  return agrupacion === 'MES' ? formatoMesCorto.format(aFecha(periodo)) : diaCorto(periodo);
}

/** Rótulo completo para el tooltip y la tabla. */
function rotuloCompleto(periodo, agrupacion) {
  if (agrupacion === 'MES') return formatoMesLargo.format(aFecha(periodo));
  if (agrupacion === 'SEMANA') return `Semana del ${diaCompleto(periodo)}`;
  return diaCompleto(periodo);
}

const formatoEntero = new Intl.NumberFormat('es-AR');

function TooltipEvolucion({ active, payload, agrupacion }) {
  if (!active || !payload?.length) return null;
  const punto = payload[0].payload;
  return (
    <div className="grafico__tooltip">
      <strong>{rotuloCompleto(punto.periodo, agrupacion)}</strong>
      <dl>
        {SERIES.map((s) => (
          <FilaSerie key={s.clave} serie={s} valor={punto[s.clave]} />
        ))}
      </dl>
    </div>
  );
}

function FilaSerie({ serie, valor }) {
  return (
    <>
      <dt>
        <span className="linea-clave" style={{ background: COLOR_MOVIMIENTO[serie.clave] }} aria-hidden="true" />
        {serie.nombre}
      </dt>
      <dd>{formatoEntero.format(valor)}</dd>
    </>
  );
}

/**
 * Evolución de entradas, salidas y transferencias en el tiempo (RF-25).
 * Cada punto cuenta movimientos registrados, no unidades: los productos tienen distintas unidades de medida.
 */
export default function EvolucionSeccion({ desde: desdeInicial, hasta: hastaInicial }) {
  const [agrupacion, setAgrupacion] = useState('SEMANA');
  const [desde, setDesde] = useState(desdeInicial);
  const [hasta, setHasta] = useState(hastaInicial);
  const [datos, setDatos] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const [verTabla, setVerTabla] = useState(false);

  useEffect(() => {
    let vigente = true;
    setCargando(true);
    setError('');
    dashboardApi
      .evolucion({ agrupacion, desde, hasta })
      .then((d) => vigente && setDatos(d))
      .catch((err) => {
        if (!vigente) return;
        // Por ejemplo, 400 RANGO_DEMASIADO_GRANDE al pedir días en un rango muy largo
        setError(mensajeDeError(err));
        setDatos(null);
      })
      .finally(() => vigente && setCargando(false));
    return () => {
      vigente = false;
    };
  }, [agrupacion, desde, hasta]);

  const puntos = datos?.puntos ?? [];
  const total = useMemo(
    () => puntos.reduce((suma, p) => suma + p.entradas + p.salidas + p.transferencias, 0),
    [puntos]
  );
  const ultimo = puntos.length - 1;

  return (
    <section className="dashboard__seccion" aria-labelledby="titulo-evolucion">
      <div className="dashboard__titulo">
        <h2 id="titulo-evolucion">Evolución de movimientos</h2>
        <span className="dashboard__nota">Cantidad de movimientos registrados, no de unidades.</span>
      </div>

      <div className="tarjeta grafico">
        <div className="herramientas herramientas--plana">
          <label className="filtro">
            <span className="filtro__etiqueta">Agrupar por</span>
            <select value={agrupacion} onChange={(e) => setAgrupacion(e.target.value)}>
              {AGRUPACIONES.map((a) => (
                <option key={a.valor} value={a.valor}>
                  {a.texto}
                </option>
              ))}
            </select>
          </label>
          <label className="filtro">
            <span className="filtro__etiqueta">Desde</span>
            <input type="date" value={desde} max={hasta} onChange={(e) => e.target.value && setDesde(e.target.value)} />
          </label>
          <label className="filtro">
            <span className="filtro__etiqueta">Hasta</span>
            <input type="date" value={hasta} min={desde} onChange={(e) => e.target.value && setHasta(e.target.value)} />
          </label>
          {puntos.length > 0 && (
            <button type="button" className="boton boton--secundario" onClick={() => setVerTabla((v) => !v)}>
              {verTabla ? 'Ver gráfico' : 'Ver tabla'}
            </button>
          )}
        </div>

        {error && (
          <p className="alerta alerta--error" role="alert">
            {error}
          </p>
        )}

        {cargando && !datos && !error && <p className="tabla__vacio">Cargando…</p>}

        {datos && total === 0 && <p className="tabla__vacio">No hubo movimientos en el período elegido.</p>}

        {datos && total > 0 && (
          <>
            <ul className="leyenda leyenda--arriba">
              {SERIES.map((s) => (
                <li key={s.clave}>
                  <span className="linea-clave" style={{ background: COLOR_MOVIMIENTO[s.clave] }} aria-hidden="true" />
                  <strong>{s.nombre}</strong>
                  <span className="leyenda__detalle">
                    {formatoEntero.format(puntos.reduce((suma, p) => suma + p[s.clave], 0))} en total
                  </span>
                </li>
              ))}
            </ul>

            {!verTabla && (
              <div
                role="img"
                aria-label="Evolución de entradas, salidas y transferencias. La tabla con los mismos datos está disponible con el botón Ver tabla."
                style={{ opacity: cargando ? 0.5 : 1 }}
              >
                <ResponsiveContainer width="100%" height={300}>
                  <LineChart data={puntos} margin={{ top: 12, right: 16, bottom: 4, left: 0 }}>
                    <CartesianGrid vertical={false} stroke={GRILLA} strokeWidth={1} />
                    <XAxis
                      dataKey="periodo"
                      tickFormatter={(p) => rotuloEje(p, agrupacion)}
                      axisLine={{ stroke: GRILLA }}
                      tickLine={false}
                      tick={{ fill: TINTA_SUAVE, fontSize: 12 }}
                      minTickGap={28}
                    />
                    <YAxis
                      allowDecimals={false}
                      axisLine={false}
                      tickLine={false}
                      width={40}
                      tick={{ fill: TINTA_SUAVE, fontSize: 12 }}
                    />
                    <Tooltip
                      content={<TooltipEvolucion agrupacion={agrupacion} />}
                      cursor={{ stroke: TINTA_SUAVE, strokeWidth: 1 }}
                    />
                    {SERIES.map((s) => (
                      <Line
                        key={s.clave}
                        type="linear"
                        dataKey={s.clave}
                        stroke={COLOR_MOVIMIENTO[s.clave]}
                        strokeWidth={2}
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        isAnimationActive={false}
                        // Punto final con aro del color de la superficie, para que se lea donde las líneas se cruzan
                        dot={({ cx, cy, index, key }) =>
                          index === ultimo ? (
                            <circle
                              key={key}
                              cx={cx}
                              cy={cy}
                              r={4}
                              fill={COLOR_MOVIMIENTO[s.clave]}
                              stroke="#ffffff"
                              strokeWidth={2}
                            />
                          ) : (
                            <g key={key} />
                          )
                        }
                        activeDot={{ r: 4, stroke: '#ffffff', strokeWidth: 2, fill: COLOR_MOVIMIENTO[s.clave] }}
                      />
                    ))}
                  </LineChart>
                </ResponsiveContainer>
              </div>
            )}

            {verTabla && (
              <div className="tabla__contenedor grafico__tabla">
                <table className="tabla">
                  <thead>
                    <tr>
                      <th>Período</th>
                      {SERIES.map((s) => (
                        <th key={s.clave} className="tabla__numero">
                          {s.nombre}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {puntos.map((p) => (
                      <tr key={p.periodo}>
                        <td>{rotuloCompleto(p.periodo, agrupacion)}</td>
                        {SERIES.map((s) => (
                          <td key={s.clave} className="tabla__numero">
                            {formatoEntero.format(p[s.clave])}
                          </td>
                        ))}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </>
        )}
      </div>
    </section>
  );
}
