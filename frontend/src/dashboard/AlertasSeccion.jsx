import { Fragment, useEffect, useState } from 'react';
import { ChevronDown, ChevronRight } from 'lucide-react';
import { formatearCantidad } from '../catalogo/productos/decimales.js';
import { parametrosApi } from '../configuracion/parametrosApi.js';

const n = (valor) => Number(valor ?? 0);

/** Cuenta paso a paso de una alerta (RN-20, RN-23 y RN-24) con los datos que ya trae /analitica/reposicion. */
function ComoSeCalculo({ alerta: a, diasCobertura }) {
  const u = a.producto.unidadMedida;
  const consumo = n(a.consumoDiario);
  const lead = n(a.proveedor.leadTimeDias);
  const seguridad = n(a.stockSeguridad);
  const disponible = n(a.stockDisponible);
  const transito = n(a.enTransito);
  const pr = n(a.puntoReposicion);
  const cant = formatearCantidad;

  // Cantidad sugerida (RN-24): necesita dias_cobertura, que sale de /parametros.
  const cobertura = diasCobertura != null ? consumo * diasCobertura : null;
  const bruto = cobertura != null ? pr + cobertura - disponible - transito : null;

  return (
    <div className="calculo">
      <div className="calculo__paso">
        <h3>1. Punto de reposición</h3>
        <p className="calculo__cuenta">
          {cant(consumo)} {u}/día × {lead} días de lead time + {cant(seguridad)} de stock de seguridad ={' '}
          <strong>
            {cant(pr)} {u}
          </strong>
        </p>
        <p className="campo__ayuda">Es el stock con el que conviene pedir: cubre el consumo mientras llega el pedido.</p>
      </div>

      <div className="calculo__paso">
        <h3>2. Por qué está en alerta</h3>
        <p className="calculo__cuenta">
          Disponible {cant(disponible)} {u} ≤ punto de reposición {cant(pr)} {u}
        </p>
        <p className="campo__ayuda">
          {transito > 0
            ? `Hay ${cant(transito)} ${u} en tránsito; se descuentan de lo sugerido.`
            : 'No hay mercadería en tránsito para este producto.'}
        </p>
      </div>

      <div className="calculo__paso">
        <h3>3. Cantidad sugerida</h3>
        {bruto == null ? (
          <p className="campo__ayuda">No se pudieron leer los parámetros para mostrar la cuenta.</p>
        ) : (
          <>
            <p className="calculo__cuenta">
              {cant(pr)} + ({cant(consumo)} × {diasCobertura} días de cobertura) − {cant(disponible)} disponible −{' '}
              {cant(transito)} en tránsito = {cant(bruto)}
            </p>
            <p className="calculo__cuenta">
              Se redondea hacia arriba y nunca es menor a 0:{' '}
              <strong>
                {cant(a.cantidadSugerida)} {u}
              </strong>
            </p>
          </>
        )}
      </div>
    </div>
  );
}

/** Tabla de alertas de reposición; cada fila se despliega con el detalle de cómo se calculó. */
export default function AlertasSeccion({ alertas }) {
  const [abiertas, setAbiertas] = useState(() => new Set());
  const [diasCobertura, setDiasCobertura] = useState(null);

  useEffect(() => {
    let vigente = true;
    parametrosApi
      .listar()
      .then((lista) => {
        const p = lista.find((x) => x.clave === 'dias_cobertura');
        if (vigente && p) setDiasCobertura(Number(p.valor));
      })
      .catch(() => {}); // sin parámetros el panel igual muestra el punto de reposición
    return () => {
      vigente = false;
    };
  }, []);

  const alternar = (id) =>
    setAbiertas((previas) => {
      const nuevas = new Set(previas);
      if (nuevas.has(id)) nuevas.delete(id);
      else nuevas.add(id);
      return nuevas;
    });

  return (
    <section className="dashboard__seccion">
      <div className="dashboard__titulo">
        <h2>Alertas de reposición</h2>
        <span className="dashboard__nota">
          Stock disponible igual o menor al punto de reposición. Abrí una fila para ver cómo se calculó.
        </span>
      </div>
      <div className="tarjeta tabla__contenedor tabla__contenedor--alertas">
        <table className="tabla">
          <thead>
            <tr>
              <th aria-label="Detalle" />
              <th>Producto</th>
              <th>Proveedor</th>
              <th className="tabla__numero">Consumo diario</th>
              <th className="tabla__numero">Disponible</th>
              <th className="tabla__numero">En tránsito</th>
              <th className="tabla__numero">Punto de reposición</th>
              <th className="tabla__numero">Sugerido</th>
            </tr>
          </thead>
          <tbody>
            {alertas.length === 0 && (
              <tr>
                <td colSpan={8} className="tabla__vacio">
                  No hay productos en alerta: ningún producto con consumo está en o por debajo de su punto de
                  reposición.
                </td>
              </tr>
            )}
            {alertas.map((a) => {
              const abierta = abiertas.has(a.producto.id);
              const Chevron = abierta ? ChevronDown : ChevronRight;
              return (
                <Fragment key={a.producto.id}>
                  <tr className="fila--alerta">
                    <td className="celda--chevron">
                      <button
                        type="button"
                        className="boton-icono"
                        aria-expanded={abierta}
                        aria-label={`${abierta ? 'Ocultar' : 'Ver'} cómo se calculó la alerta de ${a.producto.nombre}`}
                        onClick={() => alternar(a.producto.id)}
                      >
                        <Chevron size={16} aria-hidden="true" />
                      </button>
                    </td>
                    <td>
                      <strong>{a.producto.nombre}</strong>
                      <div className="campo__ayuda">{a.producto.codigo}</div>
                    </td>
                    <td>
                      {a.proveedor.nombre}
                      <div className="campo__ayuda">Lead time: {a.proveedor.leadTimeDias} días</div>
                    </td>
                    <td className="tabla__numero">{formatearCantidad(a.consumoDiario)}</td>
                    <td className="tabla__numero">
                      {formatearCantidad(a.stockDisponible)} {a.producto.unidadMedida}
                    </td>
                    <td className="tabla__numero">{a.enTransito > 0 ? formatearCantidad(a.enTransito) : '—'}</td>
                    <td className="tabla__numero">{formatearCantidad(a.puntoReposicion)}</td>
                    <td className="tabla__numero dashboard__sugerida">
                      {formatearCantidad(a.cantidadSugerida)} {a.producto.unidadMedida}
                    </td>
                  </tr>
                  {abierta && (
                    <tr className="fila--detalle">
                      <td colSpan={8}>
                        <ComoSeCalculo alerta={a} diasCobertura={diasCobertura} />
                      </td>
                    </tr>
                  )}
                </Fragment>
              );
            })}
          </tbody>
        </table>
      </div>
    </section>
  );
}
