import { useState } from 'react';
import Modal from '../../shared/ui/Modal.jsx';
import { erroresDeCampos, mensajeDeError } from '../../shared/api/cliente.js';
import { categoriasApi } from './categoriasApi.js';

const MAX_NOMBRE = 100;
const MAX_DESCRIPCION = 255;

/** Alta y edición de una categoría. Con `categoria` edita; sin ella, crea. */
export default function CategoriaForm({ categoria, onGuardado, onCancelar }) {
  const [nombre, setNombre] = useState(categoria?.nombre ?? '');
  const [descripcion, setDescripcion] = useState(categoria?.descripcion ?? '');
  const [errores, setErrores] = useState({});
  const [errorGeneral, setErrorGeneral] = useState('');
  const [enviando, setEnviando] = useState(false);

  function validar() {
    const nuevos = {};
    if (!nombre.trim()) nuevos.nombre = 'Es obligatorio.';
    else if (nombre.trim().length > MAX_NOMBRE) nuevos.nombre = `No puede superar los ${MAX_NOMBRE} caracteres.`;
    if (descripcion.trim().length > MAX_DESCRIPCION) {
      nuevos.descripcion = `No puede superar los ${MAX_DESCRIPCION} caracteres.`;
    }
    return nuevos;
  }

  async function guardar(evento) {
    evento.preventDefault();
    const nuevos = validar();
    setErrores(nuevos);
    setErrorGeneral('');
    if (Object.keys(nuevos).length > 0) return;

    const cuerpo = { nombre: nombre.trim(), descripcion: descripcion.trim() || null };
    setEnviando(true);
    try {
      const guardada = categoria
        ? await categoriasApi.editar(categoria.id, cuerpo)
        : await categoriasApi.crear(cuerpo);
      onGuardado(guardada, categoria ? 'editada' : 'creada');
    } catch (err) {
      const delServidor = erroresDeCampos(err);
      if (Object.keys(delServidor).length > 0) setErrores(delServidor);
      else setErrorGeneral(mensajeDeError(err)); // por ejemplo, 409 por nombre duplicado
      setEnviando(false);
    }
  }

  return (
    <Modal
      titulo={categoria ? 'Editar categoría' : 'Nueva categoría'}
      onCerrar={onCancelar}
      pie={
        <>
          <button type="button" className="boton boton--secundario" onClick={onCancelar} disabled={enviando}>
            Cancelar
          </button>
          <button type="submit" form="form-categoria" className="boton boton--primario" disabled={enviando}>
            {enviando ? 'Guardando…' : 'Guardar'}
          </button>
        </>
      }
    >
      <form id="form-categoria" onSubmit={guardar} noValidate>
        {errorGeneral && (
          <p className="alerta alerta--error" role="alert">
            {errorGeneral}
          </p>
        )}

        <label className="campo">
          <span className="campo__etiqueta">Nombre</span>
          <input
            type="text"
            value={nombre}
            onChange={(e) => setNombre(e.target.value)}
            maxLength={MAX_NOMBRE}
            aria-invalid={Boolean(errores.nombre)}
            autoFocus
          />
          {errores.nombre && <span className="campo__error">{errores.nombre}</span>}
        </label>

        <label className="campo">
          <span className="campo__etiqueta">Descripción (opcional)</span>
          <input
            type="text"
            value={descripcion}
            onChange={(e) => setDescripcion(e.target.value)}
            maxLength={MAX_DESCRIPCION}
            aria-invalid={Boolean(errores.descripcion)}
          />
          {errores.descripcion && <span className="campo__error">{errores.descripcion}</span>}
        </label>
      </form>
    </Modal>
  );
}
