import React from 'react';
import './Cargando.css';

interface CargandoProps {
    mensaje?: string;
    tipo?: 'montana' | 'circulo';
    variante?: 'pagina' | 'pantalla' | 'inline';
}

export const Cargando: React.FC<CargandoProps> = ({
                                                      mensaje = 'Cargando',
                                                      tipo = 'montana',
                                                      variante = 'pagina',
                                                  }) => {
    const tipoFinal = variante === 'inline' ? 'circulo' : tipo;

    return (
        <div className={`cargando cargando--${variante}`} role="status" aria-live="polite">
            {tipoFinal === 'montana' ? (
                <div className="cg-escena" aria-hidden="true">
                    <svg viewBox="0 0 200 200" className="cg-svg">
                        <circle className="cg-pista" cx="100" cy="100" r="92" />
                        <circle className="cg-vuelta" cx="100" cy="100" r="92" />
                        <path className="cg-montana" d="M40 140 L90 62 L118 104 L136 82 L170 140 Z" />
                        <path className="cg-cumbre" d="M90 62 L103 82 L96 78 L90 86 L84 78 L77 82 Z" />
                        <circle className="cg-chispa" cx="90" cy="56" r="2.6" />
                        <circle className="cg-chispa cg-chispa--2" cx="98" cy="54" r="2" />
                    </svg>
                </div>
            ) : (
                <div className="cargando-spinner" aria-hidden="true">
                    <span className="cg-arco" />
                    <span className="cg-aro" />
                    <span className="cg-orbita" />
                    <span className="cg-nucleo" />
                </div>
            )}
            <p className="cargando-texto">
                {mensaje}
                <span className="cg-puntos" aria-hidden="true">
                    <i /><i /><i />
                </span>
            </p>
        </div>
    );
};

export default Cargando;