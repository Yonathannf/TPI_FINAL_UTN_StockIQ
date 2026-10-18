/** Valores del filtro. El backend espera `activo=true|false`, o sin el parámetro para traer todos. */
export const FILTRO_ACTIVO = {
  todos: undefined,
  activos: true,
  inactivos: false,
};

export default function FiltroActivo({ valor, onCambio }) {
  return (
    <label className="filtro">
      <span className="filtro__etiqueta">Estado</span>
      <select value={valor} onChange={(e) => onCambio(e.target.value)}>
        <option value="activos">Activos</option>
        <option value="inactivos">Inactivos</option>
        <option value="todos">Todos</option>
      </select>
    </label>
  );
}
