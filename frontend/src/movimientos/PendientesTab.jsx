import { useCallback, useEffect, useState } from 'react';
import { Check, X } from 'lucide-react';
import { mensajeDeError } from '../shared/api/cliente.js';
import { formatearFechaHora } from '../shared/format/fechas.js';
import ConfirmarDialogo from '../shared/ui/ConfirmarDialogo.jsx';
import { formatearCantidad } from '../catalogo/productos/decimales.js';
import { depositosApi } from '../depositos/depositosApi.js';
import { movimientosApi } from './movimientosApi.js';
import { Recorrido, nombreCompleto } from './tipos.jsx';

/**
 * Transferencias pendientes con sus acciones de confirmar y cancelar (RF-12, RN-10 a RN-14).
 * Se usa en Movimientos y en Stock (RF-24). `version` cambia cuando hay que recargar;
 * `onCambio` avisa que se confirmó o canceló algo.
 */
export default function PendientesTab({ version = 0, onCambio }) {
  const [pendientes, setPendientes] = useState([]);
  const [depositos, setDepositos] = useState([]);
  const [destinoId, setDestinoId] = useState('');
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const [aviso, setAviso] = useState('');
  const [accion, setAccion] = useState(null); // { tipo: 'confirmar' | 'cancelar', movimiento, error, procesando }

  useEffect(() => {
    depositosApi.listar(true).then(setDepositos).catch(() => setDepositos([]));
  }, []);

  const cargar = useCallback(async () => {
    setCargando(true);
    setError('');
    try {
      setPendientes(await movimientosApi.pendientes({ depositoDestinoId: destinoId }));
    } catch (err) {
      setError(mensajeDeError(err));
    } finally {
      setCargando(false);
    }
  }, [destinoId]);

  useEffect(() => {
    cargar();
  }, [cargar, version]);

  async function ejecutar() {
    const { tipo, movimiento } = accion;
    setAccion({ ...accion, error: '', procesando: true });
    try {
      if (tipo === 'confirmar') await movimientosApi.confirmar(movimiento.id);
      else await movimientosApi.cancelar(movimiento.id);
      setAccion(null);
      setAviso(
        tipo === 'confirmar'
          ? `Recepción confirmada en ${movimiento.depositoDestino.nombre}.`
          : `Transferencia cancelada: la mercadería volvió a ${movimiento.depositoOrigen.nombre}.`
      );
      cargar();
      onCambio?.();
    } catch (err) {
      // 409: otra persona ya la confirmó o canceló (RN-13)
      setAccion({ ...accion, error: mensajeDeError(err), procesando: false });
    }
  }

  const descripcion = (m) =>
    `${formatearCantidad(m.cantidad)} ${m.producto.unidadMedida} de ${m.producto.nombre}`;

  return (
    <>
      {aviso && (
        <p className="alerta alerta--exito" role="status">
          {aviso}
        </p>
      )}
      {error && (
        <p className="alerta alerta--error" role="alert">
          {error}
        </p>
      )}

      <div className="tarjeta herramientas">
        <label className="filtro">
          <span className="filtro__etiqueta">Depósito de destino</span>
          <select value={destinoId} onChange={(e) => setDestinoId(e.target.value)}>
            <option value="">Todos</option>
            {depositos.map((d) => (
              <option key={d.id} value={d.id}>
                {d.nombre}
              </option>
            ))}
          </select>
        </label>
      </div>

      <div className="tarjeta tabla__contenedor">
        <table className="tabla">
          <thead>
            <tr>
              <th>Registrada</th>
              <th>Producto</th>
              <th className="tabla__numero">Cantidad</th>
              <th>Origen → destino</th>
              <th>Registró</th>
              <th aria-label="Acciones" />
            </tr>
          </thead>
          <tbody>
            {cargando && (
              <tr>
                <td colSpan={6} className="tabla__vacio">
                  Cargando…
                </td>
              </tr>
            )}
            {!cargando && pendientes.length === 0 && (
              <tr>
                <td colSpan={6} className="tabla__vacio">
                  No hay transferencias pendientes.
                </td>
              </tr>
            )}
            {!cargando &&
              pendientes.map((m) => (
                <tr key={m.id}>
                  <td className="mov-fecha">{formatearFechaHora(m.fecha)}</td>
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
                  <td>{nombreCompleto(m.usuario)}</td>
                  <td className="tabla__acciones">
                    <button
                      type="button"
                      className="boton-icono"
                      title="Confirmar recepción"
                      onClick={() => setAccion({ tipo: 'confirmar', movimiento: m, error: '', procesando: false })}
                    >
                      <Check size={16} aria-hidden="true" />
                      <span className="solo-lectores">Confirmar recepción de {m.producto.nombre}</span>
                    </button>
                    <button
                      type="button"
                      className="boton-icono"
                      title="Cancelar transferencia"
                      onClick={() => setAccion({ tipo: 'cancelar', movimiento: m, error: '', procesando: false })}
                    >
                      <X size={16} aria-hidden="true" />
                      <span className="solo-lectores">Cancelar transferencia de {m.producto.nombre}</span>
                    </button>
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>

      {accion && (
        <ConfirmarDialogo
          titulo={accion.tipo === 'confirmar' ? 'Confirmar recepción' : 'Cancelar transferencia'}
          mensaje={
            accion.tipo === 'confirmar'
              ? `¿Confirmás que llegaron ${descripcion(accion.movimiento)} a ${accion.movimiento.depositoDestino.nombre}? El stock se suma a ese depósito.`
              : `¿Cancelás la transferencia de ${descripcion(accion.movimiento)}? La mercadería vuelve a ${accion.movimiento.depositoOrigen.nombre}.`
          }
          textoConfirmar={accion.tipo === 'confirmar' ? 'Confirmar recepción' : 'Cancelar transferencia'}
          error={accion.error}
          procesando={accion.procesando}
          onConfirmar={ejecutar}
          onCancelar={() => setAccion(null)}
        />
      )}
    </>
  );
}
