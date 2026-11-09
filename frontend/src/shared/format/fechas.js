/**
 * Fechas de la API (RN-41): el backend las guarda y las devuelve en UTC; en pantalla
 * siempre se muestran en hora de Argentina.
 */
const ZONA = 'America/Argentina/Buenos_Aires';

const formatoFechaHora = new Intl.DateTimeFormat('es-AR', {
  timeZone: ZONA,
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
});

// 'en-CA' da el formato AAAA-MM-DD que usan los <input type="date"> y los filtros de la API
const formatoDia = new Intl.DateTimeFormat('en-CA', { timeZone: ZONA });

/** Convierte el texto de la API en Date. Si llegara sin zona horaria, se interpreta como UTC. */
export function aFecha(valor) {
  if (!valor) return null;
  const tieneZona = /([zZ]|[+-]\d{2}:?\d{2})$/.test(valor);
  return new Date(tieneZona ? valor : `${valor}Z`);
}

/** "22/10/2026, 18:30" en hora de Argentina. */
export function formatearFechaHora(valor) {
  const fecha = aFecha(valor);
  return fecha ? formatoFechaHora.format(fecha) : '—';
}

/** Día de hoy en Argentina, en formato AAAA-MM-DD. */
export function hoy() {
  return formatoDia.format(new Date());
}

/** Día de hace `dias` días en Argentina, en formato AAAA-MM-DD. */
export function haceDias(dias) {
  return formatoDia.format(new Date(Date.now() - dias * 24 * 60 * 60 * 1000));
}
