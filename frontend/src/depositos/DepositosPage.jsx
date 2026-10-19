import { useCallback, useEffect, useMemo, useState } from 'react';
import { Pencil, Plus, Power, PowerOff } from 'lucide-react';
import { mensajeDeError } from '../shared/api/cliente.js';
import ConfirmarDialogo from '../shared/ui/ConfirmarDialogo.jsx';
import FiltroActivo, { FILTRO_ACTIVO } from '../shared/ui/FiltroActivo.jsx';
import Insignia from '../shared/ui/Insignia.jsx';
import DepositoForm from './DepositoForm.jsx';
import { depositosApi } from './depositosApi.js';

/** ABM de depósitos (RF-07, RF-08). Solo ENCARGADO; la ruta ya está protegida por rol. */
export default function DepositosPage() {
  const [depositos, setDepositos] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const [aviso, setAviso] = useState('');

  const [filtro, setFiltro] = useState('activos');
  const [texto, setTexto] = useState('');

  const [formulario, setFormulario] = useState(null); // { deposito? } | null
  const [baja, setBaja] = useState(null); // { deposito, error, procesando } | null

  const cargar = useCallback(async () => {
    setCargando(true);
    setError('');
    try {
      setDepositos(await depositosApi.listar(FILTRO_ACTIVO[filtro]));
    } catch (err) {
      setError(mensajeDeError(err));
    } finally {
      setCargando(false);
    }
  }, [filtro]);

  useEffect(() => {
    cargar();
  }, [cargar]);

  const visibles = useMemo(() => {
    const buscado = texto.trim().toLowerCase();
    if (!buscado) return depositos;
    return depositos.filter(
      (d) => d.nombre.toLowerCase().includes(buscado) || (d.ubicacion ?? '').toLowerCase().includes(buscado)
    );
  }, [depositos, texto]);

  function alGuardar(guardado, accion) {
    setFormulario(null);
    setAviso(`Depósito "${guardado.nombre}" ${accion} correctamente.`);
    cargar();
  }

  async function activar(deposito) {
    setAviso('');
    setError('');
    try {
      await depositosApi.activar(deposito.id);
      setAviso(`Depósito "${deposito.nombre}" activado.`);
      cargar();
    } catch (err) {
      setError(mensajeDeError(err));
    }
  }

  async function confirmarBaja() {
    const { deposito } = baja;
    setBaja({ deposito, error: '', procesando: true });
    try {
      await depositosApi.desactivar(deposito.id);
      setBaja(null);
      setAviso(`Depósito "${deposito.nombre}" desactivado.`);
      cargar();
    } catch (err) {
      // 409: tiene stock o transferencias pendientes (RN-38)
      setBaja({ deposito, error: mensajeDeError(err), procesando: false });
    }
  }

  return (
    <div className="pagina">
      <header className="pagina__encabezado">
        <div>
          <h1>Depósitos</h1>
          <p className="pagina__bajada">Alta, edición y baja de los depósitos donde se guarda el stock.</p>
        </div>
        <button type="button" className="boton boton--primario" onClick={() => setFormulario({})}>
          <Plus size={18} aria-hidden="true" />
          Nuevo depósito
        </button>
      </header>

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
        <label className="filtro filtro--ancho">
          <span className="filtro__etiqueta">Buscar</span>
          <input
            type="search"
            placeholder="Nombre o ubicación"
            value={texto}
            onChange={(e) => setTexto(e.target.value)}
          />
        </label>
        <FiltroActivo valor={filtro} onCambio={setFiltro} />
      </div>

      <div className="tarjeta tabla__contenedor">
        <table className="tabla">
          <thead>
            <tr>
              <th>Nombre</th>
              <th>Ubicación</th>
              <th>Estado</th>
              <th aria-label="Acciones" />
            </tr>
          </thead>
          <tbody>
            {cargando && (
              <tr>
                <td colSpan={4} className="tabla__vacio">
                  Cargando…
                </td>
              </tr>
            )}
            {!cargando && visibles.length === 0 && (
              <tr>
                <td colSpan={4} className="tabla__vacio">
                  {depositos.length === 0 ? 'No hay depósitos para mostrar.' : 'Ningún depósito coincide con la búsqueda.'}
                </td>
              </tr>
            )}
            {!cargando &&
              visibles.map((d) => (
                <tr key={d.id} className={d.activo ? '' : 'fila--inactiva'}>
                  <td>
                    <strong>{d.nombre}</strong>
                  </td>
                  <td>{d.ubicacion || '—'}</td>
                  <td>
                    <Insignia activo={d.activo} />
                  </td>
                  <td className="tabla__acciones">
                    <button
                      type="button"
                      className="boton-icono"
                      title="Editar"
                      onClick={() => setFormulario({ deposito: d })}
                    >
                      <Pencil size={16} aria-hidden="true" />
                      <span className="solo-lectores">Editar {d.nombre}</span>
                    </button>
                    {d.activo ? (
                      <button
                        type="button"
                        className="boton-icono"
                        title="Desactivar"
                        onClick={() => setBaja({ deposito: d, error: '', procesando: false })}
                      >
                        <PowerOff size={16} aria-hidden="true" />
                        <span className="solo-lectores">Desactivar {d.nombre}</span>
                      </button>
                    ) : (
                      <button type="button" className="boton-icono" title="Activar" onClick={() => activar(d)}>
                        <Power size={16} aria-hidden="true" />
                        <span className="solo-lectores">Activar {d.nombre}</span>
                      </button>
                    )}
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>

      {formulario && (
        <DepositoForm deposito={formulario.deposito} onGuardado={alGuardar} onCancelar={() => setFormulario(null)} />
      )}

      {baja && (
        <ConfirmarDialogo
          titulo="Desactivar depósito"
          mensaje={`¿Querés desactivar el depósito "${baja.deposito.nombre}"? No se puede si tiene stock o transferencias pendientes.`}
          textoConfirmar="Desactivar"
          error={baja.error}
          procesando={baja.procesando}
          onConfirmar={confirmarBaja}
          onCancelar={() => setBaja(null)}
        />
      )}
    </div>
  );
}
