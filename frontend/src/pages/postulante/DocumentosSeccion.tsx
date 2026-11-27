import React, { useState } from 'react';
import { CheckCircle2, Eye, FileText, Upload } from 'lucide-react';

import { textoError } from '../../api/client.ts';
import { MAX_BYTES_DOCUMENTO } from '../../api/empresa.ts';
import {
    DOCUMENTOS_POSTULANTE,
    subirMiDocumento,
    urlMiDocumento,
    type DocumentosPostulante,
    type TipoDocumentoPostulante,
} from '../../api/postulante.ts';

interface Props {
    documentos: DocumentosPostulante;
    puedeEditar: boolean;
    onCambio: (documentos: DocumentosPostulante) => void;
}

const DocumentosSeccion: React.FC<Props> = ({ documentos, puedeEditar, onCambio }) => {
    const [subiendo, setSubiendo] = useState<TipoDocumentoPostulante | null>(null);
    const [error, setError] = useState('');
    const [mensaje, setMensaje] = useState('');

    const elegir = async (tipo: TipoDocumentoPostulante, event: React.ChangeEvent<HTMLInputElement>) => {
        const archivo = event.target.files?.[0];
        event.target.value = '';
        if (!archivo) return;
        const definicion = DOCUMENTOS_POSTULANTE.find((d) => d.tipo === tipo)!;
        setError('');
        setMensaje('');
        const extension = archivo.name.split('.').pop()?.toLowerCase() ?? '';
        if (!definicion.formatos.includes(extension)) {
            setError(`${definicion.titulo}: formato no permitido. Formatos aceptados: ${nombresFormato(definicion.formatos).join(', ')}.`);
            return;
        }
        if (archivo.size === 0 || archivo.size > MAX_BYTES_DOCUMENTO) {
            setError(archivo.size === 0 ? 'El archivo está vacío.' : 'El archivo supera el tamaño máximo de 5 MB.');
            return;
        }
        setSubiendo(tipo);
        try {
            onCambio(await subirMiDocumento(tipo, archivo));
            setMensaje(`${definicion.titulo}: actualizado correctamente.`);
        } catch (err) {
            setError(textoError(err));
        } finally {
            setSubiendo(null);
        }
    };

    const ver = async (tipo: TipoDocumentoPostulante) => {
        setError('');
        try {
            const { url } = await urlMiDocumento(tipo);
            window.open(url, '_blank', 'noopener,noreferrer');
        } catch (err) {
            setError(textoError(err));
        }
    };

    return (
        <section className="perfil-tarjeta">
            <div className="perfil-seccion-titulo">
                <div>
                    <span>DOCUMENTACIÓN</span>
                    <h2>Mis documentos</h2>
                </div>
                <FileText size={21} />
            </div>
            <p className="texto-ayuda">
                Máximo 5 MB por archivo. Al subir uno nuevo, reemplaza al anterior. Solo tú puedes verlos.
            </p>

            {DOCUMENTOS_POSTULANTE.map((d) => {
                const actual = documentos[d.clave];
                return (
                    <div className="perfil-cv" key={d.tipo}>
                        <div className="perfil-cv-icono">
                            <FileText size={28} />
                        </div>
                        <div className="perfil-cv-info">
                            <span>{d.titulo.toUpperCase()}</span>
                            <strong>{actual ? `Registrado (${actual.formato})` : 'Aún no lo subiste'}</strong>
                            <p>{formatosLegibles(d.formatos)}</p>
                        </div>
                        <div className="perfil-cv-acciones">
                            {actual && (
                                <button type="button" className="boton-ver-cv" onClick={() => void ver(d.tipo)}
                                        disabled={subiendo !== null}>
                                    <Eye size={16} /> Ver
                                </button>
                            )}
                            {puedeEditar && (
                                <label className="boton-cambiar-cv" aria-disabled={subiendo !== null || undefined}>
                                    <Upload size={16} />
                                    {subiendo === d.tipo ? 'Subiendo...' : actual ? 'Reemplazar' : 'Subir'}
                                    <input type="file" accept={d.accept} hidden disabled={subiendo !== null}
                                           aria-label={`${actual ? 'Reemplazar' : 'Subir'} ${d.titulo}`}
                                           onChange={(e) => void elegir(d.tipo, e)} />
                                </label>
                            )}
                        </div>
                    </div>
                );
            })}

            {error && <p className="mensaje-error cv-mensaje" role="alert">{error}</p>}
            {mensaje && <p className="cv-nuevo-aviso" role="status"><CheckCircle2 size={15} /> {mensaje}</p>}
        </section>
    );
};

function nombresFormato(formatos: string[]): string[] {
    return [...new Set(formatos.map((f) => (f === 'jpeg' ? 'jpg' : f).toUpperCase()))];
}

export function formatosLegibles(formatos: string[]): string {
    const nombres = nombresFormato(formatos);
    return nombres.length === 1 ? `Solo ${nombres[0]}` : nombres.join(', ');
}

export default DocumentosSeccion;
