import { useState } from 'react';
import Modal from '../../shared/ui/Modal.jsx';
import { erroresDeCampos, mensajeDeError } from '../../shared/api/cliente.js';
import { aTextoDecimal, leerDecimal } from './decimales.js';
import { productosApi } from './productosApi.js';

const LIMITES = { codigo: 50, nombre: 150, unidadMedida: 20 };
const UNIDADES_SUGERIDAS = ['un', 'kg', 'g', 'l', 'ml', 'm', 'caja', 'pack'];

/**
 * Opciones de un select de categoría o proveedor: solo los activos (RN-43),
 * más el que el producto ya tiene aunque esté inactivo, para no perderlo al editar.
 */
function opcionesDe(lista, idActual) {
  return lista
    .filter((x) => x.activo || x.id === idActual)
    .map((x) => ({ id: x.id, texto: x.activo ? x.nombre : `${x.nombre} (inactivo)` }));
}

/** Alta y edición de un producto (RF-06). Con `producto` edita; sin él, crea. */
export default function ProductoForm({ producto, categorias, proveedores, onGuardado, onCancelar }) {
  const [datos, setDatos] = useState({
    codigo: producto?.codigo ?? '',
    nombre: producto?.nombre ?? '',
    categoriaId: producto?.categoriaId?.toString() ?? '',
    proveedorId: producto?.proveedorId?.toString() ?? '',
    unidadMedida: producto?.unidadMedida ?? '',
    precioReferencia: aTextoDecimal(producto?.precioReferencia),
    stockSeguridad: producto ? aTextoDecimal(producto.stockSeguridad) : '0',
    fraccionable: producto?.fraccionable ?? true,
  });
  const [errores, setErrores] = useState({});
  const [errorGeneral, setErrorGeneral] = useState('');
  const [enviando, setEnviando] = useState(false);

  const cambiar = (campo) => (e) => setDatos((d) => ({ ...d, [campo]: e.target.value }));

  function validar() {
    const nuevos = {};
    for (const campo of Object.keys(LIMITES)) {
      const valor = datos[campo].trim();
      if (!valor) nuevos[campo] = 'Es obligatorio.';
      else if (valor.length > LIMITES[campo]) nuevos[campo] = `No puede superar los ${LIMITES[campo]} caracteres.`;
    }
    if (!datos.categoriaId) nuevos.categoriaId = 'Elegí una categoría.';
    if (!datos.proveedorId) nuevos.proveedorId = 'Elegí un proveedor.';
    if (leerDecimal(datos.precioReferencia) === null) {
      nuevos.precioReferencia = 'Ingresá un número de 0 o más, con hasta dos decimales.';
    }
    if (leerDecimal(datos.stockSeguridad) === null) {
      nuevos.stockSeguridad = 'Ingresá un número de 0 o más, con hasta dos decimales.';
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
      codigo: datos.codigo.trim(),
      nombre: datos.nombre.trim(),
      categoriaId: Number(datos.categoriaId),
      proveedorId: Number(datos.proveedorId),
      unidadMedida: datos.unidadMedida.trim(),
      precioReferencia: leerDecimal(datos.precioReferencia),
      stockSeguridad: leerDecimal(datos.stockSeguridad),
      fraccionable: datos.fraccionable,
    };
    setEnviando(true);
    try {
      const guardado = producto
        ? await productosApi.editar(producto.id, cuerpo)
        : await productosApi.crear(cuerpo);
      onGuardado(guardado, producto ? 'editado' : 'creado');
    } catch (err) {
      const delServidor = erroresDeCampos(err);
      if (Object.keys(delServidor).length > 0) setErrores(delServidor);
      // 409: código duplicado, categoría o proveedor inactivo (RN-43), paso a no fraccionable con stock no entero (RN-47)
      else setErrorGeneral(mensajeDeError(err));
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

  const campoSelect = (campo, etiqueta, opciones, vacio) => (
    <label className="campo">
      <span className="campo__etiqueta">{etiqueta}</span>
      <select value={datos[campo]} onChange={cambiar(campo)} aria-invalid={Boolean(errores[campo])}>
        <option value="">{vacio}</option>
        {opciones.map((o) => (
          <option key={o.id} value={o.id}>
            {o.texto}
          </option>
        ))}
      </select>
      {errores[campo] && <span className="campo__error">{errores[campo]}</span>}
    </label>
  );

  const campoDecimal = (campo, etiqueta, ayuda) => (
    <label className="campo">
      <span className="campo__etiqueta">{etiqueta}</span>
      <input
        type="text"
        inputMode="decimal"
        value={datos[campo]}
        onChange={cambiar(campo)}
        aria-invalid={Boolean(errores[campo])}
      />
      {ayuda && <span className="campo__ayuda">{ayuda}</span>}
      {errores[campo] && <span className="campo__error">{errores[campo]}</span>}
    </label>
  );

  return (
    <Modal
      titulo={producto ? 'Editar producto' : 'Nuevo producto'}
      onCerrar={onCancelar}
      pie={
        <>
          <button type="button" className="boton boton--secundario" onClick={onCancelar} disabled={enviando}>
            Cancelar
          </button>
          <button type="submit" form="form-producto" className="boton boton--primario" disabled={enviando}>
            {enviando ? 'Guardando…' : 'Guardar'}
          </button>
        </>
      }
    >
      <form id="form-producto" onSubmit={guardar} noValidate>
        {errorGeneral && (
          <p className="alerta alerta--error" role="alert">
            {errorGeneral}
          </p>
        )}

        {campoTexto('codigo', 'Código', { autoFocus: true })}
        {campoTexto('nombre', 'Nombre')}
        {campoSelect(
          'categoriaId',
          'Categoría',
          opcionesDe(categorias, producto?.categoriaId),
          'Elegí una categoría'
        )}
        {campoSelect(
          'proveedorId',
          'Proveedor',
          opcionesDe(proveedores, producto?.proveedorId),
          'Elegí un proveedor'
        )}

        {campoTexto('unidadMedida', 'Unidad de medida', { list: 'unidades-sugeridas' })}
        <datalist id="unidades-sugeridas">
          {UNIDADES_SUGERIDAS.map((u) => (
            <option key={u} value={u} />
          ))}
        </datalist>

        {campoDecimal('precioReferencia', 'Precio de referencia', 'Se usa para valorizar el stock y la clasificación ABC.')}
        {campoDecimal('stockSeguridad', 'Stock de seguridad', 'Entra en el cálculo del punto de reposición.')}

        <label className="campo campo--casilla">
          <input
            type="checkbox"
            checked={datos.fraccionable}
            onChange={(e) => setDatos((d) => ({ ...d, fraccionable: e.target.checked }))}
          />
          <span>
            <span className="campo__etiqueta">Se puede fraccionar</span>
            <span className="campo__ayuda">
              Si lo desmarcás, las cantidades de este producto tienen que ser enteras. No se puede desmarcar si el
              stock actual tiene decimales.
            </span>
          </span>
        </label>
      </form>
    </Modal>
  );
}
