import { Pencil, Plus, Power, PowerOff } from 'lucide-react';
import { useAuth } from '../auth/AuthContext.jsx';
import useAbm from '../shared/hooks/useAbm.js';
import { NOMBRE_ROL } from '../shared/layout/navegacion.js';
import ConfirmarDialogo from '../shared/ui/ConfirmarDialogo.jsx';
import FiltroActivo from '../shared/ui/FiltroActivo.jsx';
import Insignia from '../shared/ui/Insignia.jsx';
import UsuarioForm from './UsuarioForm.jsx';
import { usuariosApi } from './usuariosApi.js';
import './usuarios.css';

const camposBusqueda = (u) => [u.username, u.nombre, u.apellido];
const etiqueta = (u) => u.username;

/** ABM de usuarios y roles (RF-03, RF-08). Solo ENCARGADO; la ruta ya está protegida por rol. */
export default function UsuariosPage() {
  const { usuario: sesion } = useAuth();
  const abm = useAbm({ api: usuariosApi, camposBusqueda, etiqueta });
  const { confirmacion, formulario } = abm;

  return (
    <div className="pagina">
      <header className="pagina__encabezado">
        <div>
          <h1>Usuarios</h1>
          <p className="pagina__bajada">Quién puede entrar al sistema y con qué rol.</p>
        </div>
        <button type="button" className="boton boton--primario" onClick={() => abm.abrirFormulario()}>
          <Plus size={18} aria-hidden="true" />
          Nuevo usuario
        </button>
      </header>

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
            placeholder="Usuario, nombre o apellido"
            value={abm.texto}
            onChange={(e) => abm.setTexto(e.target.value)}
          />
        </label>
        <FiltroActivo valor={abm.filtro} onCambio={abm.setFiltro} />
      </div>

      <div className="tarjeta tabla__contenedor">
        <table className="tabla">
          <thead>
            <tr>
              <th>Usuario</th>
              <th>Nombre</th>
              <th>Rol</th>
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
                  {abm.items.length === 0 ? 'No hay usuarios para mostrar.' : 'Ningún usuario coincide con la búsqueda.'}
                </td>
              </tr>
            )}
            {!abm.cargando &&
              abm.visibles.map((u) => {
                const esPropio = u.id === sesion.id;
                return (
                  <tr key={u.id} className={u.activo ? '' : 'fila--inactiva'}>
                    <td>
                      <strong>{u.username}</strong>
                      {esPropio && <span className="usuario__tu">vos</span>}
                    </td>
                    <td>
                      {u.nombre} {u.apellido}
                    </td>
                    <td>
                      <span className={`insignia insignia--rol-${u.rol.toLowerCase()}`}>{NOMBRE_ROL[u.rol] ?? u.rol}</span>
                    </td>
                    <td>
                      <Insignia activo={u.activo} />
                    </td>
                    <td className="tabla__acciones">
                      <button
                        type="button"
                        className="boton-icono"
                        title="Editar"
                        onClick={() => abm.abrirFormulario(u)}
                      >
                        <Pencil size={16} aria-hidden="true" />
                        <span className="solo-lectores">Editar {u.username}</span>
                      </button>
                      {u.activo ? (
                        <button
                          type="button"
                          className="boton-icono"
                          title={esPropio ? 'No podés desactivarte a vos mismo' : 'Desactivar'}
                          disabled={esPropio}
                          onClick={() => abm.pedirBaja(u)}
                        >
                          <PowerOff size={16} aria-hidden="true" />
                          <span className="solo-lectores">Desactivar {u.username}</span>
                        </button>
                      ) : (
                        <button type="button" className="boton-icono" title="Activar" onClick={() => abm.activar(u)}>
                          <Power size={16} aria-hidden="true" />
                          <span className="solo-lectores">Activar {u.username}</span>
                        </button>
                      )}
                    </td>
                  </tr>
                );
              })}
          </tbody>
        </table>
      </div>

      {formulario && (
        <UsuarioForm
          usuario={formulario.item}
          esPropio={formulario.item?.id === sesion.id}
          onGuardado={abm.alGuardar}
          onCancelar={abm.cerrarFormulario}
        />
      )}

      {confirmacion && (
        <ConfirmarDialogo
          titulo="Desactivar usuario"
          mensaje={`¿Querés desactivar al usuario "${confirmacion.item.username}"? Va a dejar de poder ingresar al sistema. No se puede desactivar al último encargado activo.`}
          textoConfirmar="Desactivar"
          error={confirmacion.error}
          procesando={confirmacion.procesando}
          onConfirmar={abm.confirmar}
          onCancelar={abm.cancelarConfirmacion}
        />
      )}
    </div>
  );
}
