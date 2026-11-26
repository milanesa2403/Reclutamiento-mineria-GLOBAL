import Cargando from '../../components/Cargando';
import React, { useEffect, useState } from 'react';
import { Award, CheckCircle2, Eye, Plus, Trash2, Upload } from 'lucide-react';

import { listarTiposCertificacion, type TipoCertificacion } from '../../api/certificaciones.ts';
import { textoError } from '../../api/client.ts';
import { MAX_BYTES_DOCUMENTO } from '../../api/empresa.ts';
import {
    guardarPerfilProfesional,
    MAX_ELEMENTOS_PERFIL,
    obtenerPerfilProfesional,
    subirPdfCertificacion,
    urlPdfCertificacion,
    type CertificacionDto,
    type CertificacionGuardada,
} from '../../api/postulante.ts';

interface NuevaCertificacion {
    idCatalogoCertificacion: number | null;
    nombre: string;
    entidadEmisora: string;
    fechaEmision: string;
}

const NUEVA_VACIA: NuevaCertificacion = { idCatalogoCertificacion: null, nombre: '', entidadEmisora: '', fechaEmision: '' };

const aDto = (c: CertificacionGuardada): CertificacionDto => ({
    id: c.id,
    idCatalogoCertificacion: c.idCatalogoCertificacion,
    nombre: c.nombre,
    entidadEmisora: c.entidadEmisora,
    fechaEmision: c.fechaEmision,
});

const CertificacionesSeccion: React.FC<{ puedeEditar: boolean }> = ({ puedeEditar }) => {
    const [certificaciones, setCertificaciones] = useState<CertificacionGuardada[] | null>(null);
    const [tipos, setTipos] = useState<TipoCertificacion[]>([]);
    const [nueva, setNueva] = useState<NuevaCertificacion | null>(null);
    const [guardando, setGuardando] = useState(false);
    const [subiendo, setSubiendo] = useState<number | null>(null);
    const [error, setError] = useState('');
    const [mensaje, setMensaje] = useState('');

    useEffect(() => {
        obtenerPerfilProfesional()
            .then((p) => setCertificaciones(p.certificaciones))
            .catch((err) => { setCertificaciones([]); setError(textoError(err)); });
        listarTiposCertificacion().then(setTipos).catch(() => setTipos([]));
    }, []);

    const guardarLista = async (lista: CertificacionDto[], exito: string) => {
        setError('');
        setMensaje('');
        setGuardando(true);
        try {
            const perfil = await obtenerPerfilProfesional();
            const guardado = await guardarPerfilProfesional({
                resumenProfesional: perfil.resumenProfesional,
                especialidades: perfil.especialidades,
                habilidades: perfil.habilidades,
                experiencias: perfil.experiencias,
                formaciones: perfil.formaciones,
                certificaciones: lista,
            });
            setCertificaciones(guardado.certificaciones);
            setMensaje(exito);
            return true;
        } catch (err) {
            setError(textoError(err));
            return false;
        } finally {
            setGuardando(false);
        }
    };

    const agregar = async () => {
        if (!nueva || !certificaciones) return;
        const tipo = tipos.find((t) => t.id === nueva.idCatalogoCertificacion);
        const nombre = tipo ? tipo.nombre : nueva.nombre.trim();
        if (!nombre) { setError('Escribe el nombre de la certificación.'); return; }
        if (!nueva.entidadEmisora.trim()) { setError('Escribe la entidad que la emitió.'); return; }
        const ok = await guardarLista(
            [...certificaciones.map(aDto), {
                idCatalogoCertificacion: tipo ? tipo.id : null,
                nombre,
                entidadEmisora: nueva.entidadEmisora.trim(),
                fechaEmision: nueva.fechaEmision || null,
            }],
            `Certificación "${nombre}" agregada. Ya puedes subir su PDF.`
        );
        if (ok) setNueva(null);
    };

    const quitar = async (cert: CertificacionGuardada) => {
        if (!certificaciones || !window.confirm(`¿Quitar la certificación "${cert.nombre}" y su PDF?`)) return;
        await guardarLista(
            certificaciones.filter((c) => c.id !== cert.id).map(aDto),
            `Certificación "${cert.nombre}" quitada.`
        );
    };

    const elegir = async (cert: CertificacionGuardada, event: React.ChangeEvent<HTMLInputElement>) => {
        const archivo = event.target.files?.[0];
        event.target.value = '';
        if (!archivo) return;
        setError('');
        setMensaje('');
        if (!archivo.name.toLowerCase().endsWith('.pdf')) {
            setError('El certificado debe ser un archivo PDF.');
            return;
        }
        if (archivo.size === 0 || archivo.size > MAX_BYTES_DOCUMENTO) {
            setError(archivo.size === 0 ? 'El archivo está vacío.' : 'El archivo supera el tamaño máximo de 5 MB.');
            return;
        }
        setSubiendo(cert.id);
        try {
            const pdf = await subirPdfCertificacion(cert.id, archivo);
            setCertificaciones((actuales) => actuales?.map((c) => (c.id === cert.id ? { ...c, archivo: pdf } : c)) ?? null);
            setMensaje(`PDF de "${cert.nombre}" actualizado.`);
        } catch (err) {
            setError(textoError(err));
        } finally {
            setSubiendo(null);
        }
    };

    const ver = async (cert: CertificacionGuardada) => {
        setError('');
        try {
            const { url } = await urlPdfCertificacion(cert.id);
            window.open(url, '_blank', 'noopener,noreferrer');
        } catch (err) {
            setError(textoError(err));
        }
    };

    const ocupado = subiendo !== null || guardando;
    const ahora = new Date();
    ahora.setMinutes(ahora.getMinutes() - ahora.getTimezoneOffset());
    const hoy = ahora.toISOString().slice(0, 10);

    return (
        <section className="perfil-tarjeta">
            <div className="perfil-seccion-titulo">
                <div>
                    <span>CERTIFICACIONES</span>
                    <h2>Certificaciones</h2>
                </div>
                <Award size={21} />
            </div>
            <p className="texto-ayuda">Agrega tus certificaciones y adjunta el PDF de cada una (máximo 5 MB).</p>
            {certificaciones === null && <Cargando variante="inline" mensaje="Cargando certificaciones" />}
            {certificaciones?.length === 0 && !error && (
                <p className="texto-ayuda">Aún no registraste certificaciones.</p>
            )}

            {certificaciones?.map((c) => (
                <div className="perfil-cv" key={c.id}>
                    <div className="perfil-cv-icono"><Award size={26} /></div>
                    <div className="perfil-cv-info">
                        <span>{c.entidadEmisora.toUpperCase()}</span>
                        <strong>{c.nombre}</strong>
                        <p>{c.archivo ? `PDF adjunto: ${c.archivo.nombreOriginal}` : 'Sin PDF adjunto'}</p>
                    </div>
                    <div className="perfil-cv-acciones">
                        {c.archivo && (
                            <button type="button" className="boton-ver-cv" onClick={() => void ver(c)}
                                    disabled={ocupado}>
                                <Eye size={16} /> Ver
                            </button>
                        )}
                        {puedeEditar && (
                            <>
                                <label className="boton-cambiar-cv" aria-disabled={ocupado || undefined}>
                                    <Upload size={16} />
                                    {subiendo === c.id ? 'Subiendo...' : c.archivo ? 'Reemplazar' : 'Subir PDF'}
                                    <input type="file" accept=".pdf,application/pdf" hidden disabled={ocupado}
                                           aria-label={`PDF de ${c.nombre}`} onChange={(e) => void elegir(c, e)} />
                                </label>
                                <button type="button" className="boton-ver-cv" onClick={() => void quitar(c)}
                                        disabled={ocupado} aria-label={`Quitar ${c.nombre}`}>
                                    <Trash2 size={16} /> Quitar
                                </button>
                            </>
                        )}
                    </div>
                </div>
            ))}

            {puedeEditar && certificaciones !== null && nueva === null
                && certificaciones.length < MAX_ELEMENTOS_PERFIL && (
                <button type="button" className="boton-cambiar-cv" onClick={() => { setNueva(NUEVA_VACIA); setMensaje(''); }}
                        disabled={ocupado}>
                    <Plus size={16} /> Agregar certificación
                </button>
            )}
            {puedeEditar && nueva !== null && (
                <div className="perfil-form-grid">
                    <div className="campo-perfil">
                        <label htmlFor="cert-nueva-tipo">Certificación</label>
                        <select id="cert-nueva-tipo" value={nueva.idCatalogoCertificacion ?? ''}
                                onChange={(e) => setNueva({
                                    ...nueva,
                                    idCatalogoCertificacion: e.target.value === '' ? null : Number(e.target.value),
                                })}>
                            {tipos.map((t) => <option key={t.id} value={t.id}>{t.nombre}</option>)}
                            <option value="">Otra (escribe el nombre)</option>
                        </select>
                    </div>
                    {nueva.idCatalogoCertificacion === null && (
                        <div className="campo-perfil">
                            <label htmlFor="cert-nueva-nombre">Nombre de la certificación</label>
                            <input id="cert-nueva-nombre" maxLength={200} value={nueva.nombre}
                                   onChange={(e) => setNueva({ ...nueva, nombre: e.target.value })} />
                        </div>
                    )}
                    <div className="campo-perfil">
                        <label htmlFor="cert-nueva-entidad">Entidad emisora</label>
                        <input id="cert-nueva-entidad" maxLength={150} value={nueva.entidadEmisora}
                               onChange={(e) => setNueva({ ...nueva, entidadEmisora: e.target.value })} />
                    </div>
                    <div className="campo-perfil">
                        <label htmlFor="cert-nueva-fecha">Fecha de emisión (opcional)</label>
                        <input id="cert-nueva-fecha" type="date" max={hoy} value={nueva.fechaEmision}
                               onChange={(e) => setNueva({ ...nueva, fechaEmision: e.target.value })} />
                    </div>
                    <div className="perfil-acciones-finales">
                        <button type="button" className="boton-cancelar" onClick={() => setNueva(null)} disabled={guardando}>
                            Cancelar
                        </button>
                        <button type="button" className="boton-guardar" onClick={() => void agregar()} disabled={guardando}>
                            {guardando ? 'Guardando...' : 'Guardar certificación'}
                        </button>
                    </div>
                </div>
            )}
            {error && <p className="mensaje-error cv-mensaje" role="alert">{error}</p>}
            {mensaje && <p className="cv-nuevo-aviso" role="status"><CheckCircle2 size={15} /> {mensaje}</p>}
        </section>
    );
};

export default CertificacionesSeccion;
