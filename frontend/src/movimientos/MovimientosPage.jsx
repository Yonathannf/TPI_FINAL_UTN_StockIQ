import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { formatearCantidad } from '../catalogo/productos/decimales.js';
import HistorialTab from './HistorialTab.jsx';
import MovimientoForm from './MovimientoForm.jsx';
import PendientesTab from './PendientesTab.jsx';
import { TIPOS } from './tipos.jsx';
import '../catalogo/catalogo.css';
import './movimientos.css';

const PESTANAS = [
  { id: 'historial', titulo: 'Historial' },
  { id: 'pendientes', titulo: 'Transferencias pendientes' },
];

function avisoDe(m) {
  const cantidad = `${formatearCantidad(m.cantidad)} ${m.producto.unidadMedida} de ${m.producto.nombre}`;
  if (m.tipo === 'ENTRADA') return `Entrada registrada: ${cantidad} en ${m.depositoDestino.nombre}.`;
  if (m.tipo === 'SALIDA') return `Salida registrada: ${cantidad} de ${m.depositoOrigen.nombre}.`;
  return `Transferencia registrada: ${cantidad} de ${m.depositoOrigen.nombre} a ${m.depositoDestino.nombre}. Queda pendiente hasta que se confirme la recepción.`;
}

/** Registro y consulta de movimientos (RF-09 a RF-15). Disponible para ENCARGADO y OPERARIO. */
export default function MovimientosPage() {
  const [params, setParams] = useSearchParams();
  const actual = PESTANAS.find((p) => p.id === params.get('seccion')) ?? PESTANAS[0];

  const [formulario, setFormulario] = useState(null); // 'ENTRADA' | 'SALIDA' | 'TRANSFERENCIA' | null
  const [aviso, setAviso] = useState('');
  const [version, setVersion] = useState(0); // cambia para que las pestañas recarguen

  function alRegistrar(movimiento) {
    setFormulario(null);
    setAviso(avisoDe(movimiento));
    setVersion((v) => v + 1);
  }

  return (
    <div className="pagina">
      <header className="pagina__encabezado">
        <div>
          <h1>Movimientos</h1>
          <p className="pagina__bajada">
            Entradas, salidas y transferencias entre depósitos. Los movimientos no se editan: un error se corrige con
            un movimiento nuevo.
          </p>
        </div>
        <div className="mov-acciones">
          {Object.entries(TIPOS).map(([clave, t]) => (
            <button
              key={clave}
              type="button"
              className={`boton ${clave === 'ENTRADA' ? 'boton--primario' : 'boton--secundario'}`}
              onClick={() => {
                setAviso('');
                setFormulario(clave);
              }}
            >
              <t.Icono size={18} aria-hidden="true" />
              {t.etiqueta}
            </button>
          ))}
        </div>
      </header>

      {aviso && (
        <p className="alerta alerta--exito" role="status">
          {aviso}
        </p>
      )}

      <div className="pestanas" role="tablist" aria-label="Secciones de movimientos">
        {PESTANAS.map((p) => (
          <button
            key={p.id}
            type="button"
            role="tab"
            id={`pestana-${p.id}`}
            aria-selected={p.id === actual.id}
            aria-controls="panel-movimientos"
            className={`pestana ${p.id === actual.id ? 'pestana--activa' : ''}`}
            onClick={() => setParams({ seccion: p.id }, { replace: true })}
          >
            {p.titulo}
          </button>
        ))}
      </div>

      <div id="panel-movimientos" role="tabpanel" aria-labelledby={`pestana-${actual.id}`}>
        {actual.id === 'historial' ? (
          <HistorialTab version={version} />
        ) : (
          <PendientesTab version={version} onCambio={() => setVersion((v) => v + 1)} />
        )}
      </div>

      {formulario && (
        <MovimientoForm tipo={formulario} onGuardado={alRegistrar} onCancelar={() => setFormulario(null)} />
      )}
    </div>
  );
}
