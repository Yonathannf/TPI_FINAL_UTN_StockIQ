/** Etiqueta de estado de un registro con baja lógica (RF-08). */
export default function Insignia({ activo }) {
  return (
    <span className={`insignia ${activo ? 'insignia--activo' : 'insignia--inactivo'}`}>
      {activo ? 'Activo' : 'Inactivo'}
    </span>
  );
}
