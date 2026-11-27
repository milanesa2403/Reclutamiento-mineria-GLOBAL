import React, { useCallback, useEffect, useState } from 'react';
import { BriefcaseBusiness, GraduationCap, Pencil, Sparkles, Star, Wrench } from 'lucide-react';

import { textoError } from '../../api/client.ts';
import { obtenerPerfilProfesional, type PerfilProfesional } from '../../api/postulante.ts';
import Cargando from '../../components/Cargando';
import FormularioPerfil from './FormularioPerfil';

function mesAnio(fecha: string | null): string {
    if (!fecha) return 'Actualidad';
    return new Date(`${fecha}T12:00:00`).toLocaleDateString('es-PE', { month: 'short', year: 'numeric' });
}

const PerfilProfesionalSeccion: React.FC<{ puedeEditar: boolean }> = ({ puedeEditar }) => {
    const [perfil, setPerfil] = useState<PerfilProfesional | null>(null);
    const [error, setError] = useState('');
    const [editando, setEditando] = useState(false);
    const [mensaje, setMensaje] = useState('');

    const cargar = useCallback(() => {
        setError('');
        obtenerPerfilProfesional()
            .then(setPerfil)
            .catch((err) => setError(textoError(err)));
    }, []);

    useEffect(() => {
        cargar();
    }, [cargar]);

    const guardado = () => {
        setEditando(false);
        setMensaje('Perfil profesional actualizado.');
        cargar();
    };

    return (
        <section className="perfil-tarjeta">
            <div className="perfil-seccion-titulo">
                <div>
                    <span>PERFIL PROFESIONAL</span>
                    <h2>Experiencia y formación</h2>
                </div>
                {puedeEditar && !editando && perfil && (
                    <button type="button" className="boton-editar-seccion"
                            onClick={() => { setEditando(true); setMensaje(''); }}>
                        <Pencil size={15} /> Editar
                    </button>
                )}
            </div>
            {error && <p className="mensaje-error" role="alert">{error}</p>}
            {mensaje && <p className="cv-nuevo-aviso" role="status">{mensaje}</p>}
            {editando ? (
                <FormularioPerfil edicion={{ onGuardado: guardado, onCancelar: () => setEditando(false) }} />
            ) : !perfil ? (
                !error && <Cargando variante="inline" mensaje="Cargando tu perfil profesional" />
            ) : (
                <div className="pp-bloques">
                    <div className="pp-bloque">
                        <h3><Sparkles size={16} /> Especialidades</h3>
                        <div className="pp-chips">
                            {perfil.especialidades.map((e) => (
                                <span key={e.nombre} className={e.principal ? 'pp-chip pp-chip-principal' : 'pp-chip'}>
                                    {e.principal && <Star size={13} />} {e.nombre}
                                </span>
                            ))}
                        </div>
                    </div>
                    <div className="pp-bloque">
                        <h3><Wrench size={16} /> Habilidades</h3>
                        <div className="pp-chips">
                            {perfil.habilidades.map((h) => <span key={h} className="pp-chip">{h}</span>)}
                        </div>
                    </div>
                    <div className="pp-bloque">
                        <h3><BriefcaseBusiness size={16} /> Experiencia laboral</h3>
                        {perfil.experiencias.length === 0 ? (
                            <p className="texto-ayuda">Aún no registraste experiencia laboral.</p>
                        ) : (
                            <ul className="pp-lista">
                                {perfil.experiencias.map((x, i) => (
                                    <li key={i}>
                                        <strong>{x.puesto}</strong>
                                        <span>{x.empresa} · {mesAnio(x.fechaInicio)} – {mesAnio(x.fechaFin)}</span>
                                        {x.descripcion && <p>{x.descripcion}</p>}
                                    </li>
                                ))}
                            </ul>
                        )}
                    </div>
                    <div className="pp-bloque">
                        <h3><GraduationCap size={16} /> Formación académica</h3>
                        {perfil.formaciones.length === 0 ? (
                            <p className="texto-ayuda">Aún no registraste tu formación académica.</p>
                        ) : (
                            <ul className="pp-lista">
                                {perfil.formaciones.map((x, i) => (
                                    <li key={i}>
                                        <strong>{x.carreraGrado}</strong>
                                        <span>{x.institucion} · {mesAnio(x.fechaInicio)} – {x.fechaFin ? mesAnio(x.fechaFin) : 'En curso'}</span>
                                    </li>
                                ))}
                            </ul>
                        )}
                    </div>
                </div>
            )}
        </section>
    );
};

export default PerfilProfesionalSeccion;
