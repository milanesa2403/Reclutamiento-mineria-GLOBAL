import React from 'react';
import { Link } from 'react-router-dom';
import { HardHat, Compass } from 'lucide-react';

export const NotFound: React.FC = () => {
    return (
        <div className="container" style={{ padding: '5rem 1rem', textAlign: 'center', minHeight: '65vh' }}>
            <div style={{ maxWidth: '520px', margin: '0 auto' }}>
                <div style={{
                    width: '72px',
                    height: '72px',
                    background: '#fff7ed',
                    color: '#d95d18',
                    borderRadius: '50%',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    margin: '0 auto 1.5rem'
                }}>
                    <HardHat size={36} />
                </div>
                <h1 style={{ fontSize: '2.5rem', fontWeight: 800, color: '#0f172a', marginBottom: '0.5rem' }}>404</h1>
                <h2 style={{ fontSize: '1.5rem', color: '#334155', marginBottom: '1rem' }}>Página no encontrada en la faena</h2>
                <p style={{ color: '#64748b', fontSize: '1rem', lineHeight: '1.6', marginBottom: '2rem' }}>
                    La ruta que buscas no existe o ha sido reubicada dentro del sistema MinaLink.
                </p>
                <Link to="/" className="btn btn-primary">
                    <Compass size={18} style={{ marginRight: '8px' }} />
                    Volver a la Página de Inicio
                </Link>
            </div>
        </div>
    );
};

export default NotFound;
