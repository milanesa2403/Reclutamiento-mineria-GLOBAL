import React, { useEffect } from 'react';
import { Routes, Route, useLocation, Navigate } from 'react-router-dom';

import Layout from './components/Layout';
import AdminLayout from './components/AdminLayout';
import ProtectedRoute from './auth/ProtectedRoute';
import { destinoPara, useAuth } from './auth/AuthContext';
import { RUTA_COMPLETAR_REGISTRO, type Rol } from './auth/session';
import Cargando from './components/Cargando';

import Landing from './pages/inicio/Landing';
import Login from './auth/Login';
import CrearCuenta from './auth/CrearCuenta';
import CompletarRegistro from './pages/registro/CompletarRegistro';
import RecuperarPassword from './auth/RecuperarPassword';
import VerificarEmail from './auth/VerificarEmail';
import EstadoSistema from './pages/inicio/EstadoSistema';
import NotFound from './pages/inicio/NotFound';

import EmpresaHome from './pages/empresa/EmpresaHome';
import EmpresaPerfil from './pages/empresa/EmpresaPerfil';
import SolicitudEmpresa from './pages/empresa/SolicitudEmpresa';
import EmpresaPostulantes from './pages/empresa/EmpresaPostulantes';

import PostulanteHome from './pages/postulante/PostulanteHome';
import FormularioPerfil from './pages/postulante/FormularioPerfil';
import Ofertas from './pages/postulante/Ofertas';

import AdminHome from './pages/admin/AdminHome';
import AdminEmpresas from './pages/admin/AdminEmpresas';

const Inicio: React.FC = () => {
    const { estado } = useAuth();
    if (estado.tipo === 'cargando') {
        return <Cargando tipo="montana" variante="pantalla" mensaje="Validando sesión" />;
    }
    if (estado.tipo === 'sinRegistro') {
        return <Navigate to={RUTA_COMPLETAR_REGISTRO} replace />;
    }
    if (estado.tipo === 'autenticado') {
        return <Navigate to={destinoPara(estado.sesion)} replace />;
    }
    return <Landing />;
};

const solo = (rol: Rol, pagina: React.ReactNode) => (
    <ProtectedRoute allowedRoles={[rol]}>{pagina}</ProtectedRoute>
);

const publicas: [string, React.ReactNode][] = [
    ['/', <Inicio />],
    ['/nosotros', <Landing />],
    ['/login', <Login />],
    ['/registro/empresa', <CrearCuenta tipo="empresa" />],
    ['/registro/postulante', <CrearCuenta tipo="postulante" />],
    ['/registro/verificar', <VerificarEmail />],
    [RUTA_COMPLETAR_REGISTRO, <CompletarRegistro />],
    ['/recuperar-password', <RecuperarPassword />],
    ['/convocatorias', <Ofertas publico />],
    ['/estado', <EstadoSistema />],
];

const privadas: [string, Rol, React.ReactNode][] = [
    ['/empresa', 'EMPRESA', <EmpresaHome />],
    ['/empresa/postulantes', 'EMPRESA', <EmpresaPostulantes />],
    ['/empresa/perfil', 'EMPRESA', <EmpresaPerfil />],
    ['/empresa/solicitud', 'EMPRESA', <SolicitudEmpresa />],
    ['/postulante', 'POSTULANTE', <Ofertas />],
    ['/postulante/completar-perfil', 'POSTULANTE',
        <div className="container" style={{ padding: '2rem 1rem 3rem' }}><FormularioPerfil /></div>],
    ['/postulante/perfil', 'POSTULANTE', <PostulanteHome />],
    ['/postulante/postulaciones', 'POSTULANTE', <PostulanteHome />],
];

export const App: React.FC = () => {
    const location = useLocation();

    useEffect(() => {
        if (location.hash) {
            const element = document.getElementById(location.hash.replace('#', ''));
            if (element) setTimeout(() => element.scrollIntoView({ behavior: 'smooth' }), 50);
        } else {
            window.scrollTo({ top: 0, behavior: 'smooth' });
        }
    }, [location.pathname, location.hash]);

    return (
        <Routes>
            <Route element={<Layout />}>
                {publicas.map(([ruta, pagina]) => <Route key={ruta} path={ruta} element={pagina} />)}
                {privadas.map(([ruta, rol, pagina]) => <Route key={ruta} path={ruta} element={solo(rol, pagina)} />)}
                <Route path="/admin" element={solo('ADMINISTRADOR', <AdminLayout />)}>
                    <Route index element={<AdminHome />} />
                    <Route path="empresas" element={<AdminEmpresas />} />
                    <Route path="ofertas" element={<Ofertas soloLectura />} />
                </Route>
                <Route path="*" element={<NotFound />} />
            </Route>
        </Routes>
    );
};

export default App;
