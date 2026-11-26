import Cargando from '../../components/Cargando';
import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import {
    House, BriefcaseBusiness, Users, FilePenLine, Building2, Search, LogOut, ArrowRight, Clock3, Plus, Check, Trophy,
    type LucideIcon,
} from 'lucide-react';
import { useAuth } from '../../auth/AuthContext';
import { obtenerPerfilEmpresa, type PerfilEmpresa } from '../../api/empresa.ts';
import { listarMisConvocatorias, type Convocatoria } from '../../api/convocatorias.ts';
import Ofertas from '../postulante/Ofertas';
import CrearConvocatoria from './CrearConvocatoria';
import EmpresaPerfil from './EmpresaPerfil';
import EmpresaPostulantes from './EmpresaPostulantes';
import './EmpresaHome.css';

type ModuloEmpresa = 'inicio' | 'perfil' | 'convocatorias' | 'postulantes' | 'ofertas';

const MENU: { modulo: ModuloEmpresa; texto: string; Icono: LucideIcon; separar?: boolean }[] = [
    { modulo: 'inicio', texto: 'Inicio', Icono: House },
    { modulo: 'perfil', texto: 'Mi perfil', Icono: Building2 },
    { modulo: 'convocatorias', texto: 'Mis convocatorias', Icono: BriefcaseBusiness },
    { modulo: 'postulantes', texto: 'Postulantes', Icono: Users },
    { modulo: 'ofertas', texto: 'Ofertas laborales', Icono: Search, separar: true },
];

const obtenerModuloURL = (search: string): ModuloEmpresa => {
    const parametro = new URLSearchParams(search).get('modulo');
    return MENU.some((m) => m.modulo === parametro) ? (parametro as ModuloEmpresa) : 'inicio';
};

const EmpresaHome: React.FC = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const [moduloActivo, setModuloActivo] = useState<ModuloEmpresa>(() => obtenerModuloURL(window.location.search));
    const { estado, cerrarSesion } = useAuth();
    const nombreEmpresa = estado.tipo === 'autenticado' ? estado.sesion.nombre : '';
    const [perfil, setPerfil] = useState<PerfilEmpresa | null>(null);
    const [misConvocatorias, setMisConvocatorias] = useState<Convocatoria[] | null>(null);

    useEffect(() => {
        setModuloActivo(obtenerModuloURL(location.search));
    }, [location.search]);

    const cambiarModulo = (modulo: ModuloEmpresa) => {
        setModuloActivo(modulo);
        navigate(modulo === 'inicio' ? '/empresa' : `/empresa?modulo=${modulo}`);
    };

    const enInicio = moduloActivo === 'inicio';
    useEffect(() => {
        if (!enInicio) return;
        let activo = true;
        obtenerPerfilEmpresa()
            .then((datos) => {
                if (!activo) return;
                setPerfil(datos);
            })
            .catch(() => activo && setPerfil(null));
        listarMisConvocatorias()
            .then((datos) => activo && setMisConvocatorias(datos))
            .catch(() => activo && setMisConvocatorias([]));
        return () => {
            activo = false;
        };
    }, [enInicio]);

    const pasosPerfil = [
        { texto: 'Información empresarial', hecho: true },
        { texto: 'Datos de contacto', hecho: Boolean(perfil?.telefono) },
        { texto: 'Descripción institucional', hecho: Boolean(perfil?.descripcion) },
    ];
    const porcentajePerfil = perfil
        ? Math.round((pasosPerfil.filter((paso) => paso.hecho).length / pasosPerfil.length) * 100)
        : 0;

    const lista = misConvocatorias ?? [];
    const activas = lista.filter((c) => c.estado === 'PUBLICADA');
    const borradores = lista.filter((c) => c.estado === 'BORRADOR').length;
    const totalPostulaciones = lista.reduce((suma, c) => suma + (c.totalPostulaciones ?? 0), 0);
    const cifra = (valor: number) => (misConvocatorias === null ? '…' : valor);

    const tarjetas: { modulo: ModuloEmpresa; Icono: LucideIcon; valor: React.ReactNode; texto: string }[] = [
        { modulo: 'convocatorias', Icono: BriefcaseBusiness, valor: cifra(activas.length), texto: 'Convocatorias activas' },
        { modulo: 'postulantes', Icono: Users, valor: cifra(totalPostulaciones), texto: 'Postulaciones recibidas' },
        { modulo: 'convocatorias', Icono: FilePenLine, valor: cifra(borradores), texto: 'Borradores por publicar' },
        { modulo: 'convocatorias', Icono: Plus, valor: 'Crear', texto: 'Nueva convocatoria' },
    ];

    return (
        <div className="empresa-panel-layout">
            <aside className="empresa-sidebar">
                <div className="empresa-sidebar-header">
                    <span className="empresa-sidebar-mini">PANEL DE EMPRESA</span>
                    <h2>Mi empresa</h2>
                    <p>Gestiona tu perfil y convocatorias</p>
                </div>
                <nav className="empresa-sidebar-nav">
                    {MENU.map(({ modulo, texto, Icono, separar }) => (
                        <React.Fragment key={modulo}>
                            {separar && <div className="empresa-sidebar-divider" />}
                            <button
                                type="button"
                                className={moduloActivo === modulo ? 'empresa-sidebar-link activo' : 'empresa-sidebar-link'}
                                onClick={() => cambiarModulo(modulo)}
                            >
                                <Icono size={17} />
                                <span>{texto}</span>
                            </button>
                        </React.Fragment>
                    ))}
                    <div className="empresa-sidebar-divider" />
                    <button type="button" className="empresa-sidebar-link empresa-sidebar-logout" onClick={() => cerrarSesion()}>
                        <LogOut size={17} />
                        <span>Cerrar sesión</span>
                    </button>
                </nav>
                <div className="empresa-sidebar-frase">
                    <span>“</span>
                    <p>El progreso también se construye con personas.</p>
                </div>
            </aside>
            <main className="empresa-panel-contenido">
                {moduloActivo === 'inicio' && (
                    <div className="empresa-home">
                        <section className="empresa-home-header">
                            <div>
                                <span>PANEL DE EMPRESA</span>
                                <h1>Inicio</h1>
                                <p>Gestiona tu talento e impulsa el crecimiento de tu empresa.</p>
                            </div>
                        </section>
                        <section className="empresa-home-bienvenida">
                            <div className="empresa-bienvenida-contenido">
                                <span className="empresa-bienvenida-mini">BIENVENIDO DE NUEVO</span>
                                <h2>
                                    ¡Bienvenido,
                                    <strong>{' '}{nombreEmpresa}!</strong>
                                </h2>
                                <p>
                                    Administra tu perfil empresarial, revisa tus convocatorias y encuentra al talento
                                    que tu organización necesita.
                                </p>
                                <button type="button" className="empresa-bienvenida-boton" onClick={() => cambiarModulo('perfil')}>
                                    Ver perfil de empresa
                                    <ArrowRight size={16} />
                                </button>
                            </div>
                        </section>
                        <section className="empresa-estadisticas-grid">
                            {tarjetas.map(({ modulo, Icono, valor, texto }) => (
                                <article
                                    key={texto}
                                    className="empresa-estadistica-card empresa-estadistica-accion"
                                    onClick={() => cambiarModulo(modulo)}
                                >
                                    <div className="empresa-estadistica-icono">
                                        <Icono size={21} />
                                    </div>
                                    <div>
                                        <strong className={valor === 'Crear' ? 'empresa-accion-titulo' : undefined}>{valor}</strong>
                                        <span>{texto}</span>
                                    </div>
                                </article>
                            ))}
                        </section>
                        <section className="empresa-home-card empresa-perfil-estado">
                            <div className="empresa-seccion-titulo">
                                <div>
                                    <span>PERFIL EMPRESARIAL</span>
                                    <h2>Estado de tu perfil</h2>
                                </div>
                                <strong className="empresa-porcentaje">{porcentajePerfil}%</strong>
                            </div>
                            <div className="empresa-progreso">
                                <div className="empresa-progreso-barra" style={{ width: `${porcentajePerfil}%` }} />
                            </div>
                            <div className="empresa-perfil-estado-contenido">
                                <div className="empresa-checklist">
                                    {pasosPerfil.map((paso) => (
                                        <div key={paso.texto} className={paso.hecho ? undefined : 'pendiente'}>
                                            {paso.hecho ? <Check size={15} /> : <Clock3 size={15} />}
                                            {paso.texto}
                                        </div>
                                    ))}
                                </div>
                                <div className="empresa-beneficio">
                                    <Trophy size={24} />
                                    <div>
                                        <strong>Mejora la visibilidad de tu empresa</strong>
                                        <p>Un perfil completo genera mayor confianza entre los postulantes.</p>
                                    </div>
                                </div>
                            </div>
                            <button type="button" className="empresa-boton-secundario" onClick={() => cambiarModulo('perfil')}>
                                Completar perfil
                                <ArrowRight size={15} />
                            </button>
                        </section>
                        <section className="empresa-home-card">
                            <div className="empresa-seccion-titulo">
                                <div>
                                    <span>RECLUTAMIENTO</span>
                                    <h2>Convocatorias activas</h2>
                                </div>
                                <button type="button" className="empresa-ver-todas" onClick={() => cambiarModulo('convocatorias')}>
                                    Ver todas
                                    <ArrowRight size={14} />
                                </button>
                            </div>
                            {misConvocatorias === null ? (
                                <Cargando variante="inline" mensaje="Cargando convocatorias" />
                            ) : activas.length === 0 ? (
                                <p className="empresa-proximamente">
                                    <Clock3 size={16} />
                                    Aún no tienes convocatorias publicadas.
                                </p>
                            ) : (
                                <ul className="empresa-activas-lista">
                                    {activas.slice(0, 5).map((c) => (
                                        <li key={c.id}>
                                            <div>
                                                <strong>{c.puesto}</strong>
                                                <span>
                                                    {c.campamento} · {c.vacantes} {c.vacantes === 1 ? 'vacante' : 'vacantes'}
                                                    {c.fechaCierre
                                                        ? ` · cierra el ${new Date(`${c.fechaCierre}T12:00:00`).toLocaleDateString('es-PE')}`
                                                        : ''}
                                                </span>
                                            </div>
                                            <button
                                                type="button"
                                                className="empresa-ver-todas"
                                                onClick={() => navigate(`/empresa?modulo=postulantes&convocatoria=${c.id}`)}
                                            >
                                                {c.totalPostulaciones ?? 0} postulante(s)
                                                <ArrowRight size={14} />
                                            </button>
                                        </li>
                                    ))}
                                </ul>
                            )}
                        </section>
                    </div>
                )}
                {moduloActivo === 'perfil' && <EmpresaPerfil />}
                {moduloActivo === 'convocatorias' && <CrearConvocatoria />}
                {moduloActivo === 'ofertas' && <Ofertas soloLectura />}
                {moduloActivo === 'postulantes' && (
                    <EmpresaPostulantes key={new URLSearchParams(location.search).get('convocatoria') ?? 'general'} />
                )}
            </main>
        </div>
    );
};

export default EmpresaHome;
