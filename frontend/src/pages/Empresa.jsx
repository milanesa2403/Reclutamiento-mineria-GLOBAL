import { useEffect, useState } from 'react';
import { api } from '../api.js';

const ESTADOS = ['ENVIADA', 'EN_REVISION', 'ENTREVISTA', 'ACEPTADA', 'RECHAZADA'];
const VACIA = { titulo: '', descripcion: '', especialidadRequerida: '', pais: 'PE', salario: 0, moneda: 'USD' };

export default function Empresa() {
  const [f, setF] = useState(VACIA);
  const [ofertas, setOfertas] = useState([]);
  const [abierta, setAbierta] = useState(null);
  const [posts, setPosts] = useState([]);
  const [aviso, setAviso] = useState({ tipo: '', texto: '' });
  const avisar = (tipo, texto) => setAviso({ tipo, texto });

  const cargar = () => api.ofertasEmpresa().then((r) => setOfertas(r.datos)).catch((e) => avisar('error', e.message));
  useEffect(() => { cargar(); }, []);

  async function publicar(e) {
    e.preventDefault();
    try {
      await api.publicar({ ...f, salario: Number(f.salario) });
      setF(VACIA);
      avisar('ok', 'Oferta publicada.');
      cargar();
    } catch (err) { avisar('error', err.message); }
  }

  async function verPostulaciones(id) {
    setAbierta(id);
    try { setPosts((await api.postulacionesDe(id)).datos); } catch (err) { avisar('error', err.message); }
  }

  async function estado(id, nuevo) {
    try { await api.cambiarEstado(id, nuevo); verPostulaciones(abierta); } catch (err) { avisar('error', err.message); }
  }

  async function cerrar(id) {
    try { await api.cerrarOferta(id); cargar(); } catch (err) { avisar('error', err.message); }
  }

  const c = (k) => (e) => setF({ ...f, [k]: e.target.value });

  return (
    <>
      {aviso.texto && <p className={aviso.tipo}>{aviso.texto}</p>}
      <form className="tarjeta" onSubmit={publicar}>
        <h2>Publicar oferta</h2>
        <label>Título<input required value={f.titulo} onChange={c('titulo')} /></label>
        <label>Descripción<textarea value={f.descripcion} onChange={c('descripcion')} /></label>
        <div className="fila">
          <label>Especialidad<input value={f.especialidadRequerida} onChange={c('especialidadRequerida')} /></label>
          <label>País (ISO)<input required maxLength={2} value={f.pais} onChange={c('pais')} /></label>
          <label>Salario<input type="number" min="0" value={f.salario} onChange={c('salario')} /></label>
          <label>Moneda (ISO 4217)<input required maxLength={3} value={f.moneda} onChange={c('moneda')} /></label>
        </div>
        <button>Publicar</button>
      </form>
      <section className="tarjeta">
        <h2>Mis ofertas</h2>
        {ofertas.length === 0 && <p className="vacio">Aún no publicaste ofertas.</p>}
        {ofertas.map((o) => (
          <article key={o.id} className="item">
            <div><strong>{o.titulo}</strong><small>{o.pais} · {o.salario} {o.moneda}</small></div>
            <div className="acciones">
              <span className="etiqueta">{o.estado}</span>
              <button onClick={() => verPostulaciones(o.id)}>Postulantes</button>
              {o.estado === 'ABIERTA' && <button className="sec" onClick={() => cerrar(o.id)}>Cerrar</button>}
            </div>
          </article>
        ))}
      </section>
      {abierta && (
        <section className="tarjeta">
          <h2>Postulantes</h2>
          {posts.length === 0 && <p className="vacio">Sin postulaciones todavía.</p>}
          {posts.map((p) => (
            <article key={p.id} className="item">
              <div><small>Trabajador {p.trabajadorId.slice(0, 8)}</small></div>
              <div className="acciones">
                <a href={`/v1/documentos/${p.documentoCvId}`} onClick={(e) => { e.preventDefault(); avisar('error', 'La descarga requiere el token: se implementa en la siguiente etapa.'); }}>CV</a>
                <select value={p.estado} onChange={(e) => estado(p.id, e.target.value)}>
                  {ESTADOS.map((x) => <option key={x}>{x}</option>)}
                </select>
              </div>
            </article>
          ))}
        </section>
      )}
    </>
  );
}
