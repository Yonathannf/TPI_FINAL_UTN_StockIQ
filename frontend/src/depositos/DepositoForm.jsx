import { useState } from 'react';
import Modal from '../shared/ui/Modal.jsx';
import { erroresDeCampos, mensajeDeError } from '../shared/api/cliente.js';
import { depositosApi } from './depositosApi.js';

const MAX_NOMBRE = 100;
const MAX_UBICACION = 255;

/** Alta y edición de un depósito. Con `deposito` edita; sin él, crea. */
export default function DepositoForm({ deposito, onGuardado, onCancelar }) {
  const [nombre, setNombre] = useState(deposito?.nombre ?? '');
  const [ubicacion, setUbicacion] = useState(deposito?.ubicacion ?? '');
  const [errores, setErrores] = useState({});
  const [errorGeneral, setErrorGeneral] = useState('');
  const [enviando, setEnviando] = useState(false);

  function validar() {
    const nuevos = {};
    if (!nombre.trim()) nuevos.nombre = 'Es obligatorio.';
    else if (nombre.trim().length > MAX_NOMBRE) nuevos.nombre = `No puede superar los ${MAX_NOMBRE} caracteres.`;
    if (ubicacion.trim().length > MAX_UBICACION) {
      nuevos.ubicacion = `No puede superar los ${MAX_UBICACION} caracteres.`;
    }
    return nuevos;
  }

  async function guardar(evento) {
    evento.preventDefault();
    const nuevos = validar();
    setErrores(nuevos);
    setErrorGeneral('');
    if (Object.keys(nuevos).length > 0) return;

    const cuerpo = { nombre: nombre.trim(), ubicacion: ubicacion.trim() || null };
    setEnviando(true);
    try {
      const guardado = deposito
        ? await depositosApi.editar(deposito.id, cuerpo)
        : await depositosApi.crear(cuerpo);
      onGuardado(guardado, deposito ? 'editado' : 'creado');
    } catch (err) {
      const delServidor = erroresDeCampos(err);
      if (Object.keys(delServidor).length > 0) {
        setErrores(delServidor);
      } else {
        // Por ejemplo, 409 por nombre duplicado
        setErrorGeneral(mensajeDeError(err));
      }
      setEnviando(false);
    }
  }

  return (
    <Modal
      titulo={deposito ? 'Editar depósito' : 'Nuevo depósito'}
      onCerrar={onCancelar}
      pie={
        <>
          <button type="button" className="boton boton--secundario" onClick={onCancelar} disabled={enviando}>
            Cancelar
          </button>
          <button type="submit" form="form-deposito" className="boton boton--primario" disabled={enviando}>
            {enviando ? 'Guardando…' : 'Guardar'}
          </button>
        </>
      }
    >
      <form id="form-deposito" onSubmit={guardar} noValidate>
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
          <span className="campo__etiqueta">Ubicación (opcional)</span>
          <input
            type="text"
            value={ubicacion}
            onChange={(e) => setUbicacion(e.target.value)}
            maxLength={MAX_UBICACION}
            aria-invalid={Boolean(errores.ubicacion)}
          />
          {errores.ubicacion && <span className="campo__error">{errores.ubicacion}</span>}
        </label>
      </form>
    </Modal>
  );
}
