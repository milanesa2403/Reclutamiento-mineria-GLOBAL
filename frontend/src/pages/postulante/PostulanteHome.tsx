import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Briefcase, ClipboardList, UserRound } from 'lucide-react';

import { useAuth } from '../../auth/AuthContext';
import PostulantePerfil from './PostulantePerfil';
import MisPostulaciones from './MisPostulaciones';
import './Ofertas.css';
import './PostulanteHome.css';

type ModuloPostulante = 'perfil' | 'postulaciones';

const obtenerModuloURL = (pathname: string): ModuloPostulante =>
    pathname.endsWith('/postulaciones') ? 'postulaciones' : 'perfil';

const MODULOS = [['perfil', UserRound, 'Mi perfil'], ['postulaciones', ClipboardList, 'Mis postulaciones']] as const;

const PostulanteHome: React.FC = () => {
    const navigate = useNavigate();
    const location = useLocation();

    const [moduloActivo, setModuloActivo] =
        useState<ModuloPostulante>(() =>
            obtenerModuloURL(window.location.pathname)
        );

    useEffect(() => {
        setModuloActivo(obtenerModuloURL(location.pathname));
    }, [location.pathname]);

    const cambiarModulo = (modulo: ModuloPostulante) => {
        setModuloActivo(modulo);
        navigate(`/postulante/${modulo}`);
    };

    const { estado } = useAuth();

    const correoPostulante =
        estado.tipo === 'autenticado'
            ? estado.sesion.correo
            : '';

    return (
        <div className="postulante-panel-layout">
            <nav className="sub-navbar-ofertas" aria-label="Navegación del postulante">
                <div className="sub-navbar-container">
                    <button type="button" className="nav-link-btn" onClick={() => navigate('/postulante')}>
                        <Briefcase size={16} /> Ofertas laborales
                    </button>
                    {MODULOS.map(([modulo, Icono, texto]) => (
                        <button
                            key={modulo}
                            type="button"
                            className={moduloActivo === modulo ? 'nav-link-btn nav-link-active' : 'nav-link-btn'}
                            onClick={() => cambiarModulo(modulo)}
                        >
                            <Icono size={16} /> {texto}
                        </button>
                    ))}
                </div>
            </nav>
            <main className="postulante-panel-contenido">
                {moduloActivo === 'perfil' && (
                    <PostulantePerfil />
                )}
                {moduloActivo === 'postulaciones' && (
                    <MisPostulaciones correoPostulante={correoPostulante} />
                )}
            </main>
        </div>
    );
};

export default PostulanteHome;
