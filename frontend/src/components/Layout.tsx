import React, { useEffect, useState } from 'react';
import { Outlet, Link, useLocation, useNavigate } from 'react-router-dom';
import { Menu, X, User as UserIcon, LogIn, Building2, ChevronDown, LogOut } from 'lucide-react';
import { homeFor } from '../auth/session';
import { useAuth } from '../auth/AuthContext';
import { obtenerMiPerfil, type PerfilPostulante } from '../api/postulante';
import { CORREO_SOPORTE } from '../soporte';

const TIPO_USUARIO: Record<string, string> = {
    POSTULANTE: 'Postulante',
    EMPRESA: 'Representante',
    ADMINISTRADOR: 'Administrador',
};

export const Layout: React.FC = () => {
    const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
    const [menuPostulanteAbierto, setMenuPostulanteAbierto] = useState(false);
    const [perfilPostulante, setPerfilPostulante] = useState<PerfilPostulante | null>(null);
    const location = useLocation();
    const ruta = location.pathname;
    const esPanelPrivado =
        ruta === '/empresa' || ruta.startsWith('/empresa/') || ruta.startsWith('/postulante') || ruta.startsWith('/admin');

    const navigate = useNavigate();
    const { estado, cerrarSesion } = useAuth();
    const sesion = estado.tipo === 'autenticado' ? estado.sesion : null;
    const rol = sesion?.rol ?? null;
    const loggedIn = sesion !== null;
    const esPostulante = rol === 'POSTULANTE';

    useEffect(() => {
        if (!esPostulante) {
            setPerfilPostulante(null);
            return;
        }
        let activo = true;
        obtenerMiPerfil()
            .then((datos) => activo && setPerfilPostulante(datos))
            .catch(() => activo && setPerfilPostulante(null));
        return () => {
            activo = false;
        };
    }, [esPostulante, ruta]);

    const p = perfilPostulante;
    const pasosPerfil = [
        true,
        Boolean(p?.perfilCompleto),
        Boolean(p?.telefono && p?.direccion),
        Boolean(p?.resumenProfesional),
        Boolean(p?.documentos?.cv),
        Boolean(p?.documentos?.fotoDni && p?.documentos?.cul && p?.documentos?.emo),
    ];
    const porcentajePerfil = p ? Math.round((pasosPerfil.filter(Boolean).length / pasosPerfil.length) * 100) : null;
    const nombreUsuario = sesion?.nombre ?? '';

    const handleNavClick = (sectionId: string) => {
        setMobileMenuOpen(false);
        const desplazar = () => document.getElementById(sectionId)?.scrollIntoView({ behavior: 'smooth' });
        if (ruta !== '/' && ruta !== '/nosotros') {
            navigate(`/#${sectionId}`);
            setTimeout(desplazar, 100);
        } else {
            desplazar();
        }
    };

    const handleLogout = () => cerrarSesion();
    const cerrarMovil = () => setMobileMenuOpen(false);
    const activo = (condicion: boolean) => `nav-link ${condicion ? 'active' : ''}`;

    return (
        <div className="layout-wrapper">
            <header className="navbar">
                <div className="navbar-container">
                    <Link
                        to={loggedIn && rol ? homeFor(rol) : '/'}
                        className="navbar-brand"
                        title={loggedIn ? 'Ir a mi panel' : 'Ir al inicio'}
                    >
                        <img src="/images/mintra-logo.png" alt="MINTRA - Talento que mueve la industria" className="brand-logo-img" />
                    </Link>
                    {!loggedIn && (
                        <nav className="nav-menu desktop-only">
                            <button onClick={() => handleNavClick('inicio')} className={activo(ruta === '/' && !location.hash)}>
                                Inicio
                            </button>
                            <button
                                onClick={() => handleNavClick('nosotros')}
                                className={activo(location.hash === '#nosotros' || ruta === '/nosotros')}
                            >
                                Nosotros
                            </button>
                            <button onClick={() => handleNavClick('trabajos')} className={activo(location.hash === '#trabajos')}>
                                Trabajos
                            </button>
                            <Link to="/registro/postulante" className="nav-link">Postular</Link>
                            <Link to="/registro/empresa" className="nav-link">Para empresas</Link>
                            <Link to="/convocatorias" className={activo(ruta === '/convocatorias')}>Convocatorias</Link>
                        </nav>
                    )}
                    <div className="nav-actions desktop-only">
                        {loggedIn && rol ? (
                            <div className="user-logged-box">
                                {esPostulante ? (
                                    <div className="mintra-cuenta-postulante">
                                        <button
                                            type="button"
                                            className="mintra-cuenta-trigger"
                                            aria-expanded={menuPostulanteAbierto}
                                            aria-haspopup="menu"
                                            onClick={() => setMenuPostulanteAbierto(!menuPostulanteAbierto)}
                                        >
                                            <span className="navbar-representative-icon">
                                                <UserIcon size={18} />
                                            </span>
                                            <span className="navbar-representative-text">
                                                <span className="navbar-representative-name">{nombreUsuario}</span>
                                                <span className="navbar-representative-role">Postulante</span>
                                            </span>
                                            <ChevronDown size={18} className={menuPostulanteAbierto ? 'mintra-flecha abierta' : 'mintra-flecha'} />
                                        </button>
                                        {menuPostulanteAbierto && (
                                            <div className="mintra-cuenta-dropdown" role="menu">
                                                <div className="mintra-cuenta-progreso">
                                                    <strong>
                                                        {porcentajePerfil === null
                                                            ? 'Cargando perfil...'
                                                            : `Perfil completo al ${porcentajePerfil}%`}
                                                    </strong>
                                                    <div className="mintra-cuenta-barra">
                                                        <div className="mintra-cuenta-barra-valor" style={{ width: `${porcentajePerfil ?? 0}%` }} />
                                                    </div>
                                                </div>
                                                <div className="mintra-cuenta-separador" />
                                                <button
                                                    type="button"
                                                    role="menuitem"
                                                    className="mintra-cuenta-salir"
                                                    onClick={() => {
                                                        setMenuPostulanteAbierto(false);
                                                        handleLogout();
                                                    }}
                                                >
                                                    <LogOut size={18} />
                                                    Cerrar sesión
                                                </button>
                                            </div>
                                        )}
                                    </div>
                                ) : (
                                    <>
                                        <Link to={homeFor(rol)} className="navbar-representative" title="Ir a mi panel">
                                            <div className="navbar-representative-icon">
                                                <UserIcon size={18} />
                                            </div>
                                            <div className="navbar-representative-text">
                                                <span className="navbar-representative-name">{nombreUsuario}</span>
                                                <span className="navbar-representative-role">{TIPO_USUARIO[rol] ?? ''}</span>
                                            </div>
                                        </Link>
                                        <button type="button" onClick={handleLogout} className="btn btn-outline-sm">
                                            Cerrar sesión
                                        </button>
                                    </>
                                )}
                            </div>
                        ) : (
                            <div className="auth-quick-access">
                                <div className="register-prompt">
                                    <UserIcon size={18} className="user-icon" />
                                    <span className="prompt-text">¿No tienes cuenta?</span>
                                    <Link to="/registro/postulante" className="register-link">Regístrate</Link>
                                </div>
                                <Link to="/login" className="btn btn-primary-sm">
                                    <LogIn size={16} style={{ marginRight: '6px' }} />
                                    Iniciar sesión
                                </Link>
                            </div>
                        )}
                    </div>
                    <button
                        className="mobile-menu-toggle mobile-only"
                        onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                        aria-label="Abrir menú"
                    >
                        {mobileMenuOpen ? <X size={24} /> : <Menu size={24} />}
                    </button>
                </div>
                {mobileMenuOpen && (
                    <div className="mobile-drawer">
                        <nav className="mobile-nav-list">
                            <button onClick={() => handleNavClick('inicio')} className="mobile-nav-item">Inicio</button>
                            <button onClick={() => handleNavClick('nosotros')} className="mobile-nav-item">Sobre Nosotros</button>
                            <button onClick={() => handleNavClick('trabajos')} className="mobile-nav-item">Trabajos</button>
                            <Link to="/registro/empresa" className="mobile-nav-item" onClick={cerrarMovil}>
                                <Building2 size={16} /> Para Empresas Mineras
                            </Link>
                            <Link to="/convocatorias" className="mobile-nav-item" onClick={cerrarMovil}>Convocatorias</Link>
                        </nav>
                        <div className="mobile-auth-actions">
                            {loggedIn && rol ? (
                                <>
                                    <Link to={homeFor(rol)} className="btn btn-primary w-full" onClick={cerrarMovil}>
                                        Mi Panel ({rol})
                                    </Link>
                                    <button onClick={handleLogout} className="btn btn-outline w-full">Cerrar sesión</button>
                                </>
                            ) : (
                                <>
                                    <Link to="/login" className="btn btn-primary w-full" onClick={cerrarMovil}>Iniciar sesión</Link>
                                    <div className="mobile-register-options">
                                        <Link to="/registro/empresa" className="btn btn-secondary w-full" onClick={cerrarMovil}>
                                            Registrar mi empresa
                                        </Link>
                                        <Link to="/registro/postulante" className="btn btn-outline w-full" onClick={cerrarMovil}>
                                            Registrarme como postulante
                                        </Link>
                                    </div>
                                </>
                            )}
                        </div>
                    </div>
                )}
            </header>
            <main className="main-content">
                <Outlet />
            </main>
            {!esPanelPrivado && (
                <footer className="footer" id="contacto">
                    <div className="footer-top">
                        <div className="footer-container">
                            <div className="footer-motto-col">
                                <p className="motto-text">La minería conecta personas</p>
                                <div className="motto-underline" />
                            </div>
                        </div>
                    </div>
                    <div className="footer-bottom">
                        <div className="footer-container bottom-bar">
                            <p className="copyright">
                                © {new Date().getFullYear()} MINTRA Technologies S.A.C. Todos los derechos reservados.
                            </p>
                            {CORREO_SOPORTE && (
                                <p className="copyright">
                                    ¿Problemas con una empresa? Escríbenos a{' '}
                                    <a href={`mailto:${CORREO_SOPORTE}`} style={{ color: 'inherit', textDecoration: 'underline' }}>
                                        {CORREO_SOPORTE}
                                    </a>
                                </p>
                            )}
                        </div>
                    </div>
                </footer>
            )}
        </div>
    );
};

export default Layout;
