import { useState } from 'react';
import { sesion } from './api.js';
import Auth from './pages/Auth.jsx';
import Trabajador from './pages/Trabajador.jsx';
import Empresa from './pages/Empresa.jsx';
import Admin from './pages/Admin.jsx';

const VISTAS = { TRABAJADOR: Trabajador, EMPRESA: Empresa, ADMINISTRADOR: Admin };

export default function App() {
  const [s, setS] = useState(sesion.leer());
  const salir = () => { sesion.borrar(); setS(null); };
  const Vista = s ? VISTAS[s.rol] : null;

  return (
    <>
      <header>
        <h1>⛏️ Reclutamiento Minero Global</h1>
        {s && (
          <span>
            {s.rol} · {s.correo} <button onClick={salir}>Salir</button>
          </span>
        )}
      </header>
      <main>
        {Vista ? <Vista onSalir={salir} /> : <Auth onEntrar={(x) => { sesion.guardar(x); setS(x); }} />}
      </main>
    </>
  );
}
