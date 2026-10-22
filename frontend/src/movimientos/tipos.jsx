import { ArrowDownLeft, ArrowLeftRight, ArrowUpRight } from 'lucide-react';

/** Tipos de movimiento (RN-01) con su etiqueta, su ícono y su color. */
export const TIPOS = {
  ENTRADA: { etiqueta: 'Entrada', clase: 'entrada', Icono: ArrowDownLeft },
  SALIDA: { etiqueta: 'Salida', clase: 'salida', Icono: ArrowUpRight },
  TRANSFERENCIA: { etiqueta: 'Transferencia', clase: 'transferencia', Icono: ArrowLeftRight },
};

/** Estados de una transferencia (RN-10 a RN-13). */
export const ESTADOS = {
  PENDIENTE: { etiqueta: 'Pendiente', clase: 'pendiente' },
  CONFIRMADA: { etiqueta: 'Confirmada', clase: 'confirmada' },
  CANCELADA: { etiqueta: 'Cancelada', clase: 'cancelada' },
};

/** Tipo del movimiento y, si es una transferencia, su estado. */
export function InsigniaTipo({ movimiento }) {
  const tipo = TIPOS[movimiento.tipo];
  const estado = ESTADOS[movimiento.estadoTransferencia];
  return (
    <span className="mov-tipo">
      <span className={`insignia insignia--${tipo.clase}`}>
        <tipo.Icono size={13} aria-hidden="true" />
        {tipo.etiqueta}
      </span>
      {estado && <span className={`insignia insignia--${estado.clase}`}>{estado.etiqueta}</span>}
    </span>
  );
}

/** "Central → Sucursal Norte", según los depósitos que tenga el movimiento (RN-03 a RN-05). */
export function Recorrido({ movimiento }) {
  const origen = movimiento.depositoOrigen?.nombre;
  const destino = movimiento.depositoDestino?.nombre;
  if (origen && destino) {
    return (
      <span className="recorrido">
        {origen} <span aria-hidden="true">→</span>
        <span className="solo-lectores">hacia</span> {destino}
      </span>
    );
  }
  return <span className="recorrido">{origen ?? destino ?? '—'}</span>;
}

export const nombreCompleto = (usuario) => (usuario ? `${usuario.nombre} ${usuario.apellido}` : '—');
