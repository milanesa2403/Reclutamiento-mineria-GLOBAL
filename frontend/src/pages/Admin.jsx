import { useEffect, useState } from 'react';
import { api } from '../api.js';

export default function Admin() {
  const [cola, setCola] = useState(null);
  const [registros, setRegistros] = useState([]);
  const [error, setError] = useState('');

  const cargar = () => {
    api.cola().then((r) => setCola(r.datos)).catch((e) => setError(e.message));
    api.auditoria().then((r) => setRegistros(r.datos)).catch((e) => setError(e.message));
  };
  useEffect(() => { cargar(); }, []);

  async function alternar() { await api.soloLectura(!cola.soloLectura); cargar(); }
  async function reprocesar() { await api.reprocesarDlq(); cargar(); }

  return (
    <>
      {error && <p className="error">{error}</p>}
      <section className="tarjeta">
        <h2>Estado del sistema</h2>
        {cola && (
          <>
            <p>Modo: <strong>{cola.soloLectura ? 'SOLO LECTURA' : 'Normal'}</strong> · En cola: {cola.pendientes} · En DLQ: {cola.dlq}</p>
            <div className="acciones">
              <button onClick={alternar}>{cola.soloLectura ? 'Volver a modo normal' : 'Simular caída de escritura'}</button>
              <button className="sec" onClick={reprocesar} disabled={!cola.dlq}>Reprocesar DLQ</button>
            </div>
          </>
        )}
      </section>
      <section className="tarjeta">
        <h2>Auditoría (últimos 100)</h2>
        <div className="tabla">
          <table>
            <thead><tr><th>Fecha (UTC)</th><th>Acción</th><th>Recurso</th><th>IP</th><th>Permitido</th></tr></thead>
            <tbody>
              {registros.map((r) => (
                <tr key={r.id} className={r.permitido ? '' : 'negado'}>
                  <td>{r.fecha}</td><td>{r.accion}</td><td>{r.recurso}</td><td>{r.ip}</td><td>{r.permitido ? 'Sí' : 'No'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </>
  );
}
