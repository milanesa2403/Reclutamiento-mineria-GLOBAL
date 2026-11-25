import { useState } from 'react';
import { api, sesion } from '../api.js';

export default function Auth({ onEntrar }) {
  const [modo, setModo] = useState('login');
  const [f, setF] = useState({ correo: '', clave: '', rol: 'TRABAJADOR', nombre: '', pais: 'PE', idioma: 'es', consentimiento: false });
  const [error, setError] = useState('');
  const [cargando, setCargando] = useState(false);
  const cambiar = (k) => (e) => setF({ ...f, [k]: e.target.type === 'checkbox' ? e.target.checked : e.target.value });

  async function enviar(e) {
    e.preventDefault();
    setError('');
    setCargando(true);
    try {
      if (modo === 'registro') await api.registro(f);
      const { datos } = await api.login(f.correo, f.clave);
      onEntrar({ token: datos.token, rol: datos.rol, correo: f.correo });
    } catch (err) {
      setError(err.message);
    } finally {
      setCargando(false);
    }
  }

  return (
    <form className="tarjeta estrecha" onSubmit={enviar}>
      <h2>{modo === 'login' ? 'Iniciar sesión' : 'Crear cuenta'}</h2>
      <label>Correo<input type="email" required value={f.correo} onChange={cambiar('correo')} /></label>
      <label>Contraseña (mínimo 12 caracteres)
        <input type="password" required minLength={modo === 'registro' ? 12 : 1} value={f.clave} onChange={cambiar('clave')} />
      </label>
      {modo === 'registro' && (
        <>
          <label>Soy
            <select value={f.rol} onChange={cambiar('rol')}>
              <option value="TRABAJADOR">Trabajador</option>
              <option value="EMPRESA">Empresa</option>
            </select>
          </label>
          <label>{f.rol === 'EMPRESA' ? 'Razón social' : 'Nombre completo'}
            <input required value={f.nombre} onChange={cambiar('nombre')} />
          </label>
          <div className="fila">
            <label>País (ISO 3166-1)<input required maxLength={2} value={f.pais} onChange={cambiar('pais')} /></label>
            <label>Idioma (ISO 639-1)<input required maxLength={2} value={f.idioma} onChange={cambiar('idioma')} /></label>
          </div>
          <label className="check">
            <input type="checkbox" checked={f.consentimiento} onChange={cambiar('consentimiento')} required />
            Acepto el tratamiento de mis datos personales (Ley 29733 / RGPD).
          </label>
        </>
      )}
      {error && <p className="error">{error}</p>}
      <button disabled={cargando}>{cargando ? 'Procesando…' : modo === 'login' ? 'Entrar' : 'Registrarme'}</button>
      <p className="enlace" onClick={() => { setError(''); setModo(modo === 'login' ? 'registro' : 'login'); }}>
        {modo === 'login' ? '¿No tienes cuenta? Regístrate' : '¿Ya tienes cuenta? Inicia sesión'}
      </p>
    </form>
  );
}
