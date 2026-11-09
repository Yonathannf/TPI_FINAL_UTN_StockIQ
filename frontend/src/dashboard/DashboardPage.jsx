import { useCallback, useEffect, useState } from 'react';
import { AlertTriangle, ArrowLeftRight, DollarSign, Medal, RefreshCw } from 'lucide-react';
import { mensajeDeError } from '../shared/api/cliente.js';
import { formatearCantidad } from '../catalogo/productos/decimales.js';
import AlertasSeccion from './AlertasSeccion.jsx';
import AbcSeccion from './AbcSeccion.jsx';
import BarrasHorizontales from './BarrasHorizontales.jsx';
import EvolucionSeccion from './EvolucionSeccion.jsx';
import { COLOR_DEPOSITO, COLOR_ROTACION } from './colores.js';
import { dashboardApi } from './dashboardApi.js';
import './dashboard.css';

/** "2026-11-02" → "02/11/2026" (los días de la API ya vienen en hora de Argentina). */
const formatoPesos = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS', maximumFractionDigits: 0 });

const formatoRotacion = new Intl.NumberFormat('es-AR', { minimumFractionDigits: 0, maximumFractionDigits: 2 });
const formatoEntero = new Intl.NumberFormat('es-AR');

/** Cuántos productos tienen stock en cada depósito (no se suman cantidades: las unidades de medida son distintas). */
function productosPorDeposito(stock) {
  const porId = new Map();
  for (const fila of stock) {
    for (const d of fila.porDeposito) {
      if (Number(d.cantidad) <= 0) continue;
      const actual = porId.get(d.deposito.id) ?? { deposito: d.deposito, productos: 0 };
      actual.productos += 1;
      porId.set(d.deposito.id, actual);
    }
  }
  return [...porId.values()].sort((a, b) => b.productos - a.productos);
}

const formatearDia = (dia) => (dia ? dia.split('-').reverse().join('/') : '—');

function Indicador({ icono: Icono, clase = '', valor, etiqueta }) {
  return (
    <div className="tarjeta indicador">
      <span className={`icono-cuadro ${clase}`} aria-hidden="true">
        <Icono size={20} strokeWidth={1.9} />
      </span>
      <div className="indicador__texto">
        <span className="indicador__valor">{valor}</span>
        <span className="indicador__etiqueta">{etiqueta}</span>
      </div>
    </div>
  );
}

/** Dashboard analítico (RF-23 a RF-25). Solo ENCARGADO; la ruta ya está protegida por rol. */
export default function DashboardPage() {
  const [analitica, setAnalitica] = useState(null);
  const [stock, setStock] = useState(null);
  const [abc, setAbc] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');

  const cargar = useCallback(async () => {
    setCargando(true);
    setError('');
    try {
      const [a, s, c] = await Promise.all([dashboardApi.analitica(), dashboardApi.stock(), dashboardApi.abc()]);
      setAnalitica(a);
      setStock(s);
      setAbc(c);
    } catch (err) {
      setError(mensajeDeError(err));
    } finally {
      setCargando(false);
    }
  }, []);

  useEffect(() => {
    cargar();
  }, [cargar]);

  const claseA = analitica?.resumenAbc.find((r) => r.clase === 'A');

  return (
    <div className="pagina">
      <header className="pagina__encabezado">
        <div>
          <h1>Dashboard</h1>
          <p className="pagina__bajada">
            {analitica
              ? `Período de análisis: del ${formatearDia(analitica.desde)} al ${formatearDia(analitica.hasta)}.`
              : 'Alertas de reposición, clasificación ABC, rotación y evolución de movimientos.'}
          </p>
        </div>
        <button type="button" className="boton boton--secundario" onClick={cargar} disabled={cargando}>
          <RefreshCw size={16} aria-hidden="true" />
          Actualizar
        </button>
      </header>

      {error && (
        <p className="alerta alerta--error" role="alert">
          {error}
        </p>
      )}

      {cargando && !analitica && <p className="tabla__vacio">Cargando…</p>}

      {analitica && stock && abc && (
        <>
          <div className="dashboard__indicadores">
            <Indicador
              icono={AlertTriangle}
              clase="icono-cuadro--alerta"
              valor={analitica.alertas.length}
              etiqueta="Productos en alerta de reposición"
            />
            <Indicador
              icono={DollarSign}
              clase="icono-cuadro--exito"
              valor={formatoPesos.format(analitica.valorConsumoTotal)}
              etiqueta="Valor de consumo del período"
            />
            <Indicador
              icono={Medal}
              valor={claseA?.productos ?? 0}
              etiqueta={`Productos clase A (${formatearCantidad(claseA?.porcentajeValor ?? 0)}% del valor)`}
            />
            <Indicador
              icono={ArrowLeftRight}
              clase="icono-cuadro--salida"
              valor={stock.pendientes.length}
              etiqueta="Transferencias pendientes"
            />
          </div>

          <AlertasSeccion alertas={analitica.alertas} />

          <EvolucionSeccion desde={analitica.desde} hasta={analitica.hasta} />

          <AbcSeccion abc={abc} />

          <div className="dashboard__duo">
            <BarrasHorizontales
              titulo="Rotación por categoría"
              nota="Veces que rota el stock en el período: salidas ÷ stock promedio."
              datos={[...analitica.rotacionCategorias]
                .sort((a, b) => b.rotacion - a.rotacion)
                .map((c) => ({
                  id: c.categoria.id,
                  nombre: c.categoria.nombre,
                  valor: c.rotacion,
                  detalle: [
                    { etiqueta: 'Productos', valor: formatoEntero.format(c.productos) },
                    { etiqueta: 'Salidas', valor: formatoRotacion.format(c.salidas) },
                    { etiqueta: 'Stock promedio', valor: formatoRotacion.format(c.stockPromedio) },
                  ],
                }))}
              color={COLOR_ROTACION}
              etiquetaValor="Rotación"
              formatear={(v) => `${formatoRotacion.format(v)}×`}
              vacio="No hay categorías con productos activos."
            />

            <BarrasHorizontales
              titulo="Productos con stock por depósito"
              nota="Cantidad de productos distintos con stock en cada depósito."
              datos={productosPorDeposito(stock.stock).map((d) => ({
                id: d.deposito.id,
                nombre: d.deposito.activo ? d.deposito.nombre : `${d.deposito.nombre} (inactivo)`,
                valor: d.productos,
                detalle: [],
              }))}
              color={COLOR_DEPOSITO}
              etiquetaValor="Productos"
              formatear={(v) => formatoEntero.format(v)}
              vacio="Todavía no hay stock en ningún depósito."
            />
          </div>
        </>
      )}
    </div>
  );
}
