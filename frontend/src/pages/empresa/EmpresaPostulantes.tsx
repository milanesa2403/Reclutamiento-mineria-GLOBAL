import Cargando from '../../components/Cargando';
import React, { useEffect, useMemo, useState } from 'react';
import { CalendarDays, Check, ListChecks, Send, X } from 'lucide-react';
import { listarMisConvocatorias, type Convocatoria } from '../../api/convocatorias';
import {
    cambiarEstadoPostulacion,
    citarEntrevista,
    listarCandidatos,
    urlCertificadoCandidato,
    urlDocumentoCandidato,
    type Candidato,
    type EstadoPostulacion,
} from '../../api/postulaciones';
import type { TipoDocumentoPostulante } from '../../api/postulante';
import { formatearUbicacion } from '../../api/ubigeo';
import { normalizarTexto } from '../../utils/texto';
import { textoError } from '../../api/client';
import './EmpresaPostulantes.css';

interface Props {
    convocatoriaInicialId?: string | number | null;
}

function separarRequisitos(valor: unknown): string[] {
    return (typeof valor === 'string' ? valor : '')
        .split(/\r?\n/)
        .map((linea) => linea.replace(/^\s*[-•*]\s*/, '').trim())
        .filter(Boolean);
}

function mostrarFecha(valor?: string | null): string {
    if (!valor) return 'No especificada';
    const fecha = /^\d{4}-\d{2}-\d{2}$/.test(valor) ? new Date(`${valor}T12:00:00`) : new Date(valor);
    if (Number.isNaN(fecha.getTime())) return valor;
    return fecha.toLocaleDateString('es-PE', { day: '2-digit', month: '2-digit', year: 'numeric' });
}

const diaLocal = (d: Date) => new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10);

function periodo(inicio?: string | null, fin?: string | null): string {
    const mes = (valor: string) =>
        new Date(`${valor}T12:00:00`).toLocaleDateString('es-PE', { month: 'long', year: 'numeric' });
    if (!inicio) return 'Fechas no indicadas';
    return `${mes(inicio)} – ${fin ? mes(fin) : 'Actualidad'}`;
}

async function abrirEnPestana(obtenerUrl: () => Promise<string>): Promise<void> {
    const pestana = window.open('', '_blank');
    try {
        const url = await obtenerUrl();
        if (pestana) {
            pestana.opener = null;
            pestana.location.href = url;
        } else {
            window.open(url, '_blank', 'noopener');
        }
    } catch (e) {
        pestana?.close();
        throw e;
    }
}

const modalidad = (c: Convocatoria) => (c.tipo === 'PARADA_PLANTA' ? 'Parada de planta' : 'Planilla completa');
const estadoConvocatoria = (c: Convocatoria) => (
    <span className={`ep-etiqueta${c.estado === 'PUBLICADA' ? '' : ' ep-cerrada'}`}>
        {c.estado === 'PUBLICADA' ? 'Publicada' : 'Cerrada'}
    </span>
);
const nombreCompleto = (c: Candidato) => `${c.nombres} ${c.apellidos}`.trim();
const nombres = (lista: Candidato[]) => (lista.length === 1 ? nombreCompleto(lista[0]) : `${lista.length} postulantes`);
const experienciaCandidato = (c: Candidato) => `${c.aniosExperiencia} año(s)`;

const etiquetas: Record<EstadoPostulacion, string> = {
    POSTULADO: 'Nuevo',
    EN_REVISION: 'En revisión',
    ENTREVISTA: 'Entrevista',
    ACEPTADO: 'Aceptado',
    DESCARTADO: 'Descartado',
    CONFIRMADO: 'Confirmado',
    RETIRADO: 'Retirado',
};

const textoEstado = (c: Candidato) =>
    c.estado === 'ENTREVISTA' && c.entrevista
        ? `Entrevista ${mostrarFecha(c.entrevista.fecha).slice(0, 5)} ${c.entrevista.fecha.slice(11, 16)}`
        : etiquetas[c.estado] + (c.motivoRetiro ? ': ' + c.motivoRetiro : '');

const puedeCitar = (c: Candidato) => ['POSTULADO', 'EN_REVISION', 'ENTREVISTA'].includes(c.estado);

const ENTREVISTA_VACIA = { fecha: '', hora: '', modalidad: 'PRESENCIAL' as 'PRESENCIAL' | 'VIRTUAL', lugar: '', mensaje: '' };

const FILTROS_ESTADO: Array<[EstadoPostulacion, string]> = [
    ['POSTULADO', 'Nuevos'],
    ['EN_REVISION', 'En revisión'],
    ['ENTREVISTA', 'Entrevista'],
    ['ACEPTADO', 'Aceptados'],
    ['CONFIRMADO', 'Confirmados'],
    ['DESCARTADO', 'Descartados'],
    ['RETIRADO', 'Retirados'],
];

const DOCUMENTOS: Array<{ tipo: TipoDocumentoPostulante; clave: 'cv' | 'fotoDni' | 'cul' | 'emo'; nombre: string }> = [
    { tipo: 'cv', clave: 'cv', nombre: 'Currículum (CV)' },
    { tipo: 'foto-dni', clave: 'fotoDni', nombre: 'DNI' },
    { tipo: 'cul', clave: 'cul', nombre: 'Certificado Único Laboral (CUL)' },
    { tipo: 'emo', clave: 'emo', nombre: 'Examen médico ocupacional (EMO)' },
];

const EmpresaPostulantes: React.FC<Props> = ({ convocatoriaInicialId = null }) => {
    const [convocatorias, setConvocatorias] = useState<Convocatoria[]>([]);
    const [idSeleccionado, setIdSeleccionado] = useState<string | null>(() => {
        const id = convocatoriaInicialId ?? new URLSearchParams(window.location.search).get('convocatoria');
        return id === null ? null : String(id);
    });
    const [postulantes, setPostulantes] = useState<Candidato[]>([]);
    const [perfil, setPerfil] = useState<Candidato | null>(null);
    const [busqueda, setBusqueda] = useState('');
    const [buscarConvocatoria, setBuscarConvocatoria] = useState('');
    const [filtro, setFiltro] = useState<'TODOS' | EstadoPostulacion>('TODOS');
    const [experienciaMinima, setExperienciaMinima] = useState('');
    const [cargando, setCargando] = useState(true);
    const [cargandoPostulantes, setCargandoPostulantes] = useState(false);
    const [citando, setCitando] = useState<Candidato[]>([]);
    const [seleccion, setSeleccion] = useState<number[]>([]);
    const [seleccionando, setSeleccionando] = useState(false);
    const [confirmacion, setConfirmacion] = useState<{ candidatos: Candidato[]; estado: 'ACEPTADO' | 'DESCARTADO' } | null>(null);
    const [entrevista, setEntrevista] = useState(ENTREVISTA_VACIA);
    const [errores, setErrores] = useState<{ fecha?: string; hora?: string }>({});
    const [errorEntrevista, setErrorEntrevista] = useState('');
    const [error, setError] = useState('');
    const [version, setVersion] = useState(0);
    const [abriendo, setAbriendo] = useState<string | null>(null);
    const [verDetalles, setVerDetalles] = useState(false);

    useEffect(() => {
        if (convocatoriaInicialId !== null) setIdSeleccionado(String(convocatoriaInicialId));
    }, [convocatoriaInicialId]);

    const abrir = async (clave: string, obtenerUrl: () => Promise<string>) => {
        setAbriendo(clave);
        setError('');
        try {
            await abrirEnPestana(obtenerUrl);
        } catch (e) {
            setError(textoError(e));
        } finally {
            setAbriendo(null);
        }
    };

    useEffect(() => {
        let activo = true;
        setCargando(true);
        setError('');
        listarMisConvocatorias()
            .then((datos) => activo && setConvocatorias(datos.filter((c) => c.estado !== 'BORRADOR')))
            .catch((e) => activo && setError(`No se pudieron cargar las convocatorias: ${textoError(e)}`))
            .finally(() => activo && setCargando(false));
        return () => {
            activo = false;
        };
    }, [version]);

    const convocatoriasFiltradas = useMemo(() => {
        const termino = normalizarTexto(buscarConvocatoria.trim());
        return convocatorias.filter((c) => normalizarTexto(c.puesto).includes(termino));
    }, [convocatorias, buscarConvocatoria]);

    const convocatoria = useMemo(
        () => convocatorias.find((item) => String(item.id) === idSeleccionado),
        [convocatorias, idSeleccionado]
    );

    useEffect(() => {
        let activo = true;
        if (!convocatoria) {
            setPostulantes([]);
            setCargandoPostulantes(false);
            return;
        }
        setCargandoPostulantes(true);
        setError('');
        listarCandidatos(convocatoria.id)
            .then((datos) => activo && setPostulantes(datos))
            .catch((e) => {
                if (!activo) return;
                setPostulantes([]);
                setError(`No se pudieron cargar los postulantes: ${textoError(e)}`);
            })
            .finally(() => activo && setCargandoPostulantes(false));
        return () => {
            activo = false;
        };
    }, [convocatoria, version]);

    const seleccionarConvocatoria = (id: number | null) => {
        const nuevoId = id === null ? null : String(id);
        setIdSeleccionado(nuevoId);
        setSeleccion([]);
        setSeleccionando(false);
        setVerDetalles(false);
        setPerfil(null);
        setBusqueda('');
        setFiltro('TODOS');
        setExperienciaMinima('');
        setError('');
        const url = new URL(window.location.href);
        if (nuevoId) {
            url.searchParams.set('modulo', 'postulantes');
            url.searchParams.set('convocatoria', nuevoId);
        } else {
            url.searchParams.delete('convocatoria');
        }
        window.history.replaceState({}, '', url.toString());
    };

    const filtrados = useMemo(() => {
        const termino = normalizarTexto(busqueda.trim());
        return postulantes.filter((item) =>
            (normalizarTexto(nombreCompleto(item)).includes(termino) ||
                normalizarTexto(item.correo || '').includes(termino)) &&
            (experienciaMinima === '' || item.aniosExperiencia >= Number(experienciaMinima)) &&
            (filtro === 'TODOS' || item.estado === filtro));
    }, [postulantes, busqueda, filtro, experienciaMinima]);

    const contar = (estado: EstadoPostulacion) => postulantes.filter((item) => item.estado === estado).length;

    const abrirPerfil = async (item: Candidato) => {
        setPerfil(item);
        if (item.estado !== 'POSTULADO') return;
        try {
            await cambiarEstadoPostulacion(item.idPostulacion, 'EN_REVISION');
            const revisado = { ...item, estado: 'EN_REVISION' as EstadoPostulacion };
            setPostulantes((actuales) => actuales.map((p) => (p.idPostulacion === item.idPostulacion ? revisado : p)));
            setPerfil(revisado);
        } catch (e) {
            setError(textoError(e));
        }
    };

    const terminarSeleccion = () => {
        setSeleccion([]);
        setSeleccionando(false);
        setPerfil(null);
        setVersion((v) => v + 1);
    };

    const confirmarEstado = async () => {
        if (!confirmacion) return;
        setError('');
        try {
            await Promise.all(confirmacion.candidatos.map((c) => cambiarEstadoPostulacion(c.idPostulacion, confirmacion.estado)));
            terminarSeleccion();
        } catch (e) {
            setError(`No se pudo guardar el nuevo estado: ${textoError(e)}`);
        }
        setConfirmacion(null);
    };

    const abrirCitacion = (candidatos: Candidato[]) => {
        const e = candidatos.length === 1 ? candidatos[0].entrevista : null;
        setCitando(candidatos);
        setErrores({});
        setErrorEntrevista('');
        setEntrevista(e ? { fecha: e.fecha.slice(0, 10), hora: e.fecha.slice(11, 16), modalidad: e.modalidad, lugar: e.lugar,
            mensaje: e.mensaje ?? '' } : ENTREVISTA_VACIA);
    };

    const seleccionados = postulantes.filter((c) => seleccion.includes(c.idPostulacion));
    const marcables = filtrados.filter(puedeCitar).map((c) => c.idPostulacion);
    const alternarSeleccion = (id: number) =>
        setSeleccion((actual) => (actual.includes(id) ? actual.filter((x) => x !== id) : [...actual, id]));
    const hoy = diaLocal(new Date());
    const limite = diaLocal(new Date(Date.now() + 365 * 86400000));
    const editar = (campo: keyof typeof ENTREVISTA_VACIA, valor: string) => {
        setEntrevista({ ...entrevista, [campo]: valor });
        setErrores({ ...errores, [campo]: undefined });
    };

    const enviarEntrevista = async () => {
        const { fecha, hora } = entrevista;
        const nuevos = {
            fecha: fecha.length !== 10 || fecha < hoy || fecha > limite
                ? 'Elige una fecha desde hoy hasta dentro de un año.' : undefined,
            hora: !hora ? 'Indica la hora de la entrevista.'
                : fecha === hoy && hora <= new Date().toTimeString().slice(0, 5) ? 'Elige una hora posterior a la actual.' : undefined,
        };
        setErrores(nuevos);
        if (nuevos.fecha || nuevos.hora) return;
        try {
            await citarEntrevista(citando.map((c) => c.idPostulacion), {
                fecha: `${fecha}T${hora}`,
                modalidad: entrevista.modalidad,
                lugar: entrevista.lugar,
                mensaje: entrevista.mensaje || null,
            });
            setCitando([]);
            terminarSeleccion();
        } catch (e) {
            setErrorEntrevista(textoError(e));
        }
    };

    const botonVer = (clave: string, texto: string, obtenerUrl: () => Promise<string>) => (
        <button
            type="button"
            className="ep-enlace"
            disabled={abriendo !== null}
            onClick={() => void abrir(clave, obtenerUrl)}
        >
            {abriendo === clave ? 'Abriendo...' : texto}
        </button>
    );

    const urlCertificado = (idPostulacion: number, idCertificacion: number) => async () =>
        (await urlCertificadoCandidato(idPostulacion, idCertificacion)).url;

    const resumenOferta = (c: Convocatoria) => [
        modalidad(c),
        c.turno && `Turno ${c.turno}`,
        c.experienciaMinimaAnios !== null && `Exp. mín. ${c.experienciaMinimaAnios} año(s)`,
        `Cierre ${mostrarFecha(c.fechaCierre)}`,
        c.fechaInicioLabores && `Labores ${mostrarFecha(c.fechaInicioLabores)} – ${mostrarFecha(c.fechaFinLabores)}`,
    ].filter(Boolean).join(' · ');

    const detallesOferta = (c: Convocatoria): Array<[string, string[]]> => [
        ['Ubicación', [[formatearUbicacion(c.ubicacion), c.campamento, c.direccion].filter(Boolean).join(' · ')]],
        ['Descripción', [c.descripcion || 'No especificada']],
        ['Requisitos obligatorios', separarRequisitos(c.requisitosObligatorios)],
        ['Requisitos deseables', separarRequisitos(c.requisitosDeseables)],
        ['Certificados exigidos', c.certificadosRequeridos.map((r) => (r.requierePdf ? `${r.nombre} (PDF obligatorio)` : r.nombre))],
    ];

    return (
        <div className={`empresa-postulantes${perfil ? ' ep-con-perfil' : ''}`}>
            {error && <p role="alert" className="ep-alerta">{error}</p>}
            {!idSeleccionado && <h1 className="ep-titulo">Gestión de postulantes</h1>}
            {!idSeleccionado && (
                <section className="ep-seccion">
                    <div className="ep-herramientas">
                        <input
                            type="search"
                            placeholder="Buscar convocatoria por nombre"
                            value={buscarConvocatoria}
                            onChange={(e) => setBuscarConvocatoria(e.target.value)}
                        />
                    </div>
                    {cargando ? (
                        <Cargando tipo="circulo" mensaje="Cargando convocatorias" />
                    ) : (
                        <div className="ep-lista">
                            {convocatoriasFiltradas.map((item) => (
                                <button type="button" className="ep-fila" key={String(item.id)} onClick={() => seleccionarConvocatoria(item.id)}>
                                    <span>
                                        <strong>{item.puesto}</strong>
                                        <small>
                                            {modalidad(item)} · {formatearUbicacion(item.ubicacion) || 'Ubicación no especificada'}                                        </small>
                                    </span>
                                    <small><b>{item.totalPostulaciones ?? 0}</b> postulante(s) · {item.vacantes ?? 1} vacante(s) · <b>Cierre {mostrarFecha(item.fechaCierre)}</b></small>
                                    {estadoConvocatoria(item)}
                                </button>
                            ))}

                            {convocatoriasFiltradas.length === 0 && (
                                <p>No hay convocatorias publicadas o cerradas para mostrar.</p>
                            )}
                        </div>
                    )}
                </section>
            )}
            {idSeleccionado && (
                <section className="ep-seccion">
                    <button type="button" className="ep-volver" onClick={() => seleccionarConvocatoria(null)}>
                        ← Convocatorias
                    </button>
                    {cargando ? (
                        <Cargando tipo="circulo" mensaje="Cargando convocatoria" />
                    ) : !convocatoria ? (
                        <p>No se encontró la convocatoria seleccionada.</p>
                    ) : (
                        <>
                            <div className="ep-resumen">
                                <h2>{convocatoria.puesto} {estadoConvocatoria(convocatoria)}</h2>                                <p>{resumenOferta(convocatoria)}</p>
                                {verDetalles && (
                                    <div className="ep-detalles">
                                        {detallesOferta(convocatoria).map(([titulo, lineas]) => (
                                            <div key={titulo}>
                                                <h3>{titulo}</h3>
                                                <p>{lineas.join('\n') || 'No especificado'}</p>
                                            </div>
                                        ))}
                                    </div>
                                )}
                                <button type="button" className="ep-volver" onClick={() => setVerDetalles(!verDetalles)}>
                                    {verDetalles ? 'Ocultar detalles ▴' : 'Ver detalles ▾'}
                                </button>
                            </div>
                            <div className="ep-pastillas">
                                <strong>{postulantes.length} postulante(s) · {convocatoria.vacantes ?? 1} vacante(s)</strong>
                                {FILTROS_ESTADO.map(([estado, texto]) => (
                                    <button type="button" key={estado} className={filtro === estado ? 'activa' : ''}
                                        onClick={() => setFiltro(filtro === estado ? 'TODOS' : estado)}>
                                        {texto} {contar(estado)}
                                    </button>
                                ))}
                            </div>
                            <div className="ep-herramientas ep-filtros">
                                <input
                                    type="search"
                                    placeholder="Buscar por nombre o correo"
                                    value={busqueda}
                                    onChange={(e) => setBusqueda(e.target.value)}
                                />
                                <select
                                    value={experienciaMinima}
                                    onChange={(e) => setExperienciaMinima(e.target.value)}
                                    aria-label="Experiencia mínima"
                                >
                                    <option value="">Cualquier experiencia</option>
                                    {[1, 3, 5, 10].map((n) => (
                                        <option key={n} value={n}>{`${n} ${n === 1 ? 'año' : 'años'} o más`}</option>
                                    ))}
                                </select>
                                {seleccionando ? (
                                    <span className="ep-masivo">
                                        <button type="button" className="ep-boton" disabled={!seleccionados.length}
                                            onClick={() => abrirCitacion(seleccionados)}>
                                            <Send size={16} /> Citar ({seleccionados.length})
                                        </button>
                                        <button type="button" className="ep-boton ep-verde" disabled={!seleccionados.length}
                                            onClick={() => setConfirmacion({ candidatos: seleccionados, estado: 'ACEPTADO' })}>
                                            <Check size={16} /> Aceptar ({seleccionados.length})
                                        </button>
                                        <button type="button" className="ep-boton-outline" onClick={() => { setSeleccion([]); setSeleccionando(false); }}>
                                            Cancelar
                                        </button>
                                    </span>
                                ) : (
                                    <button type="button" className="ep-boton" onClick={() => setSeleccionando(true)}>
                                        <ListChecks size={16} /> Seleccionar varios
                                    </button>
                                )}
                            </div>
                            {cargandoPostulantes ? (
                                <Cargando tipo="circulo" mensaje="Cargando postulantes" />
                            ) : (
                                <div className="ep-tabla-contenedor">
                                    <table className="ep-tabla">
                                        <thead>
                                            <tr>
                                                {['Postulante', 'Experiencia', 'Requisitos', 'Estado'].map((t) => <th key={t}>{t}</th>)}
                                                <th>
                                                    {seleccionando ? (
                                                        <label className="ep-todos">
                                                            <input type="checkbox" checked={marcables.length > 0 && marcables.every((id) => seleccion.includes(id))}
                                                                onChange={(e) => setSeleccion(e.target.checked ? marcables : [])} /> Todos
                                                        </label>
                                                    ) : 'Acción'}
                                                </th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {filtrados.map((item) => {
                                                const reqs = item.requisitosCubiertos;
                                                const cumple = reqs.filter((r) => r.coincide).length;
                                                return (
                                                    <tr key={String(item.idPostulacion)}>
                                                        <td>
                                                            <strong>{nombreCompleto(item)}</strong>
                                                            <small>{item.especialidadPrincipal || 'Especialidad no indicada'}</small>
                                                        </td>
                                                        <td>{experienciaCandidato(item)}</td>
                                                        <td>
                                                            <span className={`ep-estado${cumple === reqs.length && item.documentos.cv ? '' : ' ep-en_revision'}`}>
                                                                {(reqs.length ? `Cumple ${cumple}/${reqs.length}` : `${item.certificaciones.length} cert.`) +
                                                                    (item.documentos.cv ? ' · CV' : ' · sin CV')}
                                                            </span>
                                                        </td>
                                                        <td><span className={`ep-estado ep-${item.estado.toLowerCase()}`}>{textoEstado(item)}</span></td>
                                                        <td className="ep-acciones">
                                                            <button type="button" className="ep-boton-outline" onClick={() => void abrirPerfil(item)}>
                                                                Ver perfil
                                                            </button>
                                                            {seleccionando && puedeCitar(item) && (
                                                                <input type="checkbox" aria-label={`Seleccionar a ${nombreCompleto(item)}`}
                                                                    checked={seleccion.includes(item.idPostulacion)}
                                                                    onChange={() => alternarSeleccion(item.idPostulacion)} />
                                                            )}
                                                        </td>
                                                    </tr>
                                                );
                                            })}
                                        </tbody>
                                    </table>
                                    {filtrados.length === 0 && (
                                        <p>
                                            {postulantes.length === 0
                                                ? 'Esta convocatoria aún no tiene postulaciones.'
                                                : 'No hay resultados para los filtros seleccionados.'}
                                        </p>
                                    )}
                                </div>
                            )}
                        </>
                    )}
                </section>
            )}
            {confirmacion && (
                <div className="ep-overlay" onClick={() => setConfirmacion(null)}>
                    <div className="ep-modal ep-confirmar" role="dialog" aria-modal="true" onClick={(e) => e.stopPropagation()}>
                        <h2>¿{confirmacion.estado === 'ACEPTADO' ? 'Aceptar' : 'Descartar'} a {nombres(confirmacion.candidatos)}?</h2>
                        <p>
                            {confirmacion.estado === 'ACEPTADO'
                                ? 'Tendrá(n) 3 días para confirmar su participación.'
                                : confirmacion.candidatos[0].estado === 'CONFIRMADO'
                                    ? '¿Ya lo conversaste con el postulante? Pasará a Descartado y se liberará la vacante.'
                                    : 'Ya no seguirá(n) en esta convocatoria.'}
                        </p>
                        <div>
                            <button type="button" className="ep-boton-outline" onClick={() => setConfirmacion(null)}>Cancelar</button>
                            <button type="button" className={`ep-boton ${confirmacion.estado === 'ACEPTADO' ? 'ep-verde' : 'ep-rojo'}`}
                                onClick={() => void confirmarEstado()}>
                                {confirmacion.estado === 'ACEPTADO' ? 'Aceptar' : 'Descartar'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
            {perfil && (
                <aside className="ep-modal ep-sidebar" aria-label="Perfil del postulante">
                        <button
                            type="button"
                            className="ep-cerrar"
                            onClick={() => setPerfil(null)}
                            aria-label="Cerrar perfil"
                        >
                            ✕
                        </button>
                        <h2>Perfil de {nombreCompleto(perfil)}</h2>
                        <p>
                            {perfil.correo || 'Correo no disponible'} · {perfil.telefono || 'Sin teléfono'} ·{' '}
                            {experienciaCandidato(perfil)} de experiencia
                        </p>
                        <div className="ep-bloque">
                            <h3>Resumen profesional</h3>
                            <p>{perfil.resumenProfesional || 'No registrado.'}</p>
                        </div>
                        <div className="ep-bloque">
                            <h3>Especialidades</h3>
                            {perfil.especialidades.length > 0 ? (
                                <ul>
                                    {perfil.especialidades.map((e) => (
                                        <li key={e.nombre}>
                                            {e.nombre}
                                            {e.principal && <strong> (principal)</strong>}
                                        </li>
                                    ))}
                                </ul>
                            ) : (
                                <p>No registradas.</p>
                            )}
                        </div>
                        <div className="ep-bloque">
                            <h3>Habilidades</h3>
                            <p>{perfil.habilidades.length > 0 ? perfil.habilidades.join(', ') : 'No registradas.'}</p>
                        </div>
                        <div className="ep-bloque">
                            <h3>Experiencia laboral</h3>
                            {perfil.experiencias.length > 0 ? (
                                <ul>
                                    {perfil.experiencias.map((x, i) => (
                                        <li key={i}>
                                            <strong>{x.puesto}</strong> — {x.empresa}
                                            <br />
                                            <small>{periodo(x.fechaInicio, x.fechaFin)}</small>
                                            {x.descripcion && <p>{x.descripcion}</p>}
                                        </li>
                                    ))}
                                </ul>
                            ) : (
                                <p>No registrada.</p>
                            )}
                        </div>
                        <div className="ep-bloque">
                            <h3>Formación académica</h3>
                            {perfil.formaciones.length > 0 ? (
                                <ul>
                                    {perfil.formaciones.map((f, i) => (
                                        <li key={i}>
                                            <strong>{f.carreraGrado}</strong> — {f.institucion}
                                            <br />
                                            <small>{periodo(f.fechaInicio, f.fechaFin)}</small>
                                        </li>
                                    ))}
                                </ul>
                            ) : (
                                <p>No registrada.</p>
                            )}
                        </div>
                        <div className="ep-bloque">
                            <h3>Documentos</h3>
                            <ul className="ep-documentos">
                                {DOCUMENTOS.map((d) => {
                                    const doc = perfil.documentos[d.clave];
                                    return (
                                        <li key={d.tipo}>
                                            <span>{d.nombre}</span>
                                            {doc
                                                ? botonVer(`doc-${d.tipo}`, `Ver (${doc.formato})`, async () =>
                                                    (await urlDocumentoCandidato(perfil.idPostulacion, d.tipo)).url)
                                                : <em>No subido</em>}
                                        </li>
                                    );
                                })}
                            </ul>
                        </div>
                        {perfil.requisitosCubiertos.length > 0 && (
                            <div className="ep-bloque">
                                <h3>
                                    Certificados exigidos (cumple {perfil.requisitosCubiertos.filter((r) => r.coincide).length} de{' '}
                                    {perfil.requisitosCubiertos.length})
                                </h3>
                                <ul className="ep-documentos">
                                    {perfil.requisitosCubiertos.map((r) => {
                                        const clave = `req-${r.idCertificacion}-${r.requisito}`;
                                        return (
                                            <li key={clave}>
                                                <span>
                                                    <strong>{r.requisito}</strong>
                                                    {' → '}
                                                    {r.certificacion}
                                                    {!r.coincide && <em> (por revisar)</em>}
                                                </span>
                                                {r.tienePdf
                                                    ? botonVer(clave, 'Ver PDF', urlCertificado(perfil.idPostulacion, r.idCertificacion))
                                                    : <em>Sin PDF</em>}
                                            </li>
                                        );
                                    })}
                                </ul>
                            </div>
                        )}
                        {perfil.certificaciones.length > 0 && (
                            <div className="ep-bloque">
                                <h3>Certificaciones adjuntas</h3>
                                <ul className="ep-documentos">
                                    {perfil.certificaciones.map((c) => (
                                        <li key={c.idCertificacion}>
                                            <span>
                                                {c.nombre}
                                                {c.entidadEmisora ? ` — ${c.entidadEmisora}` : ''}
                                            </span>
                                            {c.tienePdf
                                                ? botonVer(`cert-${c.idCertificacion}`, 'Ver PDF',
                                                    urlCertificado(perfil.idPostulacion, c.idCertificacion))
                                                : <em>Sin PDF</em>}
                                        </li>
                                    ))}
                                </ul>
                            </div>
                        )}
                        <div className="ep-sidebar-acciones">
                            {puedeCitar(perfil) && (
                                <>
                                    <button type="button" className="ep-boton-outline" onClick={() => abrirCitacion([perfil])}>
                                        <CalendarDays size={16} /> Citar
                                    </button>
                                    <button type="button" className="ep-boton ep-verde"
                                        onClick={() => setConfirmacion({ candidatos: [perfil], estado: 'ACEPTADO' })}>
                                        <Check size={16} /> Aceptar
                                    </button>
                                </>
                            )}
                            {(puedeCitar(perfil) || perfil.estado === 'ACEPTADO' || perfil.estado === 'CONFIRMADO') && (
                                <button type="button" className="ep-boton ep-rojo"
                                    onClick={() => setConfirmacion({ candidatos: [perfil], estado: 'DESCARTADO' })}>
                                    <X size={16} /> Descartar
                                </button>
                            )}
                        </div>
                </aside>
            )}
            {citando.length > 0 && (
                <div className="ep-overlay" onClick={() => setCitando([])}>
                    <form
                        noValidate
                        className="ep-modal"
                        role="dialog"
                        aria-modal="true"
                        aria-label="Citar a entrevista"
                        onClick={(e) => e.stopPropagation()}
                        onSubmit={(e) => {
                            e.preventDefault();
                            void enviarEntrevista();
                        }}
                    >
                        <button type="button" className="ep-cerrar" onClick={() => setCitando([])} aria-label="Cerrar">✕</button>
                        <h2>Citar a entrevista a {nombres(citando)}</h2>
                        <div className="ep-formulario">
                            <label>Fecha<input type="date" value={entrevista.fecha} min={hoy} max={limite}
                                onChange={(e) => editar('fecha', e.target.value)} />
                                {errores.fecha && <small className="ep-error">{errores.fecha}</small>}</label>
                            <label>Hora<input type="time" value={entrevista.hora}
                                onChange={(e) => editar('hora', e.target.value)} />
                                {errores.hora && <small className="ep-error">{errores.hora}</small>}</label>
                            <label>
                                Modalidad
                                <select value={entrevista.modalidad} onChange={(e) => editar('modalidad', e.target.value)}>
                                    <option value="PRESENCIAL">Presencial</option>
                                    <option value="VIRTUAL">Virtual</option>
                                </select>
                            </label>
                            <label>
                                {entrevista.modalidad === 'VIRTUAL' ? 'Enlace' : 'Lugar'}
                                <input value={entrevista.lugar} maxLength={255} onChange={(e) => editar('lugar', e.target.value)} />
                            </label>
                            <label>
                                Mensaje (opcional)
                                <textarea rows={3} maxLength={500} value={entrevista.mensaje}
                                    onChange={(e) => editar('mensaje', e.target.value)} />
                            </label>
                        </div>
                        {errorEntrevista && <p role="alert" className="ep-alerta">{errorEntrevista}</p>}
                        <button type="submit" className="ep-boton"><Send size={16} /> Enviar citación</button>
                    </form>
                </div>
            )}
        </div>
    );
};

export default EmpresaPostulantes;
