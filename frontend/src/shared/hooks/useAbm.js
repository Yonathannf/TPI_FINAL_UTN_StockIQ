import { useCallback, useEffect, useMemo, useState } from 'react';
import { mensajeDeError } from '../api/cliente.js';
import { FILTRO_ACTIVO } from '../ui/FiltroActivo.jsx';

/**
 * Estado y acciones comunes de una pantalla ABM con baja lógica:
 * carga con filtro de estado, búsqueda local, formulario, activar, desactivar y eliminar.
 *
 * - `api`: lo que devuelve crearApiRecurso().
 * - `camposBusqueda`: función que da los textos de un registro sobre los que se busca.
 * - `femenino`: para concordar los avisos ("desactivada" / "desactivado").
 */
export default function useAbm({ api, camposBusqueda, femenino = false }) {
  const [items, setItems] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const [aviso, setAviso] = useState('');
  const [filtro, setFiltro] = useState('activos');
  const [texto, setTexto] = useState('');
  const [formulario, setFormulario] = useState(null); // { item? } | null
  const [confirmacion, setConfirmacion] = useState(null); // { tipo: 'baja' | 'eliminar', item, error, procesando } | null

  const cargar = useCallback(async () => {
    setCargando(true);
    setError('');
    try {
      setItems(await api.listar({ activo: FILTRO_ACTIVO[filtro] }));
    } catch (err) {
      setError(mensajeDeError(err));
    } finally {
      setCargando(false);
    }
  }, [api, filtro]);

  useEffect(() => {
    cargar();
  }, [cargar]);

  const visibles = useMemo(() => {
    const buscado = texto.trim().toLowerCase();
    if (!buscado) return items;
    return items.filter((item) =>
      camposBusqueda(item).some((campo) => (campo ?? '').toString().toLowerCase().includes(buscado))
    );
    // camposBusqueda es estable: se define fuera del componente
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [items, texto]);

  const sufijo = femenino ? 'a' : 'o';

  /** Se llama desde el formulario cuando el guardado salió bien. */
  function alGuardar(guardado, accion) {
    setFormulario(null);
    setAviso(`"${guardado.nombre}" ${accion} correctamente.`);
    cargar();
  }

  async function activar(item) {
    setAviso('');
    setError('');
    try {
      await api.activar(item.id);
      setAviso(`"${item.nombre}" activad${sufijo}.`);
      cargar();
    } catch (err) {
      setError(mensajeDeError(err));
    }
  }

  async function confirmar() {
    const { tipo, item } = confirmacion;
    setConfirmacion({ tipo, item, error: '', procesando: true });
    try {
      if (tipo === 'baja') await api.desactivar(item.id);
      else await api.eliminar(item.id);
      setConfirmacion(null);
      setAviso(`"${item.nombre}" ${tipo === 'baja' ? 'desactivad' : 'eliminad'}${sufijo}.`);
      cargar();
    } catch (err) {
      // 409: por ejemplo, tiene productos asociados o stock
      setConfirmacion({ tipo, item, error: mensajeDeError(err), procesando: false });
    }
  }

  return {
    items,
    visibles,
    cargando,
    error,
    aviso,
    filtro,
    setFiltro,
    texto,
    setTexto,
    formulario,
    abrirFormulario: (item) => setFormulario({ item }),
    cerrarFormulario: () => setFormulario(null),
    confirmacion,
    pedirBaja: (item) => setConfirmacion({ tipo: 'baja', item, error: '', procesando: false }),
    pedirEliminar: (item) => setConfirmacion({ tipo: 'eliminar', item, error: '', procesando: false }),
    cancelarConfirmacion: () => setConfirmacion(null),
    alGuardar,
    activar,
    confirmar,
  };
}
