import Cargando from '../../components/Cargando';
import { useEffect, useMemo, useState } from 'react'
import { textoError } from '../../api/client.ts'
import { useSearchParams } from 'react-router-dom'
import { normalizarTexto } from '../../utils/texto.ts'
import {
    aprobarEmpresa,
    detalleEmpresaAdmin,
    ETIQUETA_ESTADO,
    ETIQUETA_EVENTO,
    listarEmpresasAdmin,
    observarEmpresa,
    rechazarEmpresa,
    urlDocumentoEmpresaAdmin,
    type EmpresaAdminDetalle,
    type EmpresaAdminItem,
    type EstadoEmpresa,
} from '../../api/empresa.ts'
import { descargarCsv } from '../../utils/csv.ts'
import './AdminEmpresas.css'
import { documentoLegible, nombreIdentificadorFiscal, nombrePais } from '../../utils/documentos.ts'

const ESTADOS_REGISTRADA: EstadoEmpresa[] = ['APROBADA', 'SUSPENDIDA', 'BLOQUEADA']

const ESTADOS: EstadoEmpresa[] = [
    'EN_REVISION', 'PENDIENTE_DOCUMENTO', 'OBSERVADA', 'APROBADA', 'SUSPENDIDA', 'BLOQUEADA', 'RECHAZADA',
]

type Accion = 'aprobar' | 'observar' | 'rechazar'

function claseEstado(estado: EstadoEmpresa): string {
    if (estado === 'APROBADA') return 'activa'
    if (estado === 'RECHAZADA' || estado === 'BLOQUEADA') return 'inactiva'
    return 'pendiente'
}

function formatoFecha(iso: string | null): string {
    if (!iso) return '—'
    const [anio, mes, dia] = iso.slice(0, 10).split('-')
    return `${dia}-${mes}-${anio}`
}

function formatoFechaHora(iso: string): string {
    const fecha = new Date(/(Z|[+-]\d{2}:\d{2})$/.test(iso) ? iso : `${iso}Z`)
    return fecha.toLocaleString('es-PE', { dateStyle: 'medium', timeStyle: 'short' })
}

export default function AdminEmpresas() {
    const [empresas, setEmpresas] = useState<EmpresaAdminItem[]>([])
    const [cargando, setCargando] = useState(true)
    const [error, setError] = useState<string | null>(null)

    const [busqueda, setBusqueda] = useState('')
    const [sector, setSector] = useState('todos')
    const [desde, setDesde] = useState('')
    const [hasta, setHasta] = useState('')
    const [params] = useSearchParams()
    const estadoInicial = params.get('estado') as EstadoEmpresa | null
    const [estado, setEstado] = useState<'todos' | EstadoEmpresa>(
        estadoInicial && ESTADOS.includes(estadoInicial) ? estadoInicial : 'todos',
    )

    const [detalle, setDetalle] = useState<EmpresaAdminDetalle | null>(null)
    const [cargandoDetalle, setCargandoDetalle] = useState(false)
    const [motivo, setMotivo] = useState('')
    const [procesando, setProcesando] = useState(false)
    const [mensaje, setMensaje] = useState<{ texto: string; error: boolean } | null>(null)

    useEffect(() => {
        listarEmpresasAdmin()
            .then(setEmpresas)
            .catch((err) => setError(textoError(err)))
            .finally(() => setCargando(false))
    }, [])

    const sectoresDisponibles = useMemo(() => {
        const set = new Set(empresas.map((e) => e.sectorMinero).filter((s): s is string => Boolean(s)))
        return Array.from(set).sort()
    }, [empresas])

    const registradas = useMemo(
        () => empresas.filter((e) => ESTADOS_REGISTRADA.includes(e.estado)).length,
        [empresas],
    )

    const pendientesDeRevision = useMemo(() => empresas.filter((e) => e.estado === 'EN_REVISION').length, [empresas])

    const empresasFiltradas = useMemo(() => {
        const q = normalizarTexto(busqueda.trim())
        return empresas.filter((e) => {
            const coincideTexto =
                q === '' ||
                normalizarTexto(e.razonSocial).includes(q) ||
                e.ruc.includes(q)
            const coincideSector = sector === 'todos' || e.sectorMinero === sector
            const coincideEstado = estado === 'todos' || e.estado === estado
            const dia = e.fechaRegistro ? e.fechaRegistro.slice(0, 10) : ''
            const coincideFecha =
                (desde === '' || (dia !== '' && dia >= desde)) && (hasta === '' || (dia !== '' && dia <= hasta))
            return coincideTexto && coincideSector && coincideEstado && coincideFecha
        })
    }, [empresas, busqueda, sector, estado, desde, hasta])

    function exportarCsv() {
        descargarCsv(
            `empresas_${new Date().toISOString().slice(0, 10)}.csv`,
            ['País', 'Identificador fiscal', 'Razón social', 'Sector minero', 'Correo corporativo', 'Fecha de registro', 'Estado'],
            empresasFiltradas.map((e) => [
                nombrePais(e.pais),
                e.ruc,
                e.razonSocial,
                e.sectorMinero,
                e.correoCorporativo,
                formatoFecha(e.fechaRegistro),
                ETIQUETA_ESTADO[e.estado],
            ]),
        )
    }

    async function verDetalle(id: number) {
        setCargandoDetalle(true)
        setMensaje(null)
        setMotivo('')
        try {
            setDetalle(await detalleEmpresaAdmin(id))
        } catch (err) {
            setMensaje({ texto: textoError(err), error: true })
        } finally {
            setCargandoDetalle(false)
        }
    }

    async function verDocumento(id: number) {
        try {
            const { url } = await urlDocumentoEmpresaAdmin(id)
            window.open(url, '_blank', 'noopener,noreferrer')
        } catch (err) {
            setMensaje({ texto: textoError(err), error: true })
        }
    }

    async function decidir(accion: Accion) {
        if (!detalle) return
        const motivoLimpio = motivo.trim()
        if (accion !== 'aprobar' && !motivoLimpio) {
            setMensaje({ texto: 'Escribe el motivo: se le mostrará a la empresa.', error: true })
            return
        }
        if (motivoLimpio.length > 500) {
            setMensaje({ texto: 'El motivo no puede superar los 500 caracteres.', error: true })
            return
        }
        const confirmar: Record<Accion, string> = {
            aprobar: `¿Aprobar a ${detalle.razonSocial}? Podrá usar todas las funciones de empresa.`,
            observar: `¿Enviar la observación a ${detalle.razonSocial}? Podrá subir un documento corregido.`,
            rechazar: `¿Rechazar a ${detalle.razonSocial}? Verá el motivo en su solicitud.`,
        }
        if (!window.confirm(confirmar[accion])) return

        setProcesando(true)
        setMensaje(null)
        try {
            const actualizado =
                accion === 'aprobar' ? await aprobarEmpresa(detalle.idEmpresa)
                    : accion === 'observar' ? await observarEmpresa(detalle.idEmpresa, motivoLimpio)
                        : await rechazarEmpresa(detalle.idEmpresa, motivoLimpio)
            setDetalle(actualizado)
            setEmpresas((lista) => lista.map((e) =>
                e.idEmpresa === actualizado.idEmpresa ? { ...e, estado: actualizado.estado } : e))
            setMotivo('')
            window.dispatchEvent(new Event('empresas-actualizadas'))
            setMensaje({
                texto: `Listo: la empresa quedó "${ETIQUETA_ESTADO[actualizado.estado]}".`,
                error: false,
            })
        } catch (err) {
            setMensaje({ texto: textoError(err), error: true })
        } finally {
            setProcesando(false)
        }
    }

    if (cargando) return <Cargando tipo="circulo" mensaje="Cargando empresas" />
    if (error) return <p className="admin-empresas__error">{error}</p>

    const est = detalle?.estado
    const puedeAprobarUObservar = est === 'EN_REVISION'
    const puedeRechazar = est === 'EN_REVISION' || est === 'OBSERVADA'
    const hayAcciones = puedeAprobarUObservar || puedeRechazar

    return (
        <div className="admin-empresas">
            <h1>Empresas registradas</h1>
            <div className="admin-empresas__resumen">
                <strong>{registradas}</strong>
                <span>empresas registradas</span>
                {' · '}
                <strong>{pendientesDeRevision}</strong>
                <span>solicitudes pendientes de revisión</span>
            </div>
            <div className="admin-empresas__filtros">
                <input
                    type="text"
                    placeholder="Buscar por nombre o identificador fiscal"
                    value={busqueda}
                    onChange={(e) => setBusqueda(e.target.value)}
                />
                <select value={sector} onChange={(e) => setSector(e.target.value)}>
                    <option value="todos">Todos los sectores</option>
                    {sectoresDisponibles.map((s) => (
                        <option key={s} value={s}>{s}</option>
                    ))}
                </select>
                <select value={estado} onChange={(e) => setEstado(e.target.value as 'todos' | EstadoEmpresa)}>
                    <option value="todos">Todos los estados</option>
                    {ESTADOS.map((s) => (
                        <option key={s} value={s}>{ETIQUETA_ESTADO[s]}</option>
                    ))}
                </select>
                <label>
                    Registro desde
                    <input type="date" value={desde} max={hasta || undefined} onChange={(e) => setDesde(e.target.value)} />
                </label>
                <label>
                    hasta
                    <input type="date" value={hasta} min={desde || undefined} onChange={(e) => setHasta(e.target.value)} />
                </label>
                <button type="button" className="admin-empresas__exportar" onClick={exportarCsv}
                        disabled={empresasFiltradas.length === 0}>
                    Exportar CSV ({empresasFiltradas.length})
                </button>
            </div>
            <table className="admin-empresas__tabla">
                <thead>
                <tr>
                    <th>Empresa</th>
                    <th>País</th>
                    <th>Identificador fiscal</th>
                    <th>Sector minero</th>
                    <th>Correo corporativo</th>
                    <th>Fecha de registro</th>
                    <th>Estado</th>
                    <th>Acción</th>
                </tr>
                </thead>
                <tbody>
                {empresasFiltradas.map((e) => (
                    <tr key={e.idEmpresa}>
                        <td>{e.razonSocial}</td>
                        <td>{nombrePais(e.pais)}</td>
                        <td>{e.ruc}</td>
                        <td>{e.sectorMinero ?? '—'}</td>
                        <td>{e.correoCorporativo}</td>
                        <td>{formatoFecha(e.fechaRegistro)}</td>
                        <td>
                            <span className={`admin-empresas__estado ${claseEstado(e.estado)}`}>
                                {ETIQUETA_ESTADO[e.estado]}
                            </span>
                        </td>
                        <td>
                            <button type="button" onClick={() => void verDetalle(e.idEmpresa)} disabled={cargandoDetalle}>
                                {e.estado === 'EN_REVISION' ? 'Revisar' : 'Ver'}
                            </button>
                        </td>
                    </tr>
                ))}
                {empresasFiltradas.length === 0 && (
                    <tr><td colSpan={8}>No se encontraron empresas.</td></tr>
                )}
                </tbody>
            </table>
            {mensaje && !detalle && (
                <p className={`admin-empresas__mensaje ${mensaje.error ? 'error' : ''}`} role="alert">{mensaje.texto}</p>
            )}
            {detalle && (
                <section className="admin-empresas__detalle" aria-live="polite">
                    <h2>
                        {detalle.razonSocial}{' '}
                        <span className={`admin-empresas__estado ${claseEstado(detalle.estado)}`}>
                            {ETIQUETA_ESTADO[detalle.estado]}
                        </span>
                    </h2>
                    <dl>
                        <dt>País</dt><dd>{nombrePais(detalle.pais)}</dd>
                        <dt>{nombreIdentificadorFiscal(detalle.pais)}</dt><dd>{detalle.ruc}</dd>
                        <dt>Dirección</dt><dd>{detalle.direccion ?? '—'}</dd>
                        <dt>Sector minero</dt><dd>{detalle.sectorMinero ?? '—'}</dd>
                        <dt>Correo corporativo</dt><dd>{detalle.correoCorporativo}</dd>
                        <dt>Teléfono</dt><dd>{detalle.telefono ?? '—'}</dd>
                        <dt>Fecha de registro</dt><dd>{formatoFecha(detalle.fechaRegistro)}</dd>
                        {detalle.solicitantes.map((s) => (
                            <FilaSolicitante key={s.dni} documento={documentoLegible(s.pais, s.tipoDocumento, s.dni)} nombre={`${s.nombres} ${s.apellidos}`}
                                             cargo={s.cargo} correo={s.correo} telefono={s.telefono} />
                        ))}
                        <dt>Documento de acreditación</dt>
                        <dd>
                            {detalle.documentoAcreditacion ? (
                                <>
                                    {detalle.documentoAcreditacion.nombreOriginal}{' '}
                                    <span className="admin-empresas__acciones" style={{ display: 'inline-flex' }}>
                                        <button type="button" onClick={() => void verDocumento(detalle.idEmpresa)}>
                                            Ver documento
                                        </button>
                                    </span>
                                </>
                            ) : 'Aún no lo sube'}
                        </dd>
                        {detalle.motivoRevision && (<><dt>Último motivo</dt><dd>{detalle.motivoRevision}</dd></>)}
                        {detalle.advertencias > 0 && (<><dt>Advertencias</dt><dd>{detalle.advertencias}</dd></>)}
                        {detalle.revisadoPor && (
                            <><dt>Revisado por</dt><dd>{detalle.revisadoPor} ({formatoFecha(detalle.fechaRevision)})</dd></>
                        )}
                        {detalle.rechazosAnteriores.length > 0 && (
                            <>
                                <dt>Rechazos anteriores</dt>
                                <dd>
                                    {detalle.rechazosAnteriores.map((r) => (
                                        <div key={r.fechaRechazo}>{formatoFecha(r.fechaRechazo)}: {r.motivo}</div>
                                    ))}
                                </dd>
                            </>
                        )}
                    </dl>
                    <h3>Historial</h3>
                    {detalle.historial.length === 0 ? (
                        <p>Sin eventos registrados.</p>
                    ) : (
                        <ol className="admin-empresas__historial">
                            {detalle.historial.map((h, i) => (
                                <li key={i}>
                                    <strong>{ETIQUETA_EVENTO[h.tipo]}</strong>
                                    <span>
                                        {formatoFechaHora(h.fecha)}{h.realizadoPor ? ` · ${h.realizadoPor}` : ''}
                                    </span>
                                    {h.motivo && <em>{h.motivo}</em>}
                                </li>
                            ))}
                        </ol>
                    )}
                    {detalle.estado === 'PENDIENTE_DOCUMENTO' && <p>La empresa aún no sube su documento.</p>}
                    {detalle.estado === 'OBSERVADA' && <p>La empresa debe subir un documento corregido.</p>}
                    {hayAcciones && (
                        <>
                            <label htmlFor="motivo-decision" style={{ display: 'block', marginTop: 16 }}>
                                Motivo (obligatorio, salvo para aprobar; lo verá la empresa)
                            </label>
                            <textarea
                                id="motivo-decision"
                                maxLength={500}
                                value={motivo}
                                onChange={(e) => setMotivo(e.target.value)}
                                disabled={procesando}
                            />
                        </>
                    )}
                    {(puedeAprobarUObservar || puedeRechazar) && (
                        <div className="admin-empresas__acciones">
                            {puedeAprobarUObservar && (
                                <button type="button" className="principal" disabled={procesando}
                                        onClick={() => void decidir('aprobar')}>
                                    Aprobar
                                </button>
                            )}
                            {puedeAprobarUObservar && (
                                <button type="button" disabled={procesando} onClick={() => void decidir('observar')}>
                                    Observar
                                </button>
                            )}
                            {puedeRechazar && (
                                <button type="button" className="peligro" disabled={procesando}
                                        onClick={() => void decidir('rechazar')}>
                                    Rechazar
                                </button>
                            )}
                        </div>
                    )}
                    {mensaje && (
                        <p className={`admin-empresas__mensaje ${mensaje.error ? 'error' : ''}`} role="alert">{mensaje.texto}</p>
                    )}
                    <div className="admin-empresas__acciones" style={{ marginTop: 16 }}>
                        <button type="button" onClick={() => { setDetalle(null); setMensaje(null) }}>Cerrar</button>
                    </div>
                </section>
            )}
        </div>
    )
}

function FilaSolicitante(props: { documento: string; nombre: string; cargo: string | null; correo: string; telefono: string }) {
    return (
        <>
            <dt>Solicitante</dt>
            <dd>
                {props.nombre} · {props.documento} · {props.cargo ?? 'Sin cargo'} · {props.correo} · {props.telefono}
            </dd>
        </>
    )
}