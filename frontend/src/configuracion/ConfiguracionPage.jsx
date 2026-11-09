import { useCallback, useEffect, useMemo, useState } from 'react';
import { RotateCcw } from 'lucide-react';
import { mensajeDeError } from '../shared/api/cliente.js';
import { leerDecimal } from '../catalogo/productos/decimales.js';
import { parametrosApi } from './parametrosApi.js';
import { DEFINICIONES, GRUPOS, validarParametros } from './parametros.js';
import './configuracion.css';

/** Configuración de los parámetros de cálculo (RF-22, RN-37). Solo ENCARGADO; la ruta ya está protegida. */
export default function ConfiguracionPage() {
  const [guardados, setGuardados] = useState({}); // { clave: valor } tal como está en el servidor
  const [valores, setValores] = useState({}); // lo que se está editando
  const [errores, setErrores] = useState({});
  const [cargando, setCargando] = useState(true);
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState('');
  const [aviso, setAviso] = useState('');

  const cargar = useCallback(async () => {
    setCargando(true);
    setError('');
    try {
      const lista = await parametrosApi.listar();
      const mapa = Object.fromEntries(lista.map((p) => [p.clave, String(p.valor)]));
      setGuardados(mapa);
      setValores(mapa);
    } catch (err) {
      setError(mensajeDeError(err));
    } finally {
      setCargando(false);
    }
  }, []);

  useEffect(() => {
    cargar();
  }, [cargar]);

  const cambiados = useMemo(
    () => Object.keys(DEFINICIONES).filter((c) => (valores[c] ?? '').trim() !== (guardados[c] ?? '')),
    [valores, guardados]
  );

  const cambiar = (clave) => (e) => {
    setValores((previos) => ({ ...previos, [clave]: e.target.value }));
    setErrores((previos) => ({ ...previos, [clave]: undefined }));
    setAviso('');
  };

  function restablecer() {
    setValores(Object.fromEntries(Object.entries(DEFINICIONES).map(([clave, def]) => [clave, def.defecto])));
    setErrores({});
    setAviso('');
  }

  /**
   * El servidor exige que corte_abc_a < corte_abc_b en cada pedido (RN-37), así que si cambian los dos,
   * el orden importa: si el corte B sube se guarda primero; si no, primero el A.
   */
  function ordenDeGuardado() {
    const lista = [...cambiados];
    const sube = (clave) => leerDecimal(valores[clave]) > leerDecimal(guardados[clave] ?? '0');
    if (lista.includes('corte_abc_a') && lista.includes('corte_abc_b') && sube('corte_abc_b')) {
      return ['corte_abc_b', ...lista.filter((c) => c !== 'corte_abc_b')];
    }
    return lista;
  }

  async function guardar(evento) {
    evento.preventDefault();
    setAviso('');
    setError('');
    const nuevos = validarParametros(valores);
    setErrores(nuevos);
    if (Object.keys(nuevos).length > 0 || cambiados.length === 0) return;

    setGuardando(true);
    try {
      for (const clave of ordenDeGuardado()) {
        await parametrosApi.modificar(clave, valores[clave].trim().replace(',', '.'));
      }
      setAviso('Parámetros guardados. Los próximos cálculos del dashboard ya usan los valores nuevos.');
    } catch (err) {
      setError(mensajeDeError(err));
    } finally {
      // Se vuelve a leer del servidor: si falló a mitad de camino, refleja lo que realmente quedó guardado
      await cargar();
      setGuardando(false);
    }
  }

  return (
    <div className="pagina">
      <header className="pagina__encabezado">
        <div>
          <h1>Configuración</h1>
          <p className="pagina__bajada">
            Parámetros de cálculo de la capa analítica. Se ajustan sin tocar el código.
          </p>
        </div>
      </header>

      {aviso && (
        <p className="alerta alerta--exito" role="status">
          {aviso}
        </p>
      )}
      {error && (
        <p className="alerta alerta--error" role="alert">
          {error}
        </p>
      )}

      {cargando ? (
        <p className="tabla__vacio">Cargando…</p>
      ) : (
        <form onSubmit={guardar} noValidate>
          {GRUPOS.map((grupo) => (
            <section key={grupo.titulo} className="tarjeta parametros__grupo">
              <h2>{grupo.titulo}</h2>
              <p className="parametros__bajada">{grupo.bajada}</p>
              <div className="parametros__campos">
                {grupo.claves.map((clave) => {
                  const def = DEFINICIONES[clave];
                  return (
                    <label key={clave} className="campo">
                      <span className="campo__etiqueta">{def.etiqueta}</span>
                      <span className="parametros__entrada">
                        <input
                          type="text"
                          inputMode="decimal"
                          value={valores[clave] ?? ''}
                          onChange={cambiar(clave)}
                          aria-invalid={Boolean(errores[clave])}
                          disabled={guardando}
                        />
                        <span className="parametros__unidad">{def.unidad}</span>
                      </span>
                      {errores[clave] ? (
                        <span className="campo__error">{errores[clave]}</span>
                      ) : (
                        <span className="campo__ayuda">{def.ayuda}</span>
                      )}
                    </label>
                  );
                })}
              </div>
            </section>
          ))}

          <div className="parametros__pie">
            <button type="button" className="boton boton--secundario" onClick={restablecer} disabled={guardando}>
              <RotateCcw size={16} aria-hidden="true" />
              Valores por defecto
            </button>
            <button
              type="button"
              className="boton boton--secundario"
              onClick={() => {
                setValores(guardados);
                setErrores({});
              }}
              disabled={guardando || cambiados.length === 0}
            >
              Descartar cambios
            </button>
            <button type="submit" className="boton boton--primario" disabled={guardando || cambiados.length === 0}>
              {guardando ? 'Guardando…' : 'Guardar cambios'}
            </button>
          </div>
        </form>
      )}
    </div>
  );
}
