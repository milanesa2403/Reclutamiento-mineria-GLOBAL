import Cargando from '../../components/Cargando';
import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { AlertCircle, CheckCircle2, Clock, FileText } from 'lucide-react'
import { textoError } from '../../api/client.ts'
import {
    ETIQUETA_ESTADO,
    FORMATOS_ACREDITACION,
    MAX_BYTES_DOCUMENTO,
    obtenerMiSolicitud,
    subirDocumentoAcreditacion,
    urlMiDocumento,
    type SolicitudEmpresa as Solicitud,
} from '../../api/empresa.ts'
import { useAuth } from '../../auth/AuthContext.tsx'
import { nombreIdentificadorFiscal, nombrePais } from '../../utils/documentos.ts'

const EXTENSIONES = ['pdf', 'jpg', 'jpeg', 'png']

function formatoFecha(iso: string | null): string {
    if (!iso) return '—'
    const fecha = new Date(iso.endsWith('Z') ? iso : `${iso}Z`)
    return fecha.toLocaleString('es-PE', { dateStyle: 'medium', timeStyle: 'short' })
}

function validarArchivo(archivo: File): string | null {
    const extension = archivo.name.split('.').pop()?.toLowerCase() ?? ''
    if (!EXTENSIONES.includes(extension)) return 'Formato no permitido. Sube un PDF, JPG o PNG.'
    if (archivo.size > MAX_BYTES_DOCUMENTO) return 'El archivo supera el tamaño máximo de 5 MB.'
    if (archivo.size === 0) return 'El archivo está vacío.'
    return null
}

export default function SolicitudEmpresa() {
    const navigate = useNavigate()
    const { refrescar, cerrarSesion } = useAuth()
    const [solicitud, setSolicitud] = useState<Solicitud | null>(null)
    const [errorCarga, setErrorCarga] = useState<string | null>(null)
    const [archivo, setArchivo] = useState<File | null>(null)
    const [errorArchivo, setErrorArchivo] = useState<string | null>(null)
    const [enviando, setEnviando] = useState(false)
    const [confirmacion, setConfirmacion] = useState<string | null>(null)

    useEffect(() => {
        obtenerMiSolicitud()
            .then(setSolicitud)
            .catch((err) => setErrorCarga(textoError(err)))
    }, [])

    function elegirArchivo(e: ChangeEvent<HTMLInputElement>) {
        const elegido = e.target.files?.[0] ?? null
        setConfirmacion(null)
        setArchivo(elegido)
        setErrorArchivo(elegido ? validarArchivo(elegido) : null)
    }

    async function enviar(e: FormEvent) {
        e.preventDefault()
        if (!archivo) {
            setErrorArchivo('Selecciona el documento que vas a subir.')
            return
        }
        const problema = validarArchivo(archivo)
        if (problema) {
            setErrorArchivo(problema)
            return
        }
        setEnviando(true)
        setErrorArchivo(null)
        try {
            const actualizada = await subirDocumentoAcreditacion(archivo)
            setSolicitud(actualizada)
            setArchivo(null)
            setConfirmacion('Recibimos tu documento. Tu solicitud quedó en revisión.')
            await refrescar()
        } catch (err) {
            setErrorArchivo(textoError(err))
        } finally {
            setEnviando(false)
        }
    }

    async function verDocumento() {
        try {
            const { url } = await urlMiDocumento()
            window.open(url, '_blank', 'noopener,noreferrer')
        } catch (err) {
            setErrorArchivo(textoError(err))
        }
    }

    function salir() {
        cerrarSesion()
        navigate('/login', { replace: true })
    }

    if (errorCarga) {
        return (
            <div className="container" style={{ padding: '3rem 1rem' }}>
                <section className="form-page-container">
                    <div className="alert-message error" role="alert">
                        <AlertCircle size={18} />
                        <span>No pudimos cargar tu solicitud: {errorCarga}</span>
                    </div>
                    <button type="button" className="btn btn-outline" onClick={salir}>Cerrar sesión</button>
                </section>
            </div>
        )
    }

    if (!solicitud) {
        return <Cargando tipo="montana" mensaje="Cargando tu solicitud" />
    }

    const { estado } = solicitud

    return (
        <div className="container" style={{ padding: '3rem 1rem' }}>
            <section className="form-page-container">
                <div className="form-header-box">
                    <span className="pill-tag-center">Solicitud de registro · {ETIQUETA_ESTADO[estado]}</span>
                    <h1>{solicitud.razonSocial}</h1>
                    <p>{nombreIdentificadorFiscal(solicitud.pais)} {solicitud.ruc} · {nombrePais(solicitud.pais)}</p>
                </div>
                {estado === 'PENDIENTE_DOCUMENTO' && (
                    <div className="alert-message warning" role="status">
                        <AlertCircle size={18} />
                        <span>
                            Para activar tu empresa, sube un documento que acredite que representas a la empresa:
                            la vigencia de poder (SUNARP) o una carta de autorización firmada por el representante legal.
                        </span>
                    </div>
                )}
                {estado === 'EN_REVISION' && (
                    <div className="alert-message warning" role="status">
                        <Clock size={18} />
                        <span>
                            Tu solicitud está en revisión. El administrador verificará tu documento. Vuelve a
                            ingresar para ver si fue aprobada.
                        </span>
                    </div>
                )}
                {estado === 'OBSERVADA' && (
                    <div className="alert-message warning" role="alert">
                        <AlertCircle size={18} />
                        <span>
                            Tu solicitud tiene observaciones: <strong>“{solicitud.motivoRevision}”</strong> Corrige lo
                            indicado y sube nuevamente el documento.
                        </span>
                    </div>
                )}
                {estado === 'RECHAZADA' && (
                    <div className="alert-message error" role="alert">
                        <AlertCircle size={18} />
                        <span>
                            Tu solicitud de registro no fue aceptada. Motivo: <strong>“{solicitud.motivoRevision}”</strong>
                        </span>
                    </div>
                )}
                {estado === 'SUSPENDIDA' && (
                    <div className="alert-message warning" role="alert">
                        <AlertCircle size={18} />
                        <span>
                            Tu cuenta de empresa está <strong>suspendida</strong> temporalmente. Motivo:{' '}
                            <strong>“{solicitud.motivoRevision}”</strong> Tus convocatorias publicadas fueron cerradas.
                            Comunícate con el administrador de la plataforma para revisar tu caso.
                        </span>
                    </div>
                )}
                {estado === 'BLOQUEADA' && (
                    <div className="alert-message error" role="alert">
                        <AlertCircle size={18} />
                        <span>
                            Tu cuenta de empresa fue <strong>bloqueada</strong> de forma definitiva. Motivo:{' '}
                            <strong>“{solicitud.motivoRevision}”</strong> Los datos de la empresa, tu correo y tu documento no pueden usarse
                            para un nuevo registro.
                        </span>
                    </div>
                )}
                {confirmacion && (
                    <div className="alert-message success" role="status">
                        <CheckCircle2 size={18} />
                        <span>{confirmacion}</span>
                    </div>
                )}
                {solicitud.documentoAcreditacion && (
                    <p>
                        <FileText size={16} style={{ verticalAlign: 'middle' }} /> Documento enviado:{' '}
                        <strong>{solicitud.documentoAcreditacion.nombreOriginal}</strong>{' '}
                        <span className="muted">({formatoFecha(solicitud.documentoAcreditacion.fechaSubida)})</span>{' '}
                        <button type="button" className="btn btn-outline-sm" onClick={() => void verDocumento()}>
                            Ver documento
                        </button>
                    </p>
                )}
                {solicitud.puedeSubirDocumento && (
                    <form onSubmit={enviar} className="auth-form" noValidate>
                        <div className="form-field">
                            <label htmlFor="documento-acreditacion">
                                {solicitud.documentoAcreditacion ? 'Nuevo documento' : 'Documento de acreditación'}
                                {' '}(PDF, JPG o PNG, máximo 5 MB)
                            </label>
                            <input
                                id="documento-acreditacion"
                                type="file"
                                accept={FORMATOS_ACREDITACION}
                                onChange={elegirArchivo}
                                disabled={enviando}
                                aria-invalid={errorArchivo ? true : undefined}
                                aria-describedby={errorArchivo ? 'documento-error' : undefined}
                            />
                        </div>
                        {errorArchivo && (
                            <div id="documento-error" className="alert-message error" role="alert">
                                <AlertCircle size={18} />
                                <span>{errorArchivo}</span>
                            </div>
                        )}
                        <button type="submit" className="btn btn-primary" disabled={enviando || !archivo}>
                            {enviando ? 'Enviando documento...' : 'Enviar documento'}
                        </button>
                    </form>
                )}
                {!solicitud.puedeSubirDocumento && errorArchivo && (
                    <div className="alert-message error" role="alert">
                        <AlertCircle size={18} />
                        <span>{errorArchivo}</span>
                    </div>
                )}
                <div className="actions" style={{ marginTop: '1.5rem' }}>
                    <button type="button" className="btn btn-outline" onClick={salir}>Cerrar sesión</button>
                </div>
            </section>
        </div>
    )
}
