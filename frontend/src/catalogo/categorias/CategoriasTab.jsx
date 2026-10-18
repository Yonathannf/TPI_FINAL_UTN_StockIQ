import { Pencil, Plus, Power, PowerOff, Trash2 } from 'lucide-react';
import useAbm from '../../shared/hooks/useAbm.js';
import ConfirmarDialogo from '../../shared/ui/ConfirmarDialogo.jsx';
import FiltroActivo from '../../shared/ui/FiltroActivo.jsx';
import Insignia from '../../shared/ui/Insignia.jsx';
import CategoriaForm from './CategoriaForm.jsx';
import { categoriasApi } from './categoriasApi.js';

const camposBusqueda = (c) => [c.nombre, c.descripcion];

/** Listado y ABM de categorías (RF-04, RF-08). */
export default function CategoriasTab() {
  const abm = useAbm({ api: categoriasApi, camposBusqueda, femenino: true });
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
            placeholder="Nombre o descripción"
            value={abm.texto}
            onChange={(e) => abm.setTexto(e.target.value)}
          />
        </label>
        <FiltroActivo valor={abm.filtro} onCambio={abm.setFiltro} />
        <button type="button" className="boton boton--primario" onClick={() => abm.abrirFormulario()}>
          <Plus size={18} aria-hidden="true" />
          Nueva categoría
        </button>
      </div>

      <div className="tarjeta tabla__contenedor">
        <table className="tabla">
          <thead>
            <tr>
              <th>Nombre</th>
              <th>Descripción</th>
              <th>Estado</th>
              <th aria-label="Acciones" />
            </tr>
          </thead>
          <tbody>
            {abm.cargando && (
              <tr>
                <td colSpan={4} className="tabla__vacio">
                  Cargando…
                </td>
              </tr>
            )}
            {!abm.cargando && abm.visibles.length === 0 && (
              <tr>
                <td colSpan={4} className="tabla__vacio">
                  {abm.items.length === 0 ? 'No hay categorías para mostrar.' : 'Ninguna categoría coincide con la búsqueda.'}
                </td>
              </tr>
            )}
            {!abm.cargando &&
              abm.visibles.map((c) => (
                <tr key={c.id} className={c.activo ? '' : 'fila--inactiva'}>
                  <td>
                    <strong>{c.nombre}</strong>
                  </td>
                  <td>{c.descripcion || '—'}</td>
                  <td>
                    <Insignia activo={c.activo} />
                  </td>
                  <td className="tabla__acciones">
                    <button type="button" className="boton-icono" title="Editar" onClick={() => abm.abrirFormulario(c)}>
                      <Pencil size={16} aria-hidden="true" />
                      <span className="solo-lectores">Editar {c.nombre}</span>
                    </button>
                    {c.activo ? (
                      <button type="button" className="boton-icono" title="Desactivar" onClick={() => abm.pedirBaja(c)}>
                        <PowerOff size={16} aria-hidden="true" />
                        <span className="solo-lectores">Desactivar {c.nombre}</span>
                      </button>
                    ) : (
                      <button type="button" className="boton-icono" title="Activar" onClick={() => abm.activar(c)}>
                        <Power size={16} aria-hidden="true" />
                        <span className="solo-lectores">Activar {c.nombre}</span>
                      </button>
                    )}
                    <button type="button" className="boton-icono" title="Eliminar" onClick={() => abm.pedirEliminar(c)}>
                      <Trash2 size={16} aria-hidden="true" />
                      <span className="solo-lectores">Eliminar {c.nombre}</span>
                    </button>
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>

      {abm.formulario && (
        <CategoriaForm categoria={abm.formulario.item} onGuardado={abm.alGuardar} onCancelar={abm.cerrarFormulario} />
      )}

      {confirmacion && (
        <ConfirmarDialogo
          titulo={confirmacion.tipo === 'baja' ? 'Desactivar categoría' : 'Eliminar categoría'}
          mensaje={
            confirmacion.tipo === 'baja'
              ? `¿Querés desactivar la categoría "${confirmacion.item.nombre}"? Ya no se podrá asignar a productos nuevos.`
              : `¿Querés eliminar la categoría "${confirmacion.item.nombre}"? Solo se puede si no tiene productos asociados y no se puede deshacer.`
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
