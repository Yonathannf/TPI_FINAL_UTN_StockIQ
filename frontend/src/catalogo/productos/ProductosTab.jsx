import { useEffect, useState } from 'react';
import { Pencil, Plus, Power, PowerOff } from 'lucide-react';
import { mensajeDeError } from '../../shared/api/cliente.js';
import useAbm from '../../shared/hooks/useAbm.js';
import ConfirmarDialogo from '../../shared/ui/ConfirmarDialogo.jsx';
import FiltroActivo from '../../shared/ui/FiltroActivo.jsx';
import Insignia from '../../shared/ui/Insignia.jsx';
import { categoriasApi } from '../categorias/categoriasApi.js';
import { proveedoresApi } from '../proveedores/proveedoresApi.js';
import { formatearCantidad, formatearPrecio } from './decimales.js';
import ProductoForm from './ProductoForm.jsx';
import { productosApi } from './productosApi.js';

const camposBusqueda = (p) => [p.codigo, p.nombre, p.categoria?.nombre, p.proveedor?.nombre];
const etiqueta = (p) => p.nombre;

/** Listado y ABM de productos con filtros por categoría, proveedor y estado (RF-06, RF-08). */
export default function ProductosTab() {
  const [categorias, setCategorias] = useState([]);
  const [proveedores, setProveedores] = useState([]);
  const [errorOpciones, setErrorOpciones] = useState('');
  const [categoriaId, setCategoriaId] = useState('');
  const [proveedorId, setProveedorId] = useState('');

  const abm = useAbm({
    api: productosApi,
    camposBusqueda,
    etiqueta,
    filtrosExtra: { categoriaId, proveedorId },
  });
  const { confirmacion } = abm;

  // Categorías y proveedores (todos, activos o no) para los filtros y el formulario
  useEffect(() => {
    Promise.all([categoriasApi.listar({}), proveedoresApi.listar({})])
      .then(([cats, provs]) => {
        setCategorias(cats);
        setProveedores(provs);
      })
      .catch((err) => setErrorOpciones(mensajeDeError(err)));
  }, []);

  return (
    <>
      {abm.aviso && (
        <p className="alerta alerta--exito" role="status">
          {abm.aviso}
        </p>
      )}
      {(abm.error || errorOpciones) && (
        <p className="alerta alerta--error" role="alert">
          {abm.error || errorOpciones}
        </p>
      )}

      <div className="tarjeta herramientas">
        <label className="filtro filtro--ancho">
          <span className="filtro__etiqueta">Buscar</span>
          <input
            type="search"
            placeholder="Código o nombre"
            value={abm.texto}
            onChange={(e) => abm.setTexto(e.target.value)}
          />
        </label>
        <label className="filtro">
          <span className="filtro__etiqueta">Categoría</span>
          <select value={categoriaId} onChange={(e) => setCategoriaId(e.target.value)}>
            <option value="">Todas</option>
            {categorias.map((c) => (
              <option key={c.id} value={c.id}>
                {c.nombre}
                {c.activo ? '' : ' (inactiva)'}
              </option>
            ))}
          </select>
        </label>
        <label className="filtro">
          <span className="filtro__etiqueta">Proveedor</span>
          <select value={proveedorId} onChange={(e) => setProveedorId(e.target.value)}>
            <option value="">Todos</option>
            {proveedores.map((p) => (
              <option key={p.id} value={p.id}>
                {p.nombre}
                {p.activo ? '' : ' (inactivo)'}
              </option>
            ))}
          </select>
        </label>
        <FiltroActivo valor={abm.filtro} onCambio={abm.setFiltro} />
        <button type="button" className="boton boton--primario" onClick={() => abm.abrirFormulario()}>
          <Plus size={18} aria-hidden="true" />
          Nuevo producto
        </button>
      </div>

      <div className="tarjeta tabla__contenedor">
        <table className="tabla">
          <thead>
            <tr>
              <th>Código</th>
              <th>Producto</th>
              <th>Proveedor</th>
              <th>Unidad</th>
              <th className="tabla__numero">Precio ref.</th>
              <th className="tabla__numero">Stock seg.</th>
              <th>Estado</th>
              <th aria-label="Acciones" />
            </tr>
          </thead>
          <tbody>
            {abm.cargando && (
              <tr>
                <td colSpan={8} className="tabla__vacio">
                  Cargando…
                </td>
              </tr>
            )}
            {!abm.cargando && abm.visibles.length === 0 && (
              <tr>
                <td colSpan={8} className="tabla__vacio">
                  {abm.items.length === 0 ? 'No hay productos para mostrar.' : 'Ningún producto coincide con la búsqueda.'}
                </td>
              </tr>
            )}
            {!abm.cargando &&
              abm.visibles.map((p) => (
                <tr key={p.id} className={p.activo ? '' : 'fila--inactiva'}>
                  <td>
                    <code className="codigo">{p.codigo}</code>
                  </td>
                  <td>
                    <strong>{p.nombre}</strong>
                    <div className="tabla__secundario">{p.categoria?.nombre}</div>
                  </td>
                  <td>{p.proveedor?.nombre}</td>
                  <td>
                    {p.unidadMedida}
                    {!p.fraccionable && <div className="tabla__secundario">Solo enteros</div>}
                  </td>
                  <td className="tabla__numero">{formatearPrecio(p.precioReferencia)}</td>
                  <td className="tabla__numero">{formatearCantidad(p.stockSeguridad)}</td>
                  <td>
                    <Insignia activo={p.activo} />
                  </td>
                  <td className="tabla__acciones">
                    <button type="button" className="boton-icono" title="Editar" onClick={() => abm.abrirFormulario(p)}>
                      <Pencil size={16} aria-hidden="true" />
                      <span className="solo-lectores">Editar {p.nombre}</span>
                    </button>
                    {p.activo ? (
                      <button type="button" className="boton-icono" title="Desactivar" onClick={() => abm.pedirBaja(p)}>
                        <PowerOff size={16} aria-hidden="true" />
                        <span className="solo-lectores">Desactivar {p.nombre}</span>
                      </button>
                    ) : (
                      <button type="button" className="boton-icono" title="Activar" onClick={() => abm.activar(p)}>
                        <Power size={16} aria-hidden="true" />
                        <span className="solo-lectores">Activar {p.nombre}</span>
                      </button>
                    )}
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>

      {abm.formulario && (
        <ProductoForm
          producto={abm.formulario.item}
          categorias={categorias}
          proveedores={proveedores}
          onGuardado={abm.alGuardar}
          onCancelar={abm.cerrarFormulario}
        />
      )}

      {confirmacion && (
        <ConfirmarDialogo
          titulo="Desactivar producto"
          mensaje={`¿Querés desactivar el producto "${confirmacion.item.nombre}"? No se puede si tiene stock, stock en tránsito o transferencias pendientes.`}
          textoConfirmar="Desactivar"
          error={confirmacion.error}
          procesando={confirmacion.procesando}
          onConfirmar={abm.confirmar}
          onCancelar={abm.cancelarConfirmacion}
        />
      )}
    </>
  );
}
