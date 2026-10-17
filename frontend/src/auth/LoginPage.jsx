import { useEffect, useRef, useState } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from './AuthContext.jsx';
import { mensajeDeError } from '../shared/api/cliente.js';
import Logo from '../shared/layout/Logo.jsx';
import './login.css';

export default function LoginPage() {
  const { usuario, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [demorado, setDemorado] = useState(false);
  const temporizador = useRef(null);

  useEffect(() => () => clearTimeout(temporizador.current), []);

  if (usuario) {
    return <Navigate to={location.state?.desde ?? '/'} replace />;
  }

  async function ingresar(evento) {
    evento.preventDefault();
    if (!username.trim() || !password) {
      setError('Completá el usuario y la contraseña.');
      return;
    }
    setError('');
    setEnviando(true);
    // Si el servidor está "dormido" (Render), avisamos que puede tardar
    temporizador.current = setTimeout(() => setDemorado(true), 6000);
    try {
      await login(username.trim(), password);
      navigate(location.state?.desde ?? '/', { replace: true });
    } catch (err) {
      setError(mensajeDeError(err));
      setEnviando(false);
    } finally {
      clearTimeout(temporizador.current);
      setDemorado(false);
    }
  }

  return (
    <main className="login">
      <div className="login__contenedor">
        <div className="login__marca">
          <Logo />
        </div>

        <form className="tarjeta login__form" onSubmit={ingresar} noValidate>
          <h1>Ingresar</h1>
          <p className="login__ayuda">Usá el usuario y la contraseña que te dio el encargado.</p>

          <label className="campo">
            <span className="campo__etiqueta">Usuario</span>
            <input
              type="text"
              autoComplete="username"
              autoFocus
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              disabled={enviando}
            />
          </label>

          <label className="campo">
            <span className="campo__etiqueta">Contraseña</span>
            <input
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              disabled={enviando}
            />
          </label>

          {error && (
            <p className="alerta alerta--error" role="alert">
              {error}
            </p>
          )}

          <button type="submit" className="boton boton--primario boton--ancho" disabled={enviando}>
            {enviando ? 'Ingresando…' : 'Ingresar'}
          </button>

          {demorado && (
            <p className="login__aviso" role="status">
              El servidor se está iniciando. La primera vez puede tardar hasta un minuto.
            </p>
          )}
        </form>
      </div>
    </main>
  );
}