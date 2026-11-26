import React, { useEffect, useState } from 'react';
import {
    User,
    Phone,
    MapPin,
    CreditCard,
    Mail,
    FileText,
    Pencil,
    Save,
    X,
    CheckCircle2,
    ShieldCheck,
} from 'lucide-react';

import './PostulantePerfil.css';
import { textoError, erroresPorCampo } from '../../api/client.ts';
import {
    actualizarMiContacto,
    obtenerMiPerfil,
    type PerfilPostulante,
} from '../../api/postulante.ts';
import { useAuth } from '../../auth/AuthContext.tsx';
import CertificacionesSeccion from './CertificacionesSeccion';
import PerfilProfesionalSeccion from './PerfilProfesionalSeccion';
import DocumentosSeccion from './DocumentosSeccion';
import Cargando from '../../components/Cargando';
import { nombrePais, nombreTipoDocumento } from '../../utils/documentos.ts'

interface Formulario {
    telefono: string;
    direccion: string;
    resumenProfesional: string;
}

const TELEFONO = /^[+\d][\d\s-]{6,19}$/;
const MAX_DIRECCION = 255;
const MAX_RESUMEN = 2000;

function aFormulario(p: PerfilPostulante): Formulario {
    return {
        telefono: p.telefono ?? '',
        direccion: p.direccion ?? '',
        resumenProfesional: p.resumenProfesional ?? '',
    };
}

const PostulantePerfil: React.FC = () => {
    const { estado } = useAuth();
    const puedeEditar = estado.tipo === 'autenticado' && estado.sesion.rol === 'POSTULANTE';

    const [perfil, setPerfil] = useState<PerfilPostulante | null>(null);
    const [errorCarga, setErrorCarga] = useState<string | null>(null);

    const [formulario, setFormulario] = useState<Formulario>({ telefono: '', direccion: '', resumenProfesional: '' });
    const [editando, setEditando] = useState(false);
    const [guardando, setGuardando] = useState(false);
    const [mensajeExito, setMensajeExito] = useState('');
    const [errorGeneral, setErrorGeneral] = useState('');
    const [errores, setErrores] = useState<Record<string, string>>({});

    useEffect(() => {
        obtenerMiPerfil()
            .then((p) => {
                setPerfil(p);
                setFormulario(aFormulario(p));
            })
            .catch((err) => setErrorCarga(textoError(err)));
    }, []);

    useEffect(() => {
        if (!mensajeExito) return;
        const id = window.setTimeout(() => setMensajeExito(''), 4000);
        return () => window.clearTimeout(id);
    }, [mensajeExito]);

    const cambiarCampo = (
        event: React.ChangeEvent<
            HTMLInputElement | HTMLTextAreaElement
        >
    ) => {
        const { name, value } = event.target;

        setFormulario((anterior) => ({
            ...anterior,
            [name]: value,
        }));
        setErrores((anteriores) => {
            const copia = { ...anteriores };
            delete copia[name];
            return copia;
        });
    };

    const validarFormulario = (f: Formulario): Record<string, string> => {
        const e: Record<string, string> = {};
        if (!f.telefono.trim()) e.telefono = 'Ingresa un número de teléfono.';
        else if (!TELEFONO.test(f.telefono.trim())) e.telefono = 'Ingresa un teléfono válido, por ejemplo 987 654 321.';
        if (f.direccion.length > MAX_DIRECCION) e.direccion = `Máximo ${MAX_DIRECCION} caracteres.`;
        if (f.resumenProfesional.length > MAX_RESUMEN) e.resumenProfesional = `Máximo ${MAX_RESUMEN} caracteres.`;
        return e;
    };

    const iniciarEdicion = () => {
        if (!perfil) return;
        setFormulario(aFormulario(perfil));
        setEditando(true);
        setMensajeExito('');
        setErrorGeneral('');
        setErrores({});
    };

    const cancelarEdicion = () => {
        if (perfil) setFormulario(aFormulario(perfil));
        setEditando(false);
        setErrorGeneral('');
        setErrores({});
    };

    const guardarCambios = async () => {
        if (!puedeEditar || guardando) {
            return;
        }

        const encontrados = validarFormulario(formulario);
        setErrores(encontrados);
        if (Object.keys(encontrados).length > 0) {
            setErrorGeneral('Revisa los campos marcados.');
            return;
        }

        setGuardando(true);
        setErrorGeneral('');
        try {
            const actualizado = await actualizarMiContacto({
                telefono: formulario.telefono.trim(),
                direccion: formulario.direccion.trim() || null,
                resumenProfesional: formulario.resumenProfesional.trim() || null,
            });
            setPerfil(actualizado);
            setFormulario(aFormulario(actualizado));
            setEditando(false);
            setMensajeExito('Perfil actualizado correctamente.');
        } catch (error) {
            setErrores(erroresPorCampo(error));
            setErrorGeneral(textoError(error));
        } finally {
            setGuardando(false);
        }
    };

    const errorDe = (campo: keyof Formulario) => errores[campo] && (
        <span className="mensaje-error" role="alert">{errores[campo]}</span>
    );

    if (errorCarga) {
        return (
            <main className="perfil-postulante">
                <div className="perfil-contenedor">
                    <div className="mensaje-error"><span>No pudimos cargar tu perfil: {errorCarga}</span></div>
                </div>
            </main>
        );
    }

    if (!perfil) {
        return (
            <main className="perfil-postulante">
                <div className="perfil-contenedor">
                    <Cargando tipo="montana" mensaje="Cargando tu perfil" />
                </div>
            </main>
        );
    }

    return (
        <main className="perfil-postulante">
            <div className="perfil-contenedor">
                {mensajeExito && (
                    <div className="mensaje-exito" role="status">
                        <CheckCircle2 size={20} />
                        <span>{mensajeExito}</span>
                    </div>
                )}
                {errorGeneral && (
                    <div className="mensaje-error" role="alert">
                        <span>{errorGeneral}</span>
                    </div>
                )}
                <section className="perfil-cabecera">
                    <div className="perfil-avatar">
                        <User size={45} />
                    </div>
                    <div className="perfil-identidad">
                        <span className="perfil-etiqueta">
                            PERFIL PROFESIONAL
                        </span>
                        <h1>
                            {perfil.nombres}{' '}
                            {perfil.apellidos}
                        </h1>
                        <p>
                            Información personal y profesional
                            del postulante
                        </p>
                    </div>
                    {puedeEditar && !editando && (
                        <button type="button" className="boton-editar" onClick={iniciarEdicion}>
                            <Pencil size={17} />
                            Editar perfil
                        </button>
                    )}
                </section>
                <section className="perfil-tarjeta">
                    <div className="perfil-seccion-titulo">
                        <div>
                            <span>INFORMACIÓN PERSONAL</span>
                            <h2>
                                Datos del postulante
                            </h2>
                        </div>
                        <ShieldCheck size={21} />
                    </div>
                    <p className="texto-ayuda">
                        Los datos de identidad se muestran
                        únicamente como información de consulta.
                    </p>
                    <div className="perfil-form-grid">
                        <div className="campo-perfil">
                            <label htmlFor="perfil-nombres">
                                <User size={16} />
                                Nombres
                            </label>
                            <input id="perfil-nombres" value={perfil.nombres} disabled />
                            <span className="dato-protegido">
                                <ShieldCheck size={12} />
                                Solo lectura
                            </span>
                        </div>
                        <div className="campo-perfil">
                            <label htmlFor="perfil-apellidos">
                                <User size={16} />
                                Apellidos
                            </label>
                            <input id="perfil-apellidos" value={perfil.apellidos} disabled />
                            <span className="dato-protegido">
                                <ShieldCheck size={12} />
                                Solo lectura
                            </span>
                        </div>
                        <div className="campo-perfil">
                            <label htmlFor="perfil-dni">
                                <CreditCard size={16} />
                                {nombreTipoDocumento(perfil.tipoDocumento)} · {nombrePais(perfil.pais)}
                            </label>
                            <input id="perfil-dni" value={perfil.dni} disabled />
                            <span className="dato-protegido">
                                <ShieldCheck size={12} />
                                Solo lectura
                            </span>
                        </div>
                        <div className="campo-perfil">
                            <label htmlFor="perfil-correo">
                                <Mail size={16} />
                                Correo electrónico
                            </label>
                            <input id="perfil-correo" value={perfil.correo} disabled />
                        </div>
                        <div className="campo-perfil">
                            <label htmlFor="perfil-telefono">
                                <Phone size={16} />
                                Teléfono
                            </label>
                            <input
                                id="perfil-telefono"
                                name="telefono"
                                type="tel"
                                maxLength={20}
                                value={
                                    editando
                                        ? formulario.telefono
                                        : perfil.telefono
                                }
                                onChange={cambiarCampo}
                                disabled={!editando}
                                aria-invalid={errores.telefono ? true : undefined}
                            />
                            {editando && (
                                <span className="campo-editable-ayuda">
                                    Puedes modificar este dato
                                </span>
                            )}
                            {errorDe('telefono')}
                        </div>
                        <div className="campo-perfil">
                            <label htmlFor="perfil-direccion">
                                <MapPin size={16} />
                                Dirección
                            </label>
                            <input
                                id="perfil-direccion"
                                name="direccion"
                                maxLength={MAX_DIRECCION}
                                value={
                                    editando
                                        ? formulario.direccion
                                        : perfil.direccion ?? ''
                                }
                                onChange={cambiarCampo}
                                disabled={!editando}
                                aria-invalid={errores.direccion ? true : undefined}
                            />
                            {editando && (
                                <span className="campo-editable-ayuda">
                                    Puedes modificar este dato
                                </span>
                            )}
                            {errorDe('direccion')}
                        </div>
                    </div>
                </section>
                <section className="perfil-tarjeta">
                    <div className="perfil-seccion-titulo">
                        <div>
                            <span>PERFIL PROFESIONAL</span>
                            <h2>Extracto profesional</h2>
                        </div>
                        <FileText size={21} />
                    </div>
                    {editando ? (
                        <>
                            <textarea
                                className="perfil-resumen-input"
                                name="resumenProfesional"
                                aria-label="Extracto profesional"
                                value={
                                    formulario.resumenProfesional
                                }
                                onChange={cambiarCampo}
                                rows={5}
                                maxLength={MAX_RESUMEN}
                                placeholder="Describe brevemente tu experiencia profesional..."
                            />
                            <div className="perfil-contador">
                                {
                                    formulario
                                        .resumenProfesional
                                        .length
                                } / {MAX_RESUMEN}
                            </div>
                            {errorDe('resumenProfesional')}
                        </>
                    ) : (
                        <p className="resumen-texto">
                            {perfil.resumenProfesional || 'Aún no escribiste tu extracto profesional.'}
                        </p>
                    )}
                </section>
                <PerfilProfesionalSeccion puedeEditar={puedeEditar} />
                <DocumentosSeccion
                    documentos={perfil.documentos}
                    puedeEditar={puedeEditar}
                    onCambio={(documentos) => setPerfil((actual) => (actual ? { ...actual, documentos } : actual))}
                />
                <CertificacionesSeccion puedeEditar={puedeEditar} />
                {editando && puedeEditar && (
                    <div className="perfil-acciones-finales">
                        <button type="button" className="boton-cancelar" onClick={cancelarEdicion} disabled={guardando}>
                            <X size={17} />
                            Cancelar
                        </button>
                        <button
                            type="button"
                            className="boton-guardar"
                            onClick={() => void guardarCambios()}
                            disabled={guardando}
                        >
                            <Save size={17} />
                            {guardando ? 'Guardando...' : 'Guardar cambios'}
                        </button>
                    </div>
                )}
            </div>
        </main>
    );
};

export default PostulantePerfil;
