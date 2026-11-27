import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from './AuthContext';
import { homeFor, RUTA_COMPLETAR_PERFIL, RUTA_COMPLETAR_REGISTRO, RUTA_SOLICITUD_EMPRESA, type Rol } from './session';
import Cargando from '../components/Cargando';

interface ProtectedRouteProps {
  children: React.ReactNode;
  allowedRoles?: Rol[];
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children, allowedRoles }) => {
  const location = useLocation();
  const { estado } = useAuth();

  if (estado.tipo === 'cargando') {
    return <Cargando tipo="montana" variante="pantalla" mensaje="Validando sesión" />;
  }

  if (estado.tipo === 'error') {
    return <ErrorDeSesion mensaje={estado.mensaje} />;
  }

  if (estado.tipo === 'anonimo') {
    if (estado.destino && estado.desde === location.pathname) return <Navigate to={estado.destino} replace />;
    return <Navigate to={estado.expirada ? '/login?expired=true' : '/login'} state={{ from: location }} replace />;
  }

  if (estado.tipo === 'sinRegistro') {
    return <Navigate to={RUTA_COMPLETAR_REGISTRO} replace />;
  }

  const { sesion } = estado;
  const ruta = location.pathname;

  if (allowedRoles && !allowedRoles.includes(sesion.rol)) {
    return <Navigate to={homeFor(sesion.rol)} replace />;
  }

  if (sesion.rol === 'POSTULANTE') {
    const enOnboarding = ruta === RUTA_COMPLETAR_PERFIL;
    if (!sesion.perfilCompleto && !enOnboarding) return <Navigate to={RUTA_COMPLETAR_PERFIL} replace />;
    if (sesion.perfilCompleto && enOnboarding) return <Navigate to={homeFor('POSTULANTE')} replace />;
  }

  if (sesion.rol === 'EMPRESA') {
    const enSolicitud = ruta === RUTA_SOLICITUD_EMPRESA;
    const aprobada = sesion.estadoEmpresa === 'APROBADA';
    if (!aprobada && !enSolicitud) return <Navigate to={RUTA_SOLICITUD_EMPRESA} replace />;
    if (aprobada && enSolicitud) return <Navigate to={homeFor('EMPRESA')} replace />;
  }

  return <>{children}</>;
};

function ErrorDeSesion({ mensaje }: { mensaje: string }) {
  const { cerrarSesion, refrescar } = useAuth();
  return (
      <div className="container" style={{ padding: '3rem 1rem', maxWidth: 560 }}>
        <div className="alert-message error" role="alert">
          <span>No pudimos cargar los datos de tu cuenta: {mensaje}</span>
        </div>
        <div style={{ display: 'flex', gap: '0.75rem' }}>
          <button type="button" className="btn btn-primary" onClick={() => void refrescar()}>
            Reintentar
          </button>
          <button
              type="button"
              className="btn btn-outline"
              onClick={() => {
                cerrarSesion('/login');
              }}
          >
            Cerrar sesión
          </button>
        </div>
      </div>
  );
}

export default ProtectedRoute;
