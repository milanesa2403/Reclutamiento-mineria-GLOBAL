import Cargando from '../../components/Cargando';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    Search, MapPin, Briefcase, Calendar, X,
    CheckCircle, AlertTriangle, Clock, Building2, FileText, Info,
    UserRound, Bookmark, BookmarkCheck, ShieldCheck, FolderOpen, Users, ClipboardList, Phone, Mail,
} from 'lucide-react';
import {
    buscarConvocatorias,
    estaDisponible,
    guardarConvocatoria,
    listarGuardadas,
    quitarGuardada,
    type Convocatoria,
    type TipoConvocatoria,
} from '../../api/convocatorias';
import {
    listarMisPostulaciones,
    listarRequisitos,
    postular,
    type RequisitoDeConvocatoria,
    type RequisitosPostulacion,
} from '../../api/postulaciones';
import { formatearUbicacion } from '../../api/ubigeo';
import { textoError, getStatus } from '../../api/client';
import SelectorUbigeo, { UBIGEO_VACIO, type ValorUbigeo } from '../../components/SelectorUbigeo';
import './Ofertas.css';

type TabFiltro = 'todas' | 'pronto' | 'nuevas' | 'guardadas';

type Alerta =
    | { tipo: 'incompleto'; faltantes: string[] }
    | { tipo: 'requisitos'; datos: RequisitosPostulacion }
    | { tipo: 'exito' }
    | { tipo: 'error'; mensaje: string };

type SidebarEstado =
    | { tipo: 'oferta'; oferta: Convocatoria }
    | { tipo: 'empresa'; empresa: Convocatoria['empresa']; ofertaOrigen: Convocatoria }
    | null;

const DIA_MS = 24 * 60 * 60 * 1000;

const nombreTipo = (tipo: TipoConvocatoria) =>
    tipo === 'PARADA_PLANTA' ? 'Parada de Planta' : 'Planilla Completa';

const aFecha = (iso: string) => new Date(iso.length === 10 ? `${iso}T12:00:00` : iso);

const mostrarFecha = (iso: string | null) =>
    iso ? aFecha(iso).toLocaleDateString('es-PE', { day: '2-digit', month: '2-digit', year: 'numeric' }) : '—';

const diasRestantes = (c: Convocatoria): number | null => {
    if (!c.fechaCierre) return null;
    const hoy = new Date();
    hoy.setHours(12, 0, 0, 0);
    return Math.max(0, Math.round((aFecha(c.fechaCierre).getTime() - hoy.getTime()) / DIA_MS));
};

const esNueva = (c: Convocatoria) =>
    !!c.fechaPublicacion && Date.now() - aFecha(c.fechaPublicacion).getTime() <= 3 * DIA_MS;

const opcionInicial = (r: RequisitoDeConvocatoria): number | '' =>
    r.opciones.find((o) => !r.requierePdf || o.tienePdf)?.idCertificacion ?? '';

interface Props {
    soloLectura?: boolean;
    publico?: boolean;
}

export default function Ofertas({ soloLectura = false, publico = false }: Props) {
    const sinPostulante = soloLectura || publico;
    const navigate = useNavigate();

    const [convocatorias, setConvocatorias] = useState<Convocatoria[]>([]);
    const [guardadas, setGuardadas] = useState<Convocatoria[]>([]);
    const [postuladas, setPostuladas] = useState<Set<number>>(new Set());
    const [sinVer, setSinVer] = useState(0);
    const [perfil, setPerfil] = useState<RequisitosPostulacion | null>(null);
    const [cargando, setCargando] = useState(true);
    const [errorCarga, setErrorCarga] = useState('');

    const [filtroTexto, setFiltroTexto] = useState('');
    const [textoBuscado, setTextoBuscado] = useState('');
    const [ubigeo, setUbigeo] = useState<ValorUbigeo>(UBIGEO_VACIO);
    const [filtroTipo, setFiltroTipo] = useState<'' | TipoConvocatoria>(() => {
        const tipo = new URLSearchParams(window.location.search).get('tipo');
        return tipo === 'PARADA_PLANTA' || tipo === 'PLANILLA_COMPLETA' ? tipo : '';
    });
    const [tabActivo, setTabActivo] = useState<TabFiltro>('todas');

    const [sidebarActivo, setSidebarActivo] = useState<SidebarEstado>(null);

    const [alerta, setAlerta] = useState<Alerta | null>(null);
    const [eleccion, setEleccion] = useState<Record<number, number | ''>>({});
    const [procesando, setProcesando] = useState(false);
    const [guardando, setGuardando] = useState<number | null>(null);
    const [ofertaPedida, setOfertaPedida] = useState(() => Number(new URLSearchParams(window.location.search).get('oferta')) || null);
    useEffect(() => {
        if (ofertaPedida === null || cargando) return;
        const oferta = convocatorias.find((c) => c.id === ofertaPedida);
        if (oferta) setSidebarActivo({ tipo: 'oferta', oferta });
        setOfertaPedida(null);
    }, [ofertaPedida, cargando, convocatorias]);

    const idsGuardadas = useMemo(() => new Set(guardadas.map((g) => g.id)), [guardadas]);

    useEffect(() => {
        const t = setTimeout(() => setTextoBuscado(filtroTexto.trim()), 350);
        return () => clearTimeout(t);
    }, [filtroTexto]);

    useEffect(() => {
        let activo = true;
        setCargando(true);
        setErrorCarga('');
        buscarConvocatorias({
            texto: textoBuscado || undefined,
            tipo: filtroTipo || undefined,
            departamento: ubigeo.departamento || undefined,
            provincia: ubigeo.provincia || undefined,
            distrito: ubigeo.distrito || undefined,
        })
            .then((lista) => activo && setConvocatorias(lista))
            .catch((e) => activo && setErrorCarga(textoError(e)))
            .finally(() => activo && setCargando(false));
        return () => {
            activo = false;
        };
    }, [textoBuscado, filtroTipo, ubigeo]);

    const cargarDelPostulante = useCallback(async () => {
        const [g, p, r] = await Promise.allSettled([listarGuardadas(), listarMisPostulaciones(), listarRequisitos()]);
        if (g.status === 'fulfilled') setGuardadas(g.value);
        if (p.status === 'fulfilled') {
            setPostuladas(new Set(p.value.map((x) => x.idConvocatoria)));
            setSinVer(p.value.filter((x) => !x.visto).length);
        }
        if (r.status === 'fulfilled') setPerfil(r.value);
    }, []);

    useEffect(() => {
        if (!sinPostulante) void cargarDelPostulante();
    }, [cargarDelPostulante, sinPostulante]);

    const countPronto = convocatorias.filter((c) => (diasRestantes(c) ?? 99) <= 3).length;
    const countNuevas = convocatorias.filter(esNueva).length;

    const lista = useMemo(() => {
        switch (tabActivo) {
            case 'pronto':
                return convocatorias.filter((c) => (diasRestantes(c) ?? 99) <= 3);
            case 'nuevas':
                return convocatorias.filter(esNueva);
            case 'guardadas':
                return guardadas;
            default:
                return convocatorias;
        }
    }, [tabActivo, convocatorias, guardadas]);

    const alternarGuardada = async (oferta: Convocatoria) => {
        if (guardando !== null) return;
        setGuardando(oferta.id);
        try {
            if (idsGuardadas.has(oferta.id)) {
                await quitarGuardada(oferta.id);
                setGuardadas((actual) => actual.filter((g) => g.id !== oferta.id));
            } else {
                await guardarConvocatoria(oferta.id);
                setGuardadas((actual) => [{ ...oferta, guardada: true }, ...actual]);
            }
        } catch (e) {
            setAlerta({ tipo: 'error', mensaje: textoError(e) });
        } finally {
            setGuardando(null);
        }
    };

    const iniciarPostulacion = async (oferta: Convocatoria) => {
        setProcesando(true);
        try {
            const datos = await listarRequisitos(oferta.id);
            if (!datos.puedePostular) {
                setAlerta({ tipo: 'incompleto', faltantes: datos.faltantes });
                return;
            }
            setEleccion(Object.fromEntries(datos.certificadosRequeridos.map((r) => [r.idRequisito, opcionInicial(r)])));
            setAlerta({ tipo: 'requisitos', datos });
        } catch (e) {
            setAlerta({ tipo: 'error', mensaje: textoError(e) });
        } finally {
            setProcesando(false);
        }
    };

    const confirmarPostulacion = async (datos: RequisitosPostulacion) => {
        if (sidebarActivo?.tipo !== 'oferta') return;
        const ofertaActual = sidebarActivo.oferta;

        const requisitos = datos.certificadosRequeridos.map((r) => ({
            idRequisito: r.idRequisito,
            idCertificacion: eleccion[r.idRequisito],
        }));
        if (requisitos.some((r) => r.idCertificacion === '')) return;

        setProcesando(true);
        try {
            await postular({
                idConvocatoria: ofertaActual.id,
                requisitos: requisitos as Array<{ idRequisito: number; idCertificacion: number }>,
            });
            setPostuladas((actual) => new Set(actual).add(ofertaActual.id));
            setAlerta({ tipo: 'exito' });
        } catch (e) {
            if (getStatus(e) === 409) {
                setPostuladas((actual) => new Set(actual).add(ofertaActual.id));
            }
            setAlerta({ tipo: 'error', mensaje: textoError(e) });
        } finally {
            setProcesando(false);
        }
    };

    const cerrarTodo = () => {
        setAlerta(null);
        setSidebarActivo(null);
    };

    const perfilIncompleto = perfil !== null && (!perfil.perfilCompleto || !perfil.tieneCv);

    const botonGuardar = (oferta: Convocatoria, clase: string) => {
        if (sinPostulante) return null;
        const guardada = idsGuardadas.has(oferta.id);
        return (
            <button
                type="button"
                className={`${clase} ${guardada ? 'guardada' : ''}`}
                onClick={() => void alternarGuardada(oferta)}
                disabled={guardando === oferta.id}
                aria-pressed={guardada}
                title={guardada ? 'Quitar de guardadas' : 'Guardar para después'}
            >
                {guardada ? <BookmarkCheck size={17} /> : <Bookmark size={17} />}
                {guardada ? 'Guardada' : 'Guardar'}
            </button>
        );
    };

    return (
        <div className="convocatorias-layout">
            {!sinPostulante && (
                <div className="sub-navbar-ofertas">
                    <div className="sub-navbar-container">
                        <span className="nav-link-active"><Briefcase size={16} /> Ofertas laborales</span>
                        <button className="nav-link-btn" onClick={() => navigate('/postulante/perfil')}>
                            <UserRound size={16} /> Mi perfil
                        </button>
                        <button className="nav-link-btn" onClick={() => navigate('/postulante/postulaciones')}>
                            <ClipboardList size={16} /> Mis postulaciones{sinVer > 0 && ` (${sinVer})`}
                        </button>
                    </div>
                </div>
            )}
            {perfilIncompleto && (
                <div className="banner-perfil-alerta">
                    <div className="banner-content">
                        <div className="banner-text">
                            <AlertTriangle className="text-orange-600" size={18} />
                            <span><strong>Completa tu perfil para postular:</strong> {perfil.faltantes.join(' ')}</span>
                        </div>
                        <button className="banner-btn" onClick={() => navigate('/postulante/perfil')}>
                            Completar perfil
                        </button>
                    </div>
                </div>
            )}
            <div className={`convocatorias-layout-principal ${sidebarActivo ? 'con-sidebar' : ''}`} style={{ marginTop: '2.5rem' }}>
                <main className="convocatorias-container" style={{ padding: 0 }}>
                    <div className="convocatorias-header">
                        <h1 className="convocatorias-title">
                            {publico ? 'Convocatorias' : soloLectura ? 'Ofertas laborales publicadas' : 'Búsqueda de Ofertas'}
                        </h1>
                        <p className="convocatorias-desc">
                            {publico
                                ? 'Explora las ofertas vigentes del sector minero. Para postular, crea tu cuenta de postulante.'
                                : soloLectura
                                ? 'Así ven los postulantes las convocatorias publicadas. Solo lectura.'
                                : 'Encuentra convocatorias acordes a tu perfil y ubicación en campamento.'}
                        </p>
                    </div>
                    <div className="tabs-modern-container">
                        <button onClick={() => setTabActivo('todas')} className={`tab-modern ${tabActivo === 'todas' ? 'active' : ''}`}>
                            Todas <span className="tab-count">{convocatorias.length}</span>
                        </button>
                        <button onClick={() => setTabActivo('pronto')} className={`tab-modern ${tabActivo === 'pronto' ? 'active' : ''}`}>
                            Cierran pronto <span className="tab-count">{countPronto}</span>
                        </button>
                        <button onClick={() => setTabActivo('nuevas')} className={`tab-modern ${tabActivo === 'nuevas' ? 'active' : ''}`}>
                            Nuevas <span className="tab-count">{countNuevas}</span>
                        </button>
                        {!sinPostulante && (
                            <button onClick={() => setTabActivo('guardadas')} className={`tab-modern ${tabActivo === 'guardadas' ? 'active' : ''}`}>
                                Guardadas <span className="tab-count">{guardadas.length}</span>
                            </button>
                        )}
                    </div>
                    {tabActivo !== 'guardadas' && (
                        <div className="convocatorias-filtros shadow-sm">
                            <div className="filtro-input-wrapper">
                                <Search className="filtro-icon text-slate-400" />
                                <input
                                    type="text"
                                    placeholder="Buscar puesto, empresa o campamento..."
                                    className="filtro-input"
                                    value={filtroTexto}
                                    onChange={(e) => setFiltroTexto(e.target.value)}
                                />
                            </div>
                            <SelectorUbigeo
                                valor={ubigeo}
                                onChange={setUbigeo}
                                textosVacios={['Todos los departamentos', 'Todas las provincias', 'Todos los distritos']}
                                claseCampo="filtro-select-wrapper"
                                claseSelect="filtro-select"
                                icono={<MapPin className="filtro-icon text-orange-500" />}
                                idBase="feed-ubigeo"
                            />
                            <div className="filtro-select-wrapper">
                                <Briefcase className="filtro-icon text-blue-900" />
                                <select
                                    className="filtro-select"
                                    value={filtroTipo}
                                    onChange={(e) => setFiltroTipo(e.target.value as '' | TipoConvocatoria)}
                                    aria-label="Modalidad"
                                >
                                    <option value="">Cualquier modalidad</option>
                                    <option value="PLANILLA_COMPLETA">Planilla Completa</option>
                                    <option value="PARADA_PLANTA">Parada de Planta</option>
                                </select>
                            </div>
                        </div>
                    )}
                    {errorCarga && <div className="convocatorias-aviso error">{errorCarga}</div>}
                    <div className="convocatorias-lista">
                        {cargando && tabActivo !== 'guardadas' && (
                            <Cargando tipo="circulo" mensaje="Cargando convocatorias" />
                        )}
                        {!cargando && lista.length === 0 && (
                            <div className="convocatorias-aviso">
                                {tabActivo === 'guardadas'
                                    ? 'Aún no guardas convocatorias. Usa el botón «Guardar» para revisarlas después.'
                                    : 'No hay convocatorias que coincidan con tu búsqueda.'}
                            </div>
                        )}

                        {lista.map((oferta) => {
                            const dias = diasRestantes(oferta);
                            const disponible = estaDisponible(oferta);
                            const esSeleccionada = sidebarActivo?.tipo === 'oferta' && sidebarActivo.oferta.id === oferta.id;
                            return (
                                <div key={oferta.id} className={`convocatoria-card-modern shadow-sm ${disponible ? '' : 'no-disponible'} ${esSeleccionada ? 'border-primary' : ''}`}>
                                    <div className="card-logo">
                                        {oferta.empresa.logoUrl ? (
                                            <img
                                                src={oferta.empresa.logoUrl}
                                                alt="Logo de la empresa"
                                                style={{ width: '32px', height: '32px', objectFit: 'cover', borderRadius: '6px' }}
                                            />
                                        ) : (
                                            <Building2 className="w-8 h-8 text-slate-400" />
                                        )}
                                    </div>
                                    <div className="card-body">
                                        <div className="card-tags">
                                            <span className="tag-modalidad">{nombreTipo(oferta.tipo)}</span>
                                            <span className="tag-turno">{oferta.turno}</span>
                                            {esNueva(oferta) && disponible && <span className="tag-nueva">NUEVA</span>}
                                            {!disponible && <span className="tag-cerrada">YA NO DISPONIBLE</span>}
                                            {postuladas.has(oferta.id) && <span className="tag-postulada">POSTULASTE</span>}
                                        </div>
                                        {oferta.busqueda && (
                                            <div className="card-busqueda"><FolderOpen size={14} /> {oferta.busqueda.nombre}</div>
                                        )}
                                        <h2 className="card-puesto">{oferta.puesto}</h2>
                                        {publico ? (
                                            <span className="card-empresa">
                                                {oferta.empresa.razonSocial}
                                            </span>
                                        ) : (
                                            <button type="button" className="card-empresa enlace-empresa"
                                                    onClick={() => setSidebarActivo({ tipo: 'empresa', empresa: oferta.empresa, ofertaOrigen: oferta })}>
                                                {oferta.empresa.razonSocial}
                                                
                                            </button>
                                        )}
                                        <div className="card-meta">
                                            <span className="meta-item"><MapPin className="text-orange-500" /> {formatearUbicacion(oferta.ubicacion)} ({oferta.campamento})</span>
                                            <span className="meta-item"><Users className="text-blue-900" /> {oferta.vacantes} {oferta.vacantes === 1 ? 'vacante' : 'vacantes'}</span>
                                            <span className="meta-item"><Calendar className="text-blue-900" /> Pub: {mostrarFecha(oferta.fechaPublicacion)}</span>
                                        </div>
                                    </div>
                                    <div className="card-actions">
                                        {disponible && (
                                            <div className={`badge-dias ${dias !== null && dias <= 3 ? 'urgente' : ''}`}>
                                                <Clock className="w-4 h-4" />
                                                {dias === null ? 'Sin fecha de cierre' : dias === 0 ? 'Cierra hoy' : `Cierra en ${dias} días`}
                                            </div>
                                        )}
                                        <div className="card-botones">
                                            {botonGuardar(oferta, 'card-btn-guardar')}
                                            <button onClick={() => setSidebarActivo({ tipo: 'oferta', oferta })} className="card-btn">
                                                Ver detalles
                                            </button>
                                        </div>
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                </main>
                {sidebarActivo && (
                    <aside className="sidebar-panel-costado">

                        {sidebarActivo.tipo === 'oferta' && (() => {
                            const ofertaSeleccionada = sidebarActivo.oferta;
                            return (
                                <div className="sidebar-contenido-unificado scrollable" style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
                                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', borderBottom: '1px solid #e2e8f0', paddingBottom: '1rem' }}>
                                        <div>
                                            <div className="modal-header-tags" style={{ marginBottom: '0.5rem' }}>
                                                <span className="modal-badge-tipo" style={{ background: '#ffedd5', color: '#c2410c' }}>{nombreTipo(ofertaSeleccionada.tipo)}</span>
                                            </div>
                                            <h2 style={{ fontSize: '1.4rem', fontWeight: 800, color: '#0f2640', margin: '0 0 0.25rem' }}>{ofertaSeleccionada.puesto}</h2>
                                            {publico ? (
                                                <span
                                                      style={{ fontSize: '0.95rem', color: '#475569', fontWeight: 600 }}>
                                                    {ofertaSeleccionada.empresa.razonSocial}
                                                </span>
                                            ) : (
                                                <button
                                                    type="button"
                                                    className="enlace-empresa"
                                                    style={{ fontSize: '0.95rem', color: '#475569', fontWeight: 600 }}
                                                    onClick={() => setSidebarActivo({ tipo: 'empresa', empresa: ofertaSeleccionada.empresa, ofertaOrigen: ofertaSeleccionada })}
                                                >
                                                    {ofertaSeleccionada.empresa.razonSocial}
                                                </button>
                                            )}
                                        </div>
                                        <button onClick={() => setSidebarActivo(null)} className="modal-close-btn" style={{ position: 'relative', top: 0, right: 0, background: '#f1f5f9', color: '#475569' }}>
                                            <X size={20} />
                                        </button>
                                    </div>
                                    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem', flex: 1, overflowY: 'auto', paddingRight: '4px' }}>
                                        <div>
                                            <h3 className="section-title" style={{ fontSize: '1rem' }}><FileText className="w-4 h-4 text-orange-600" /> Descripción del Puesto</h3>
                                            <p className="section-text" style={{ fontSize: '0.9rem' }}>{ofertaSeleccionada.descripcion}</p>
                                        </div>
                                        <div>
                                            <h3 className="section-title" style={{ fontSize: '1rem' }}><CheckCircle className="w-4 h-4 text-green-600" /> Requisitos Obligatorios</h3>
                                            <div className="req-box req-obligatorio" style={{ fontSize: '0.88rem' }}>{ofertaSeleccionada.requisitosObligatorios}</div>
                                        </div>
                                        {ofertaSeleccionada.requisitosDeseables && (
                                            <div>
                                                <h3 className="section-title" style={{ fontSize: '1rem' }}><Info className="w-4 h-4 text-blue-600" /> Requisitos Deseables</h3>
                                                <div className="req-box req-deseable" style={{ fontSize: '0.88rem' }}>{ofertaSeleccionada.requisitosDeseables}</div>
                                            </div>
                                        )}
                                        {ofertaSeleccionada.certificadosRequeridos.length > 0 && (
                                            <div>
                                                <h3 className="section-title" style={{ fontSize: '1rem' }}><ShieldCheck className="w-4 h-4 text-orange-600" /> Certificados exigidos</h3>
                                                <ul className="cert-exigidos" style={{ fontSize: '0.88rem' }}>
                                                    {ofertaSeleccionada.certificadosRequeridos.map((c) => (
                                                        <li key={c.idRequisito}>
                                                            {c.nombre}
                                                            {c.requierePdf && <span className="cert-pdf">PDF obligatorio</span>}
                                                        </li>
                                                    ))}
                                                </ul>
                                            </div>
                                        )}
                                        <div className="side-card">
                                            <h4 className="side-title">Resumen de la Oferta</h4>
                                            <ul className="side-list" style={{ fontSize: '0.88rem' }}>
                                                <li>
                                                    <strong>Postulaciones hasta:</strong>{' '}
                                                    {ofertaSeleccionada.fechaCierre
                                                        ? mostrarFecha(ofertaSeleccionada.fechaCierre)
                                                        : 'Sin fecha de cierre'}
                                                </li>
                                                <li><strong>Vacantes:</strong> {ofertaSeleccionada.vacantes}</li>
                                                <li><strong>Experiencia:</strong> {ofertaSeleccionada.experienciaMinimaAnios ?? 0} años mín.</li>
                                                <li><strong>Régimen:</strong> {ofertaSeleccionada.turno}</li>
                                                <li><strong>Ubicación:</strong> {formatearUbicacion(ofertaSeleccionada.ubicacion)}</li>
                                            </ul>
                                        </div>
                                    </div>
                                    <div style={{ borderTop: '1px solid #e2e8f0', paddingTop: '1rem', display: 'flex', gap: '0.75rem', justifyContent: 'flex-end' }}>
                                        {botonGuardar(ofertaSeleccionada, 'btn-test-outline btn-guardar')}
                                        {soloLectura ? null : publico ? (
                                            <button
                                                className="btn-primary"
                                                onClick={() => navigate('/registro/postulante')}
                                                style={{ padding: '0.6rem 1.2rem', fontSize: '0.9rem' }}
                                            >
                                                Postular (crea tu cuenta)
                                            </button>
                                        ) : postuladas.has(ofertaSeleccionada.id) ? (
                                            <button className="btn-primary" disabled style={{ padding: '0.6rem 1.2rem', fontSize: '0.9rem' }}>Ya postulaste</button>
                                        ) : estaDisponible(ofertaSeleccionada) ? (
                                            <button
                                                className="btn-primary"
                                                disabled={procesando}
                                                onClick={() => void iniciarPostulacion(ofertaSeleccionada)}
                                                style={{ padding: '0.6rem 1.2rem', fontSize: '0.9rem' }}
                                            >
                                                {procesando ? 'Verificando...' : 'Postularme'}
                                            </button>
                                        ) : (
                                            <button className="btn-primary" disabled style={{ padding: '0.6rem 1.2rem', fontSize: '0.9rem' }}>Cerrada</button>
                                        )}
                                    </div>
                                </div>
                            );
                        })()}

                        {sidebarActivo.tipo === 'empresa' && (() => {
                            const empresaVista = sidebarActivo.empresa;
                            const ofertaOrigen = sidebarActivo.ofertaOrigen;
                            return (
                                <div className="sidebar-contenido-unificado scrollable" style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
                                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid #e2e8f0', paddingBottom: '1rem' }}>
                                        <button
                                            onClick={() => setSidebarActivo({ tipo: 'oferta', oferta: ofertaOrigen })}
                                            className="btn-volver-atras"
                                            style={{ background: 'none', border: 'none', color: '#1e3a8a', cursor: 'pointer', fontWeight: 'bold', display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.9rem', padding: 0 }}
                                        >
                                            ← Volver a la oferta
                                        </button>
                                        <button onClick={() => setSidebarActivo(null)} className="modal-close-btn" style={{ position: 'relative', top: 0, right: 0, background: '#f1f5f9', color: '#475569' }} aria-label="Cerrar">
                                            <X size={20} />
                                        </button>
                                    </div>
                                    <div style={{ textAlign: 'center', margin: '1rem 0' }}>
                                        {empresaVista.logoUrl ? (
                                            <img
                                                src={empresaVista.logoUrl}
                                                alt="Logo de la empresa"
                                                style={{ width: '64px', height: '64px', objectFit: 'cover', borderRadius: '50%', margin: '0 auto 10px', display: 'block' }}
                                            />
                                        ) : (
                                            <Building2 className="text-blue-900" size={40} style={{ margin: '0 auto 10px' }} />
                                        )}
                                        <h3 style={{ fontSize: '1.3rem', fontWeight: 800, color: '#0f2640', marginBottom: '4px' }}>{empresaVista.razonSocial}</h3>
                                        {empresaVista.sectorMinero && <p style={{ color: '#64748b', fontSize: '0.88rem', fontWeight: 500 }}>Sector: {empresaVista.sectorMinero}</p>}
                                    </div>
                                    <p style={{ margin: '0 0 1.25rem', lineHeight: 1.6, color: '#334155', fontSize: '0.9rem' }}>
                                        {empresaVista.descripcion || 'Esta empresa aún no agregó una descripción.'}
                                    </p>
                                    <ul style={{ listStyle: 'none', padding: 0, display: 'flex', flexDirection: 'column', gap: '8px', marginBottom: '1.5rem', fontSize: '0.88rem', color: '#475569' }}>
                                        {empresaVista.direccion && <li style={{ display: 'flex', alignItems: 'center', gap: '8px' }}><MapPin size={15} /> {empresaVista.direccion}</li>}
                                        {empresaVista.telefono && <li style={{ display: 'flex', alignItems: 'center', gap: '8px' }}><Phone size={15} /> {empresaVista.telefono}</li>}
                                        <li style={{ display: 'flex', alignItems: 'center', gap: '8px' }}><Mail size={15} /> {empresaVista.correoCorporativo}</li>
                                    </ul>

                                    {(() => {
                                        const ofertas = convocatorias.filter((c) => c.empresa.id === empresaVista.id);
                                        return ofertas.length > 0 && (
                                            <div style={{ marginTop: 'auto', borderTop: '1px solid #e2e8f0', paddingTop: '1rem' }}>
                                                <h4 style={{ fontSize: '0.9rem', fontWeight: 800, color: '#0f2640', marginBottom: '10px', textTransform: 'uppercase' }}>Ofertas publicadas ({ofertas.length})</h4>
                                                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', maxHeight: '180px', overflowY: 'auto' }}>
                                                    {ofertas.map((c) => (
                                                        <button key={c.id} type="button" className="enlace-empresa"
                                                                style={{ textAlign: 'left', background: '#f8fafc', border: '1px solid #e2e8f0', padding: '8px 12px', borderRadius: '6px', cursor: 'pointer', fontSize: '0.88rem' }}
                                                                onClick={() => setSidebarActivo({ tipo: 'oferta', oferta: c })}>
                                                            {c.puesto}
                                                        </button>
                                                    ))}
                                                </div>
                                            </div>
                                        );
                                    })()}
                                </div>
                            );
                        })()}

                    </aside>
                )}
            </div>
            {alerta?.tipo === 'incompleto' && (
                <div className="modal-overlay z-top">
                    <div className="alert-box alert-warning">
                        <AlertTriangle className="alert-icon text-orange-500" />
                        <h3 className="alert-title">¡Aún no puedes postular!</h3>
                        <ul className="alert-lista">
                            {alerta.faltantes.map((f) => <li key={f}>{f}</li>)}
                        </ul>
                        <div className="alert-actions">
                            <button onClick={() => setAlerta(null)} className="btn-secondary">Volver</button>
                            <button onClick={() => navigate('/postulante/perfil')} className="btn-primary">Completar mi perfil</button>
                        </div>
                    </div>
                </div>
            )}
            {alerta?.tipo === 'requisitos' && sidebarActivo?.tipo === 'oferta' && (
                <div className="modal-overlay z-top">
                    <div className="alert-box alert-requisitos">
                        <ShieldCheck className="alert-icon text-orange-500" />
                        <h3 className="alert-title">Confirma tu postulación</h3>
                        <p className="alert-text">
                            {sidebarActivo.oferta.puesto} — {sidebarActivo.oferta.empresa.razonSocial}
                        </p>
                        {alerta.datos.certificadosRequeridos.length > 0 && (
                            <div className="req-eleccion">
                                <p className="alert-text">Indica qué certificado de tu perfil cumple cada requisito:</p>
                                {alerta.datos.certificadosRequeridos.map((r) => (
                                    <label key={r.idRequisito} className="req-eleccion-fila">
                                        <span>
                                            {r.nombre}
                                            {r.requierePdf && <span className="cert-pdf">PDF obligatorio</span>}
                                        </span>
                                        <select
                                            value={eleccion[r.idRequisito] ?? ''}
                                            onChange={(e) =>
                                                setEleccion((a) => ({
                                                    ...a,
                                                    [r.idRequisito]: e.target.value === '' ? '' : Number(e.target.value),
                                                }))
                                            }
                                        >
                                            <option value="">Elige un certificado</option>
                                            {r.opciones.map((o) => (
                                                <option
                                                    key={o.idCertificacion}
                                                    value={o.idCertificacion}
                                                    disabled={r.requierePdf && !o.tienePdf}
                                                >
                                                    {o.nombre}
                                                    {o.tienePdf ? ' (con PDF)' : r.requierePdf ? ' (falta el PDF)' : ''}
                                                    {o.coincide ? ' ★' : ''}
                                                </option>
                                            ))}
                                        </select>
                                    </label>
                                ))}
                            </div>
                        )}
                        <div className="alert-actions">
                            <button onClick={() => setAlerta(null)} className="btn-secondary" disabled={procesando}>Volver</button>
                            <button
                                onClick={() => void confirmarPostulacion(alerta.datos)}
                                className="btn-primary"
                                disabled={
                                    procesando ||
                                    alerta.datos.certificadosRequeridos.some((r) => (eleccion[r.idRequisito] ?? '') === '')
                                }
                            >
                                {procesando ? 'Enviando...' : 'Postularme'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
            {alerta?.tipo === 'exito' && (
                <div className="modal-overlay z-top">
                    <div className="alert-box alert-success">
                        <CheckCircle className="alert-icon text-green-600" />
                        <h3 className="alert-title">¡Postulación Exitosa!</h3>
                        <p className="alert-text">Tu perfil y documentos han sido enviados correctamente. Sigue su estado en «Mis postulaciones».</p>
                        <div className="alert-actions center">
                            <button onClick={cerrarTodo} className="btn-primary w-full">Entendido</button>
                        </div>
                    </div>
                </div>
            )}
            {alerta?.tipo === 'error' && (
                <div className="modal-overlay z-top">
                    <div className="alert-box alert-warning">
                        <AlertTriangle className="alert-icon text-orange-500" />
                        <h3 className="alert-title">No se pudo completar</h3>
                        <p className="alert-text">{alerta.mensaje}</p>
                        <div className="alert-actions center">
                            <button onClick={() => setAlerta(null)} className="btn-primary w-full">Entendido</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}