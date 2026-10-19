import Modal from './Modal.jsx';

/**
 * Pide confirmación antes de una acción delicada (por ejemplo, una baja).
 * Si el backend la rechaza (409), el mensaje se muestra dentro del mismo diálogo.
 */
export default function ConfirmarDialogo({
  titulo,
  mensaje,
  textoConfirmar = 'Confirmar',
  error = '',
  procesando = false,
  onConfirmar,
  onCancelar,
}) {
  return (
    <Modal
      titulo={titulo}
      onCerrar={onCancelar}
      pie={
        <>
          <button type="button" className="boton boton--secundario" onClick={onCancelar} disabled={procesando}>
            Cancelar
          </button>
          <button type="button" className="boton boton--primario" onClick={onConfirmar} disabled={procesando}>
            {procesando ? 'Procesando…' : textoConfirmar}
          </button>
        </>
      }
    >
      <p className="modal__mensaje">{mensaje}</p>
      {error && (
        <p className="alerta alerta--error" role="alert">
          {error}
        </p>
      )}
    </Modal>
  );
}
