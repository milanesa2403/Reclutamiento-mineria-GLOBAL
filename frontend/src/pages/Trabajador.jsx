import { useEffect, useState } from 'react';
import { api } from '../api.js';

export default function Trabajador() {
  const [ofertas, setOfertas] = useState([]);
  const [docs, setDocs] = useState([]);
  const [posts, setPosts] = useState([]);
  const [filtro, setFiltro] = useState({ especialidad: '', pais: '' });
  const [cv, setCv] = useState('');
  const [aviso, setAviso] = useState({ tipo: '', texto: '' });

  const avisar = (tipo, texto) => setAviso({ tipo, texto });
  const cargarOfertas = () => api.ofertas(filtro.especialidad, filtro.pais).then((r) => setOfertas(r.datos)).catch((e) => avisar('error', e.message));
  const cargarDocs = () => api.misDocumentos().then((r) => { setDocs(r.datos); if (!cv && r.datos.length) setCv(r.datos.find((d) => d.tipo === 'CV')?.id || ''); });
  const cargarPosts = () => api.misPostulaciones().then((r) => setPosts(r.datos));

  useEffect(() => { cargarOfertas(); cargarDocs(); cargarPosts(); }, []);

  async function subir(e) {
    const archivo = e.target.files[0];
    if (!archivo) return;
    try {
      const { datos } = await api.subirDocumento('CV', archivo);
      setCv(datos.id);
      avisar('ok', datos.estado === 'PENDIENTE_REVISION' ? 'CV subido, pendiente de revisión.' : 'CV subido correctamente.');
      cargarDocs();
    } catch (err) { avisar('error', err.message); }
    e.target.value = '';
  }

  async function postular(ofertaId) {
    if (!cv) return avisar('error', 'Primero sube tu CV (PDF, máx. 10 MB).');
    try {
      const r = await api.postular(ofertaId, cv);
      avisar('ok', r.estado === 202 ? r.datos.mensaje : 'Postulación enviada.');
      cargarPosts();
    } catch (err) { avisar('error', err.message); }
  }

  const titulo = (id) => ofertas.find((o) => o.id === id)?.titulo || id.slice(0, 8);

  return (
    <>
      {aviso.texto && <p className={aviso.tipo}>{aviso.texto}</p>}
      <section className="tarjeta">
        <h2>Mi CV</h2>
        <input type="file" accept=".pdf,.doc,.docx,.png,.jpg" onChange={subir} />
        {docs.length > 0 && (
          <select value={cv} onChange={(e) => setCv(e.target.value)}>
            {docs.filter((d) => d.tipo === 'CV').map((d) => <option key={d.id} value={d.id}>{d.nombreArchivo} ({d.estado})</option>)}
          </select>
        )}
      </section>
      <section className="tarjeta">
        <h2>Ofertas abiertas</h2>
        <div className="fila">
          <input placeholder="Especialidad (ej. perforación)" value={filtro.especialidad} onChange={(e) => setFiltro({ ...filtro, especialidad: e.target.value })} />
          <input placeholder="País (ej. CL)" maxLength={2} value={filtro.pais} onChange={(e) => setFiltro({ ...filtro, pais: e.target.value })} />
          <button onClick={cargarOfertas}>Buscar</button>
        </div>
        {ofertas.length === 0 && <p className="vacio">No hay ofertas con esos filtros.</p>}
        {ofertas.map((o) => (
          <article key={o.id} className="item">
            <div>
              <strong>{o.titulo}</strong>
              <small>{o.especialidadRequerida} · {o.pais} · {o.salario} {o.moneda}</small>
              <p>{o.descripcion}</p>
            </div>
            <button onClick={() => postular(o.id)}>Postular</button>
          </article>
        ))}
      </section>
      <section className="tarjeta">
        <h2>Mis postulaciones</h2>
        {posts.length === 0 && <p className="vacio">Aún no has postulado.</p>}
        {posts.map((p) => (
          <article key={p.id} className="item">
            <span>{titulo(p.ofertaId)}</span>
            <span className="etiqueta">{p.estado}</span>
          </article>
        ))}
      </section>
    </>
  );
}
