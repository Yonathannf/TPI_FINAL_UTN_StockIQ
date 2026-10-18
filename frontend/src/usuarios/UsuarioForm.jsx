import { useState } from 'react';
import Modal from '../shared/ui/Modal.jsx';
import { erroresDeCampos, mensajeDeError } from '../shared/api/cliente.js';
import { NOMBRE_ROL } from '../shared/layout/navegacion.js';
import { usuariosApi } from './usuariosApi.js';

const MAX_USERNAME = 60;
const MAX_NOMBRE = 100;
const MIN_PASSWORD = 8;
const MAX_PASSWORD = 72;

/**
 * Alta y edición de un usuario (RF-03).
 * - Alta: username, contraseña, nombre, apellido y rol.
 * - Edición: el username no se modifica; la contraseña es opcional (vacía = no se cambia).
 * - `esPropio`: el backend no permite que un usuario cambie su propio rol (RN-40).
 */
export default function UsuarioForm({ usuario, esPropio = false, onGuardado, onCancelar }) {
  const edicion = Boolean(usuario);
  const [datos, setDatos] = useState({
    username: usuario?.username ?? '',
    password: '',
    nombre: usuario?.nombre ?? '',
    apellido: usuario?.apellido ?? '',
    rol: usuario?.rol ?? 'OPERARIO',
  });
  const [errores, setErrores] = useState({});
  const [errorGeneral, setErrorGeneral] = useState('');
  const [enviando, setEnviando] = useState(false);

  const cambiar = (campo) => (e) => setDatos((d) => ({ ...d, [campo]: e.target.value }));

  function validar() {
    const nuevos = {};
    if (!edicion) {
      if (!datos.username.trim()) nuevos.username = 'Es obligatorio.';
      else if (datos.username.trim().length > MAX_USERNAME) {
        nuevos.username = `No puede superar los ${MAX_USERNAME} caracteres.`;
      }
    }
    if (!datos.nombre.trim()) nuevos.nombre = 'Es obligatorio.';
    else if (datos.nombre.trim().length > MAX_NOMBRE) nuevos.nombre = `No puede superar los ${MAX_NOMBRE} caracteres.`;
    if (!datos.apellido.trim()) nuevos.apellido = 'Es obligatorio.';
    else if (datos.apellido.trim().length > MAX_NOMBRE) {
      nuevos.apellido = `No puede superar los ${MAX_NOMBRE} caracteres.`;
    }
    const hayPassword = datos.password.length > 0;
    if (!edicion && !hayPassword) nuevos.password = 'Es obligatoria.';
    else if (hayPassword && (datos.password.length < MIN_PASSWORD || datos.password.length > MAX_PASSWORD)) {
      nuevos.password = `Debe tener entre ${MIN_PASSWORD} y ${MAX_PASSWORD} caracteres.`;
    }
    return nuevos;
  }

  async function guardar(evento) {
    evento.preventDefault();
    const nuevos = validar();
    setErrores(nuevos);
    setErrorGeneral('');
    if (Object.keys(nuevos).length > 0) return;

    setEnviando(true);
    try {
      let guardado;
      if (edicion) {
        const cuerpo = { nombre: datos.nombre.trim(), apellido: datos.apellido.trim(), rol: datos.rol };
        if (datos.password) cuerpo.password = datos.password;
        guardado = await usuariosApi.editar(usuario.id, cuerpo);
      } else {
        guardado = await usuariosApi.crear({
          username: datos.username.trim(),
          password: datos.password,
          nombre: datos.nombre.trim(),
          apellido: datos.apellido.trim(),
          rol: datos.rol,
        });
      }
      onGuardado(guardado, edicion ? 'editado' : 'creado');
    } catch (err) {
      const delServidor = erroresDeCampos(err);
      if (Object.keys(delServidor).length > 0) setErrores(delServidor);
      // 409: username duplicado, último ENCARGADO activo o cambio del propio rol (RN-33, RN-40)
      else setErrorGeneral(mensajeDeError(err));
      setEnviando(false);
    }
  }

  return (
    <Modal
      titulo={edicion ? 'Editar usuario' : 'Nuevo usuario'}
      onCerrar={onCancelar}
      pie={
        <>
          <button type="button" className="boton boton--secundario" onClick={onCancelar} disabled={enviando}>
            Cancelar
          </button>
          <button type="submit" form="form-usuario" className="boton boton--primario" disabled={enviando}>
            {enviando ? 'Guardando…' : 'Guardar'}
          </button>
        </>
      }
    >
      <form id="form-usuario" onSubmit={guardar} noValidate>
        {errorGeneral && (
          <p className="alerta alerta--error" role="alert">
            {errorGeneral}
          </p>
        )}

        <label className="campo">
          <span className="campo__etiqueta">Usuario</span>
          <input
            type="text"
            value={datos.username}
            onChange={cambiar('username')}
            maxLength={MAX_USERNAME}
            disabled={edicion}
            autoComplete="off"
            aria-invalid={Boolean(errores.username)}
            autoFocus={!edicion}
          />
          {edicion && <span className="campo__ayuda">El nombre de usuario no se puede modificar.</span>}
          {errores.username && <span className="campo__error">{errores.username}</span>}
        </label>

        <label className="campo">
          <span className="campo__etiqueta">Nombre</span>
          <input
            type="text"
            value={datos.nombre}
            onChange={cambiar('nombre')}
            maxLength={MAX_NOMBRE}
            aria-invalid={Boolean(errores.nombre)}
            autoFocus={edicion}
          />
          {errores.nombre && <span className="campo__error">{errores.nombre}</span>}
        </label>

        <label className="campo">
          <span className="campo__etiqueta">Apellido</span>
          <input
            type="text"
            value={datos.apellido}
            onChange={cambiar('apellido')}
            maxLength={MAX_NOMBRE}
            aria-invalid={Boolean(errores.apellido)}
          />
          {errores.apellido && <span className="campo__error">{errores.apellido}</span>}
        </label>

        <label className="campo">
          <span className="campo__etiqueta">Rol</span>
          <select value={datos.rol} onChange={cambiar('rol')} disabled={esPropio} aria-invalid={Boolean(errores.rol)}>
            {Object.entries(NOMBRE_ROL).map(([valor, titulo]) => (
              <option key={valor} value={valor}>
                {titulo}
              </option>
            ))}
          </select>
          {esPropio && <span className="campo__ayuda">No podés cambiar tu propio rol.</span>}
          {errores.rol && <span className="campo__error">{errores.rol}</span>}
        </label>

        <label className="campo">
          <span className="campo__etiqueta">{edicion ? 'Contraseña nueva (opcional)' : 'Contraseña'}</span>
          <input
            type="password"
            value={datos.password}
            onChange={cambiar('password')}
            maxLength={MAX_PASSWORD}
            autoComplete="new-password"
            aria-invalid={Boolean(errores.password)}
          />
          <span className="campo__ayuda">
            {edicion ? 'Dejala vacía para no cambiarla. ' : ''}Entre {MIN_PASSWORD} y {MAX_PASSWORD} caracteres.
          </span>
          {errores.password && <span className="campo__error">{errores.password}</span>}
        </label>
      </form>
    </Modal>
  );
}
