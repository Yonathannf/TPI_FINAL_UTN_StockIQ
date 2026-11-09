import { useEffect, useMemo, useState } from 'react';
import Modal from '../shared/ui/Modal.jsx';
import { erroresDeCampos, mensajeDeError } from '../shared/api/cliente.js';
import { productosApi } from '../catalogo/productos/productosApi.js';
import { formatearCantidad, leerDecimal } from '../catalogo/productos/decimales.js';
import { depositosApi } from '../depositos/depositosApi.js';
import { stockApi } from '../stock/stockApi.js';
import { movimientosApi } from './movimientosApi.js';
import { TIPOS } from './tipos.jsx';

const MAX_MOTIVO = 255;
const MOTIVOS_SALIDA = ['Venta', 'Rotura', 'Vencimiento', 'Consumo interno', 'Ajuste'];

/** Qué depósitos lleva cada tipo de movimiento (RN-03 a RN-05). */
const CONFIG = {
  ENTRADA: { origen: false, destino: true, motivoObligatorio: false, registrar: movimientosApi.registrarEntrada },
  SALIDA: { origen: true, destino: false, motivoObligatorio: true, registrar: movimientosApi.registrarSalida },
  TRANSFERENCIA: {
    origen: true,
    destino: true,
    motivoObligatorio: false,
    registrar: movimientosApi.registrarTransferencia,
  },
};

const etiquetaProducto = (p) => `${p.codigo} — ${p.nombre}`;

/**
 * Registro de una entrada, una salida o una transferencia (RF-09 a RF-11).
 * La fecha y el usuario responsable los asigna el backend (RN-41); acá no se envían.
 */
export default function MovimientoForm({ tipo, onGuardado, onCancelar }) {
  const config = CONFIG[tipo];

  const [productos, setProductos] = useState([]);
  const [depositos, setDepositos] = useState([]);
  const [cargandoOpciones, setCargandoOpciones] = useState(true);

  const [datos, setDatos] = useState({
    productoId: '',
    depositoOrigenId: '',
    depositoDestinoId: '',
    cantidad: '',
    motivo: '',
  });
  const [disponible, setDisponible] = useState(null); // stock del producto en el depósito origen
  const [errores, setErrores] = useState({});
  const [errorGeneral, setErrorGeneral] = useState('');
  const [enviando, setEnviando] = useState(false);

  // Solo productos y depósitos activos: sobre los inactivos no se registran movimientos (RN-08)
  useEffect(() => {
    Promise.all([productosApi.listar({ activo: true }), depositosApi.listar(true)])
      .then(([prods, deps]) => {
        setProductos(prods);
        setDepositos(deps);
      })
      .catch((err) => setErrorGeneral(mensajeDeError(err)))
      .finally(() => setCargandoOpciones(false));
  }, []);

  const producto = useMemo(
    () => productos.find((p) => String(p.id) === datos.productoId) ?? null,
    [productos, datos.productoId]
  );

  // Stock disponible en el origen, para avisar antes de enviar (el backend igual lo valida, RN-06)
  useEffect(() => {
    setDisponible(null);
    if (!config.origen || !datos.productoId || !datos.depositoOrigenId) return undefined;
    let vigente = true;
    stockApi
      .deProducto(datos.productoId)
      .then((stock) => {
        if (!vigente) return;
        const fila = stock.porDeposito?.find((d) => String(d.deposito.id) === datos.depositoOrigenId);
        setDisponible(fila ? Number(fila.cantidad) : 0);
      })
      .catch(() => {
        // Si no se puede consultar, no se muestra el dato; el backend valida al guardar
      });
    return () => {
      vigente = false;
    };
  }, [config.origen, datos.productoId, datos.depositoOrigenId]);

  function cambiar(campo, valor) {
    setDatos((anteriores) => ({ ...anteriores, [campo]: valor }));
  }

  function validar() {
    const nuevos = {};
    if (!datos.productoId) nuevos.productoId = 'Elegí un producto.';
    if (config.origen && !datos.depositoOrigenId) nuevos.depositoOrigenId = 'Elegí el depósito de origen.';
    if (config.destino && !datos.depositoDestinoId) nuevos.depositoDestinoId = 'Elegí el depósito de destino.';
    if (config.origen && config.destino && datos.depositoOrigenId && datos.depositoOrigenId === datos.depositoDestinoId) {
      nuevos.depositoDestinoId = 'El destino tiene que ser distinto del origen.';
    }

    const cantidad = leerDecimal(datos.cantidad);
    if (!datos.cantidad.trim()) nuevos.cantidad = 'Ingresá la cantidad.';
    else if (cantidad === null) nuevos.cantidad = 'Usá un número con hasta dos decimales.';
    else if (cantidad <= 0) nuevos.cantidad = 'Tiene que ser mayor a 0.';
    else if (producto && !producto.fraccionable && !Number.isInteger(cantidad)) {
      nuevos.cantidad = 'Este producto solo admite cantidades enteras.';
    } else if (disponible !== null && cantidad > disponible) {
      nuevos.cantidad = `Supera el stock disponible en el depósito de origen (${formatearCantidad(disponible)}).`;
    }

    if (config.motivoObligatorio && !datos.motivo.trim()) nuevos.motivo = 'El motivo es obligatorio en las salidas.';
    else if (datos.motivo.trim().length > MAX_MOTIVO) nuevos.motivo = `No puede superar los ${MAX_MOTIVO} caracteres.`;
    return nuevos;
  }

  async function guardar(evento) {
    evento.preventDefault();
    const nuevos = validar();
    setErrores(nuevos);
    setErrorGeneral('');
    if (Object.keys(nuevos).length > 0) return;

    // Solo los campos que corresponden a cada tipo: el backend rechaza los de más
    const cuerpo = {
      productoId: Number(datos.productoId),
      cantidad: leerDecimal(datos.cantidad),
      motivo: datos.motivo.trim() || null,
    };
    if (config.origen) cuerpo.depositoOrigenId = Number(datos.depositoOrigenId);
    if (config.destino) cuerpo.depositoDestinoId = Number(datos.depositoDestinoId);

    setEnviando(true);
    try {
      const movimiento = await config.registrar(cuerpo);
      onGuardado(movimiento);
    } catch (err) {
      const delServidor = erroresDeCampos(err);
      if (Object.keys(delServidor).length > 0) setErrores(delServidor);
      else setErrorGeneral(mensajeDeError(err)); // por ejemplo, 409 por stock insuficiente
      setEnviando(false);
    }
  }

  const unidad = producto?.unidadMedida;
  const depositosDestino = config.origen
    ? depositos.filter((d) => String(d.id) !== datos.depositoOrigenId)
    : depositos;

  return (
    <Modal
      titulo={`Registrar ${TIPOS[tipo].etiqueta.toLowerCase()}`}
      onCerrar={onCancelar}
      pie={
        <>
          <button type="button" className="boton boton--secundario" onClick={onCancelar} disabled={enviando}>
            Cancelar
          </button>
          <button
            type="submit"
            form="form-movimiento"
            className="boton boton--primario"
            disabled={enviando || cargandoOpciones}
          >
            {enviando ? 'Registrando…' : 'Registrar'}
          </button>
        </>
      }
    >
      <form id="form-movimiento" onSubmit={guardar} noValidate>
        {errorGeneral && (
          <p className="alerta alerta--error" role="alert">
            {errorGeneral}
          </p>
        )}
        {tipo === 'TRANSFERENCIA' && (
          <p className="campo__ayuda form-mov__ayuda">
            La mercadería sale del origen al registrarla y queda en tránsito hasta que se confirme la recepción en el
            destino.
          </p>
        )}

        <label className="campo">
          <span className="campo__etiqueta">Producto</span>
          <select
            value={datos.productoId}
            onChange={(e) => cambiar('productoId', e.target.value)}
            aria-invalid={Boolean(errores.productoId)}
            disabled={cargandoOpciones}
            autoFocus
          >
            <option value="">{cargandoOpciones ? 'Cargando…' : 'Elegí un producto'}</option>
            {productos.map((p) => (
              <option key={p.id} value={p.id}>
                {etiquetaProducto(p)}
              </option>
            ))}
          </select>
          {errores.productoId && <span className="campo__error">{errores.productoId}</span>}
        </label>

        <div className="form-mov__fila">
          {config.origen && (
            <label className="campo">
              <span className="campo__etiqueta">Depósito de origen</span>
              <select
                value={datos.depositoOrigenId}
                onChange={(e) => cambiar('depositoOrigenId', e.target.value)}
                aria-invalid={Boolean(errores.depositoOrigenId)}
                disabled={cargandoOpciones}
              >
                <option value="">Elegí un depósito</option>
                {depositos.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.nombre}
                  </option>
                ))}
              </select>
              {errores.depositoOrigenId && <span className="campo__error">{errores.depositoOrigenId}</span>}
            </label>
          )}

          {config.destino && (
            <label className="campo">
              <span className="campo__etiqueta">Depósito de destino</span>
              <select
                value={datos.depositoDestinoId}
                onChange={(e) => cambiar('depositoDestinoId', e.target.value)}
                aria-invalid={Boolean(errores.depositoDestinoId)}
                disabled={cargandoOpciones}
              >
                <option value="">Elegí un depósito</option>
                {depositosDestino.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.nombre}
                  </option>
                ))}
              </select>
              {errores.depositoDestinoId && <span className="campo__error">{errores.depositoDestinoId}</span>}
            </label>
          )}
        </div>

        <label className="campo">
          <span className="campo__etiqueta">Cantidad{unidad ? ` (${unidad})` : ''}</span>
          <input
            type="text"
            inputMode="decimal"
            value={datos.cantidad}
            onChange={(e) => cambiar('cantidad', e.target.value)}
            aria-invalid={Boolean(errores.cantidad)}
            placeholder={producto && !producto.fraccionable ? 'Solo enteros' : 'Ej.: 12 o 2,5'}
          />
          {disponible !== null && !errores.cantidad && (
            <span className="campo__ayuda">
              Disponible en el depósito de origen: {formatearCantidad(disponible)}
              {unidad ? ` ${unidad}` : ''}
            </span>
          )}
          {errores.cantidad && <span className="campo__error">{errores.cantidad}</span>}
        </label>

        <label className="campo">
          <span className="campo__etiqueta">Motivo{config.motivoObligatorio ? '' : ' (opcional)'}</span>
          <input
            type="text"
            value={datos.motivo}
            onChange={(e) => cambiar('motivo', e.target.value)}
            maxLength={MAX_MOTIVO}
            aria-invalid={Boolean(errores.motivo)}
            list={tipo === 'SALIDA' ? 'motivos-salida' : undefined}
            placeholder={tipo === 'SALIDA' ? 'Venta, rotura, vencimiento, ajuste…' : ''}
          />
          {tipo === 'SALIDA' && (
            <datalist id="motivos-salida">
              {MOTIVOS_SALIDA.map((m) => (
                <option key={m} value={m} />
              ))}
            </datalist>
          )}
          {errores.motivo && <span className="campo__error">{errores.motivo}</span>}
        </label>
      </form>
    </Modal>
  );
}
