import { useState } from 'react';
import Modal from '../../shared/ui/Modal.jsx';
import { erroresDeCampos, mensajeDeError } from '../../shared/api/cliente.js';
import { proveedoresApi } from './proveedoresApi.js';

const LIMITES = { nombre: 150, contacto: 150, telefono: 50, email: 150 };
const FORMATO_EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/** Alta y edición de un proveedor, con su lead time en días (RF-05). */
export default function ProveedorForm({ proveedor, onGuardado, onCancelar }) {
  const [datos, setDatos] = useState({
    nombre: proveedor?.nombre ?? '',
    contacto: proveedor?.contacto ?? '',
    telefono: proveedor?.telefono ?? '',
    email: proveedor?.email ?? '',
    leadTimeDias: proveedor?.leadTimeDias?.toString() ?? '0',
  });
  const [errores, setErrores] = useState({});
  const [errorGeneral, setErrorGeneral] = useState('');
  const [enviando, setEnviando] = useState(false);

  const cambiar = (campo) => (e) => setDatos((d) => ({ ...d, [campo]: e.target.value }));

  function validar() {
    const nuevos = {};
    if (!datos.nombre.trim()) nuevos.nombre = 'Es obligatorio.';
    for (const campo of Object.keys(LIMITES)) {
      if (datos[campo].trim().length > LIMITES[campo]) {
        nuevos[campo] = `No puede superar los ${LIMITES[campo]} caracteres.`;
      }
    }
    if (datos.email.trim() && !FORMATO_EMAIL.test(datos.email.trim())) {
      nuevos.email = 'El email no tiene un formato válido.';
    }
    if (!/^\d+$/.test(datos.leadTimeDias.trim())) {
      nuevos.leadTimeDias = 'Ingresá un número entero de días, 0 o más.';
    }
    return nuevos;
  }

  async function guardar(evento) {
    evento.preventDefault();
    const nuevos = validar();
    setErrores(nuevos);
    setErrorGeneral('');
    if (Object.keys(nuevos).length > 0) return;

    const cuerpo = {
      nombre: datos.nombre.trim(),
      contacto: datos.contacto.trim() || null,
      telefono: datos.telefono.trim() || null,
      email: datos.email.trim() || null,
      leadTimeDias: Number(datos.leadTimeDias.trim()),
    };
    setEnviando(true);
    try {
      const guardado = proveedor
        ? await proveedoresApi.editar(proveedor.id, cuerpo)
        : await proveedoresApi.crear(cuerpo);
      onGuardado(guardado, proveedor ? 'editado' : 'creado');
    } catch (err) {
      const delServidor = erroresDeCampos(err);
      if (Object.keys(delServidor).length > 0) setErrores(delServidor);
      else setErrorGeneral(mensajeDeError(err)); // por ejemplo, 409 por nombre duplicado
      setEnviando(false);
    }
  }

  const campoTexto = (campo, etiqueta, extra = {}) => (
    <label className="campo">
      <span className="campo__etiqueta">{etiqueta}</span>
      <input
        type="text"
        value={datos[campo]}
        onChange={cambiar(campo)}
        maxLength={LIMITES[campo]}
        aria-invalid={Boolean(errores[campo])}
        {...extra}
      />
      {errores[campo] && <span className="campo__error">{errores[campo]}</span>}
    </label>
  );

  return (
    <Modal
      titulo={proveedor ? 'Editar proveedor' : 'Nuevo proveedor'}
      onCerrar={onCancelar}
      pie={
        <>
          <button type="button" className="boton boton--secundario" onClick={onCancelar} disabled={enviando}>
            Cancelar
          </button>
          <button type="submit" form="form-proveedor" className="boton boton--primario" disabled={enviando}>
            {enviando ? 'Guardando…' : 'Guardar'}
          </button>
        </>
      }
    >
      <form id="form-proveedor" onSubmit={guardar} noValidate>
        {errorGeneral && (
          <p className="alerta alerta--error" role="alert">
            {errorGeneral}
          </p>
        )}

        {campoTexto('nombre', 'Nombre', { autoFocus: true })}
        {campoTexto('contacto', 'Contacto (opcional)')}
        {campoTexto('telefono', 'Teléfono (opcional)', { type: 'tel' })}
        {campoTexto('email', 'Email (opcional)', { type: 'email' })}

        <label className="campo">
          <span className="campo__etiqueta">Lead time (días)</span>
          <input
            type="number"
            min="0"
            step="1"
            inputMode="numeric"
            value={datos.leadTimeDias}
            onChange={cambiar('leadTimeDias')}
            aria-invalid={Boolean(errores.leadTimeDias)}
          />
          <span className="campo__ayuda">Días que tarda el proveedor en entregar. Se usa para el punto de reposición.</span>
          {errores.leadTimeDias && <span className="campo__error">{errores.leadTimeDias}</span>}
        </label>
      </form>
    </Modal>
  );
}
