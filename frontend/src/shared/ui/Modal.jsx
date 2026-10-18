import { useEffect, useId, useRef } from 'react';
import { X } from 'lucide-react';

/**
 * Ventana modal accesible: se cierra con Escape o clickeando el fondo,
 * enfoca el panel al abrirse y devuelve el foco al elemento anterior al cerrarse.
 */
export default function Modal({ titulo, onCerrar, children, pie }) {
  const idTitulo = useId();
  const panel = useRef(null);
  const alCerrar = useRef(onCerrar);
  alCerrar.current = onCerrar;

  useEffect(() => {
    const previo = document.activeElement;
    const alTecla = (e) => {
      if (e.key === 'Escape') alCerrar.current();
    };
    document.addEventListener('keydown', alTecla);
    document.body.style.overflow = 'hidden';
    panel.current?.focus();
    return () => {
      document.removeEventListener('keydown', alTecla);
      document.body.style.overflow = '';
      previo?.focus?.();
    };
  }, []);

  return (
    <div className="modal__fondo" onMouseDown={(e) => e.target === e.currentTarget && alCerrar.current()}>
      <div
        ref={panel}
        className="tarjeta modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby={idTitulo}
        tabIndex={-1}
      >
        <header className="modal__encabezado">
          <h2 id={idTitulo}>{titulo}</h2>
          <button type="button" className="boton-icono" onClick={onCerrar} title="Cerrar">
            <X size={18} aria-hidden="true" />
            <span className="solo-lectores">Cerrar</span>
          </button>
        </header>
        <div className="modal__cuerpo">{children}</div>
        {pie && <footer className="modal__pie">{pie}</footer>}
      </div>
    </div>
  );
}
