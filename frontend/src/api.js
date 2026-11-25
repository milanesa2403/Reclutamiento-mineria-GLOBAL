// Cliente minimo de la API /v1. El token vive en sessionStorage (se borra al cerrar la pestana).
const CLAVE = 'sesion';

export const sesion = {
  leer: () => { try { return JSON.parse(sessionStorage.getItem(CLAVE)); } catch { return null; } },
  guardar: (s) => sessionStorage.setItem(CLAVE, JSON.stringify(s)),
  borrar: () => sessionStorage.removeItem(CLAVE),
};

async function pedir(ruta, { metodo = 'GET', cuerpo, formData, idempotente = false } = {}) {
  const s = sesion.leer();
  const headers = {};
  if (s?.token) headers.Authorization = `Bearer ${s.token}`;
  if (cuerpo) headers['Content-Type'] = 'application/json';
  if (idempotente) headers['Idempotency-Key'] = crypto.randomUUID();
  const res = await fetch(ruta, { method: metodo, headers, body: formData ?? (cuerpo ? JSON.stringify(cuerpo) : undefined) });
  const texto = await res.text();
  const datos = texto ? JSON.parse(texto) : null;
  if (!res.ok) {
    if (res.status === 401 && s) { sesion.borrar(); window.location.reload(); }
    // Formato estandar del backend: { codigo, mensaje }
    throw Object.assign(new Error(datos?.mensaje || `Error ${res.status}`), { estado: res.status, codigo: datos?.codigo });
  }
  return { estado: res.status, datos };
}

export const api = {
  login: (correo, clave) => pedir('/v1/auth/login', { metodo: 'POST', cuerpo: { correo, clave } }),
  registro: (d) => pedir('/v1/auth/registro', { metodo: 'POST', cuerpo: d }),
  // ofertas
  ofertas: (especialidad = '', pais = '') =>
    pedir(`/v1/ofertas?especialidad=${encodeURIComponent(especialidad)}&pais=${encodeURIComponent(pais)}`),
  // trabajador
  misDocumentos: () => pedir('/v1/trabajador/documentos'),
  subirDocumento: (tipo, archivo) => {
    const f = new FormData();
    f.append('tipo', tipo);
    f.append('archivo', archivo);
    return pedir('/v1/documentos', { metodo: 'POST', formData: f });
  },
  postular: (ofertaId, cvId) =>
    pedir('/v1/trabajador/postulaciones', { metodo: 'POST', cuerpo: { ofertaId, cvId }, idempotente: true }),
  misPostulaciones: () => pedir('/v1/trabajador/postulaciones'),
  // empresa
  publicar: (d) => pedir('/v1/empresa/ofertas', { metodo: 'POST', cuerpo: d }),
  ofertasEmpresa: () => pedir('/v1/empresa/ofertas'),
  cerrarOferta: (id) => pedir(`/v1/empresa/ofertas/${id}/cerrar`, { metodo: 'POST' }),
  postulacionesDe: (id) => pedir(`/v1/empresa/ofertas/${id}/postulaciones`),
  cambiarEstado: (id, estado) =>
    pedir(`/v1/empresa/postulaciones/${id}/estado`, { metodo: 'PUT', cuerpo: { estado }, idempotente: true }),
  // admin
  auditoria: () => pedir('/v1/admin/auditoria'),
  cola: () => pedir('/v1/admin/cola'),
  soloLectura: (activo) => pedir(`/v1/admin/modo-solo-lectura?activo=${activo}`, { metodo: 'POST' }),
  reprocesarDlq: () => pedir('/v1/admin/cola/reprocesar-dlq', { metodo: 'POST' }),
  // cuenta
  solicitarBorrado: () => pedir('/v1/cuenta/solicitud-borrado', { metodo: 'POST' }),
};
