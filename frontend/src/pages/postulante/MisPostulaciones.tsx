import Cargando from '../../components/Cargando';
import React, { useEffect, useMemo, useState } from 'react';
import { Building2, CalendarDays, ClipboardList, Clock3, Factory, History, MapPin, MessageSquare, X } from 'lucide-react';

import {
    listarMisPostulaciones,
    confirmarPostulacion,
    retirarPostulacion,
    marcarPostulacionesVistas,
    type EstadoPostulacion,
    type MiPostulacion,
} from '../../api/postulaciones';
import { formatearUbicacion } from '../../api/ubigeo';
import { textoError } from '../../api/client';
import AvisoSoporte from '../../components/AvisoSoporte';
import './MisPostulaciones.css';

const enProceso = (estado: EstadoPostulacion) => estado === 'POSTULADO' || estado === 'EN_REVISION' || estado === 'ENTREVISTA';

type FiltroPostulacion = 'TODAS' | 'ACTIVAS' | 'FINALIZADAS';

const ESTADOS: Record<EstadoPostulacion, { etiqueta: string; descripcion: string }> = {
    POSTULADO: { etiqueta: 'Enviada', descripcion: 'Tu postulación fue enviada. La empresa aún no la revisa.' },
    EN_REVISION: { etiqueta: 'En revisión', descripcion: 'La empresa está evaluando tu postulación.' },
    ENTREVISTA: { etiqueta: 'Entrevista', descripcion: 'Te citaron a entrevista.' },
    ACEPTADO: { etiqueta: 'Aceptado', descripcion: 'Tu postulación fue aceptada por la empresa reclutadora.' },
    DESCARTADO: { etiqueta: 'Descartado', descripcion: 'La empresa no continuará con tu postulación.' },
    CONFIRMADO: { etiqueta: 'Confirmado', descripcion: 'Confirmaste esta oferta laboral.' },
    RETIRADO: { etiqueta: 'Retirado', descripcion: 'Te retiraste de este proceso.' },
};

const obtenerEstado = (estado: EstadoPostulacion) => ESTADOS[estado].etiqueta;

const obtenerDescripcion = (estado: EstadoPostulacion, motivo?: string | null) =>
    estado === 'RETIRADO' ? motivo ?? ESTADOS.RETIRADO.descripcion : ESTADOS[estado].descripcion;

const fechaCorta = (fecha: string | null, opciones?: Intl.DateTimeFormatOptions) =>
    fecha ? new Date(fecha.length === 10 ? `${fecha}T12:00:00` : fecha).toLocaleDateString('es-PE', opciones ?? { day: '2-digit', month: '2-digit', year: 'numeric' }) : 'No disponible';

const logo = (p: MiPostulacion) => (
    <span className="mis-postulaciones-logo">{p.logoUrl ? <img src={p.logoUrl} alt="" /> : <Building2 size={20} />}</span>
);

const aviso = (p: MiPostulacion) =>
    p.estado === 'ACEPTADO' && p.fechaLimiteConfirmacion
        ? `¡Fuiste aceptado! Confirma antes del ${fechaCorta(p.fechaLimiteConfirmacion)}`
        : obtenerDescripcion(p.estado, p.motivoRetiro);

const PREGUNTAS = {
    confirmar: '¿Confirmas que aceptas esta oferta?',
    rechazar: '¿Seguro que deseas rechazar esta oferta? Tu postulación pasará a "Retirado".',
    retirar: '¿Seguro que deseas retirarte de este proceso? Tu postulación pasará a "Retirado".',
};

type Accion = keyof typeof PREGUNTAS;

interface MisPostulacionesProps {
    correoPostulante?: string;
}

const MisPostulaciones: React.FC<MisPostulacionesProps> = () => {
    const [postulaciones, setPostulaciones] = useState<MiPostulacion[]>([]);
    const [cargando, setCargando] = useState(true);
    const [error, setError] = useState('');
    const [filtro, setFiltro] = useState<FiltroPostulacion>('TODAS');
    const [seleccionada, setSeleccionada] = useState<MiPostulacion | null>(null);
    const [pregunta, setPregunta] = useState<Accion | null>(null);

    const responder = async (p: MiPostulacion, accion: Accion) => {
        setPregunta(null);
        try {
            await (accion === 'confirmar' ? confirmarPostulacion(p.idPostulacion) : retirarPostulacion(p.idPostulacion));
            setPostulaciones((await listarMisPostulaciones()));
            setSeleccionada(null);
        } catch (e) {
            setError(textoError(e));
        }
    };

    const confirmacion = (p: MiPostulacion, accion: Accion) => (
        <>
            <span>{PREGUNTAS[accion] + (accion !== 'confirmar' ? '' : p.tipo === 'PARADA_PLANTA'
                ? ' Tus otras paradas que se crucen en fechas se retirarán.' : ' Tus demás postulaciones seguirán activas.')}</span>
            <div>
                <button type="button" className={`mis-postulaciones-${accion === 'confirmar' ? 'confirmar' : 'rechazar'}`}
                    onClick={() => responder(p, accion)}>Sí, {accion}</button>
                <button type="button" onClick={() => setPregunta(null)}>Cancelar</button>
            </div>
        </>
    );

    useEffect(() => {
        let activo = true;
        listarMisPostulaciones()
            .then((lista) => {
                if (activo) setPostulaciones(lista);
                if (activo && lista.some((p) => !p.visto)) marcarPostulacionesVistas().catch(() => undefined);
            })
            .catch((e) => activo && setError(textoError(e)))
            .finally(() => activo && setCargando(false));
        return () => {
            activo = false;
        };
    }, []);

    useEffect(() => {
        if (!seleccionada) return;
        const cerrarConEscape = (event: KeyboardEvent) => {
            if (event.key === 'Escape') setSeleccionada(null);
        };
        window.addEventListener('keydown', cerrarConEscape);
        return () => window.removeEventListener('keydown', cerrarConEscape);
    }, [seleccionada]);

    const total = postulaciones.length;
    const enRevision = postulaciones.filter((item) => enProceso(item.estado)).length;
    const finalizadas = total - enRevision;

    const postulacionesFiltradas = useMemo(
        () => postulaciones.filter((item) =>
            filtro === 'TODAS' || (filtro === 'ACTIVAS') === enProceso(item.estado)),
        [postulaciones, filtro]
    );

    const filtros = [
        { valor: 'TODAS' as const, Icono: ClipboardList, texto: 'Todas', cantidad: total },
        { valor: 'ACTIVAS' as const, Icono: Clock3, texto: 'En proceso', cantidad: enRevision },
        { valor: 'FINALIZADAS' as const, Icono: History, texto: 'Finalizadas', cantidad: finalizadas },
    ];

    return (
        <section className="mis-postulaciones">
            <h1 className="mis-postulaciones-titulo">Mis postulaciones</h1>
            <div className="mis-postulaciones-filtros">
                {filtros.map(({ valor, Icono, texto, cantidad }) => (
                    <button
                        key={valor}
                        type="button"
                        className={filtro === valor ? 'activo' : ''}
                        onClick={() => setFiltro(valor)}
                        aria-pressed={filtro === valor}
                    >
                        <Icono size={17} />
                        {`${texto} (${cantidad})`}
                    </button>
                ))}
            </div>
            {error && (
                <div className="mis-postulaciones-vacio" role="alert">
                    <h2>No se pudieron cargar tus postulaciones</h2>
                    <p>{error}</p>
                </div>
            )}
            {cargando ? (
                <div className="mis-postulaciones-vacio">
                    <Cargando tipo="circulo" mensaje="Cargando tus postulaciones" />
                </div>
            ) : error ? null : postulacionesFiltradas.length === 0 ? (
                <div className="mis-postulaciones-vacio">
                    <ClipboardList size={46} />
                    <h2>{total === 0 ? 'Aún no tienes postulaciones' : 'No tienes postulaciones en esta categoría'}</h2>
                    <p>
                        {total === 0
                            ? 'Cuando postules a una convocatoria laboral, podrás consultar aquí su información y estado.'
                            : 'Prueba seleccionando otro filtro para consultar tus postulaciones.'}
                    </p>
                </div>
            ) : (
                <div className={`mis-postulaciones-cuerpo ${seleccionada ? 'con-detalle' : ''}`}>
                    <div className="mis-postulaciones-lista">
                        {postulacionesFiltradas.map((p) => (
                            <button
                                type="button"
                                key={p.idPostulacion}
                                className={`mis-postulaciones-fila ${seleccionada?.idPostulacion === p.idPostulacion ? 'seleccionada' : ''}`}
                                onClick={() => { setSeleccionada(p); setPregunta(null); }}
                            >
                                {logo(p)}
                                <span>
                                    <strong>
                                        {p.puesto}
                                        {!p.visto && <span className="mis-postulaciones-nuevo">Nuevo</span>}
                                    </strong>
                                    <small className="mis-postulaciones-empresa">{p.empresa}</small>
                                    <small><Factory size={14} /> {[p.campamento, formatearUbicacion(p.ubicacion)].filter(Boolean).join(' · ') || 'Ubicación no indicada'}</small>
                                </span>
                                <span className={`mis-postulaciones-estado estado-${p.estado.toLowerCase()}`}>{obtenerEstado(p.estado)}</span>
                            </button>
                        ))}
                    </div>
                    {seleccionada && (
                        <aside className="mis-postulaciones-panel" aria-label="Detalle de postulación">
                            <div className="mis-postulaciones-panel-empresa">
                                {logo(seleccionada)}
                                <span>{seleccionada.empresa}</span>
                                <button type="button" onClick={() => setSeleccionada(null)} aria-label="Cerrar detalle">
                                    <X size={18} />
                                </button>
                            </div>
                            <h2>{seleccionada.puesto}</h2>
                            <p>
                                {[formatearUbicacion(seleccionada.ubicacion), seleccionada.campamento,
                                    `Postulaste el ${fechaCorta(seleccionada.fechaPostulacion)}`].filter(Boolean).join(' · ')}
                            </p>
                            <div className={`mis-postulaciones-aviso estado-${seleccionada.estado.toLowerCase()}`}>
                                {seleccionada.estado === 'ENTREVISTA' && seleccionada.entrevista ? (
                                    <>
                                        <strong>Te citaron a entrevista</strong>
                                        <span>
                                            <CalendarDays size={14} />
                                            {fechaCorta(seleccionada.entrevista.fecha, { weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric' })}
                                            {' · '}{seleccionada.entrevista.fecha.slice(11, 16)}
                                        </span>
                                        <span>
                                            <MapPin size={14} />
                                            {seleccionada.entrevista.modalidad === 'VIRTUAL' ? 'Virtual' : 'Presencial'} · {seleccionada.entrevista.lugar}
                                        </span>
                                        {seleccionada.entrevista.mensaje && (
                                            <span><MessageSquare size={14} /> {seleccionada.entrevista.mensaje}</span>
                                        )}
                                    </>
                                ) : (
                                    <span>{aviso(seleccionada)}</span>
                                )}
                                {seleccionada.estado === 'ACEPTADO' && (pregunta ? confirmacion(seleccionada, pregunta) : (
                                    <div>
                                        <button type="button" className="mis-postulaciones-confirmar" onClick={() => setPregunta('confirmar')}>Confirmar</button>
                                        <button type="button" onClick={() => setPregunta('rechazar')}>Rechazar oferta</button>
                                    </div>
                                ))}
                            </div>
                            <h3>Sobre la oferta</h3>
                            {seleccionada.fechaInicioLabores && seleccionada.fechaFinLabores && (
                                <p className="mis-postulaciones-labores">
                                    <CalendarDays size={14} /> Labores: {fechaCorta(seleccionada.fechaInicioLabores)} – {fechaCorta(seleccionada.fechaFinLabores)}
                                    {' · '}{(Date.parse(seleccionada.fechaFinLabores) - Date.parse(seleccionada.fechaInicioLabores)) / 86400000 + 1} días
                                </p>
                            )}
                            <dl>
                                {seleccionada.turno && <div><dt>Turno</dt><dd>{seleccionada.turno}</dd></div>}
                                <div><dt>Modalidad</dt><dd>{seleccionada.tipo === 'PARADA_PLANTA' ? 'Parada de planta' : 'Planilla completa'}</dd></div>
                                {seleccionada.direccion && (
                                    <div className="ancho">
                                        <dt>Dirección</dt>
                                        <dd>{[seleccionada.direccion, seleccionada.referencia && `Ref.: ${seleccionada.referencia}`].filter(Boolean).join(' · ')}</dd>
                                    </div>
                                )}
                                {seleccionada.descripcion && (
                                    <div className="ancho"><dt>Descripción</dt><dd className="texto">{seleccionada.descripcion}</dd></div>
                                )}
                            </dl>
                            <h3>Tu postulación</h3>
                            <dl>
                                <div className="ancho">
                                    <dt>Certificados enviados</dt>
                                    <dd>{seleccionada.certificacionesAdjuntas.join(', ') || 'Ninguno'}</dd>
                                </div>
                                <div><dt>Cierre</dt><dd>{fechaCorta(seleccionada.fechaCierre)}</dd></div>
                                <div><dt>Último cambio</dt><dd>{fechaCorta(seleccionada.fechaCambioEstado ?? seleccionada.fechaPostulacion)}</dd></div>
                            </dl>
                            {enProceso(seleccionada.estado) && (pregunta ? (
                                <div className="mis-postulaciones-aviso estado-retirado">{confirmacion(seleccionada, pregunta)}</div>
                            ) : (
                                <button type="button" className="mis-postulaciones-enlace" onClick={() => setPregunta('retirar')}>
                                    Retirarme de este proceso
                                </button>
                            ))}
                        </aside>
                    )}
                </div>
            )}
            <AvisoSoporte />
        </section>
    );
};

export default MisPostulaciones;
