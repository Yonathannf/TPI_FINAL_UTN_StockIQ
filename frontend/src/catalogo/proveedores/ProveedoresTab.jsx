import { Pencil, Plus, Power, PowerOff, Trash2 } from 'lucide-react';
import useAbm from '../../shared/hooks/useAbm.js';
import ConfirmarDialogo from '../../shared/ui/ConfirmarDialogo.jsx';
import FiltroActivo from '../../shared/ui/FiltroActivo.jsx';
import Insignia from '../../shared/ui/Insignia.jsx';
import ProveedorForm from './ProveedorForm.jsx';
import { proveedoresApi } from './proveedoresApi.js';

const camposBusqueda = (p) => [p.nombre, p.contacto, p.telefono, p.email];

const textoLeadTime = (dias) => (dias === 1 ? '1 día' : `${dias} días`);

/** Listado y ABM de proveedores con su lead time (RF-05, RF-08). */
export default function ProveedoresTab() {
  const abm = useAbm({ api: proveedoresApi, camposBusqueda });
  const { confirmacion } = abm;

  return (
    <>
      {abm.aviso && (
        <p className="alerta alerta--exito" role="status">
          {abm.aviso}
        </p>
      )}
      {abm.error && (
        <p className="alerta alerta--error" role="alert">
          {abm.error}
        </p>
      )}

      <div className="tarjeta herramientas">
        <label className="filtro filtro--ancho">
          <span className="filtro__etiqueta">Buscar</span>
          <input
            type="search"
            placeholder="Nombre, contacto, teléfono o email"
            value={abm.texto}
            onChange={(e) => abm.setTexto(e.target.value)}
          />
        </label>
        <FiltroActivo valor={abm.filtro} onCambio={abm.setFiltro} />
        <button type="button" className="boton boton--primario" onClick={() => abm.abrirFormulario()}>
          <Plus size={18} aria-hidden="true" />
          Nuevo proveedor
        </button>
      </div>

      <div className="tarjeta tabla__contenedor">
        <table className="tabla">
          <thead>
            <tr>
              <th>Nombre</th>
              <th>Contacto</th>
              <th>Lead time</th>
              <th>Estado</th>
              <th aria-label="Acciones" />
            </tr>
          </thead>
          <tbody>
            {abm.cargando && (
              <tr>
                <td colSpan={5} className="tabla__vacio">
                  Cargando…
                </td>
              </tr>
            )}
            {!abm.cargando && abm.visibles.length === 0 && (
              <tr>
                <td colSpan={5} className="tabla__vacio">
                  {abm.items.length === 0 ? 'No hay proveedores para mostrar.' : 'Ningún proveedor coincide con la búsqueda.'}
                </td>
              </tr>
            )}
            {!abm.cargando &&
              abm.visibles.map((p) => (
                <tr key={p.id} className={p.activo ? '' : 'fila--inactiva'}>
                  <td>
                    <strong>{p.nombre}</strong>
                  </td>
                  <td>
                    {p.contacto || p.telefono || p.email ? (
                      <>
                        {p.contacto && <div>{p.contacto}</div>}
                        {(p.telefono || p.email) && (
                          <div className="tabla__secundario">{[p.telefono, p.email].filter(Boolean).join(' · ')}</div>
                        )}
                      </>
                    ) : (
                      '—'
                    )}
                  </td>
                  <td>{textoLeadTime(p.leadTimeDias)}</td>
                  <td>
                    <Insignia activo={p.activo} />
                  </td>
                  <td className="tabla__acciones">
                    <button type="button" className="boton-icono" title="Editar" onClick={() => abm.abrirFormulario(p)}>
                      <Pencil size={16} aria-hidden="true" />
                      <span className="solo-lectores">Editar {p.nombre}</span>
                    </button>
                    {p.activo ? (
                      <button type="button" className="boton-icono" title="Desactivar" onClick={() => abm.pedirBaja(p)}>
                        <PowerOff size={16} aria-hidden="true" />
                        <span className="solo-lectores">Desactivar {p.nombre}</span>
                      </button>
                    ) : (
                      <button type="button" className="boton-icono" title="Activar" onClick={() => abm.activar(p)}>
                        <Power size={16} aria-hidden="true" />
                        <span className="solo-lectores">Activar {p.nombre}</span>
                      </button>
                    )}
                    <button type="button" className="boton-icono" title="Eliminar" onClick={() => abm.pedirEliminar(p)}>
                      <Trash2 size={16} aria-hidden="true" />
                      <span className="solo-lectores">Eliminar {p.nombre}</span>
                    </button>
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>

      {abm.formulario && (
        <ProveedorForm proveedor={abm.formulario.item} onGuardado={abm.alGuardar} onCancelar={abm.cerrarFormulario} />
      )}

      {confirmacion && (
        <ConfirmarDialogo
          titulo={confirmacion.tipo === 'baja' ? 'Desactivar proveedor' : 'Eliminar proveedor'}
          mensaje={
            confirmacion.tipo === 'baja'
              ? `¿Querés desactivar al proveedor "${confirmacion.item.nombre}"? Los productos activos pueden seguir teniéndolo como principal.`
              : `¿Querés eliminar al proveedor "${confirmacion.item.nombre}"? Solo se puede si no tiene productos asociados y no se puede deshacer.`
          }
          textoConfirmar={confirmacion.tipo === 'baja' ? 'Desactivar' : 'Eliminar'}
          error={confirmacion.error}
          procesando={confirmacion.procesando}
          onConfirmar={abm.confirmar}
          onCancelar={abm.cancelarConfirmacion}
        />
      )}
    </>
  );
}
