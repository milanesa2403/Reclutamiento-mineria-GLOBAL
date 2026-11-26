import Cargando from '../../components/Cargando';
import React, { useEffect, useState } from 'react';

import {
    Building2,
    MapPin,
    Phone,
    Mail,
    FileText,
    Pickaxe,
    Pencil,
    Save,
    X,
    UserRound,
    CheckCircle2,
    ShieldCheck,
} from 'lucide-react';

import { textoError, erroresPorCampo } from '../../api/client.ts';
import { listarSectoresMineros, type SectorMinero } from '../../api/catalogos.ts';
import {
    actualizarPerfilEmpresa,
    obtenerPerfilEmpresa,
    type ActualizarPerfilEmpresa,
    type PerfilEmpresa,
} from '../../api/empresa.ts';
import { useAuth } from '../../auth/AuthContext.tsx';
import './EmpresaPerfil.css';
import { nombreIdentificadorFiscal, nombrePais, nombreTipoDocumento } from '../../utils/documentos.ts'

interface Formulario {
    idSectorMinero: string;
    telefono: string;
    descripcion: string;
    telefonoRepresentante: string;
    cargoRepresentante: string;
}

const MAX_DESCRIPCION = 1000;
const TELEFONO = /^[+\d][\d\s-]{6,19}$/;

function aFormulario(p: PerfilEmpresa): Formulario {
    return {
        idSectorMinero: p.idSectorMinero ? String(p.idSectorMinero) : '',
        telefono: p.telefono ?? '',
        descripcion: p.descripcion ?? '',
        telefonoRepresentante: p.telefonoRepresentante ?? '',
        cargoRepresentante: p.cargoRepresentante ?? '',
    };
}

const EmpresaPerfil: React.FC = () => {

    const { estado } = useAuth();
    const puedeEditar = estado.tipo === 'autenticado' && estado.sesion.rol === 'EMPRESA'
        && estado.sesion.estadoEmpresa === 'APROBADA';

    const [perfil, setPerfil] = useState<PerfilEmpresa | null>(null);
    const [sectores, setSectores] = useState<SectorMinero[]>([]);
    const [errorCarga, setErrorCarga] = useState<string | null>(null);

    const [formulario, setFormulario] = useState<Formulario | null>(null);
    const [editando, setEditando] = useState(false);
    const [guardando, setGuardando] = useState(false);
    const [mensajeExito, setMensajeExito] = useState('');
    const [mensajeError, setMensajeError] = useState('');
    const [erroresCampo, setErroresCampo] = useState<Record<string, string>>({});

    useEffect(() => {
        obtenerPerfilEmpresa()
            .then((p) => {
                setPerfil(p);
                setFormulario(aFormulario(p));
            })
            .catch((err) => setErrorCarga(textoError(err)));
        listarSectoresMineros()
            .then(setSectores)
            .catch(() => setSectores([]));
    }, []);


    const cambiarCampo = (
        event: React.ChangeEvent<
            HTMLInputElement |
            HTMLTextAreaElement |
            HTMLSelectElement
        >
    ) => {

        const { name, value } = event.target;

        setFormulario((anterior) => anterior && ({
            ...anterior,
            [name]: value,
        }));

        setErroresCampo((anteriores) => {
            const copia = { ...anteriores };
            delete copia[name];
            return copia;
        });
        setMensajeError('');
        setMensajeExito('');
    };

    const iniciarEdicion = () => {
        if (!perfil) return;
        setFormulario(aFormulario(perfil));
        setMensajeExito('');
        setMensajeError('');
        setErroresCampo({});
        setEditando(true);
    };

    const validarFormulario = (f: Formulario): Record<string, string> => {
        const errores: Record<string, string> = {};
        if (!f.idSectorMinero) errores.idSectorMinero = 'Selecciona el sector minero de la empresa.';
        if (f.telefono.trim() && !TELEFONO.test(f.telefono.trim())) {
            errores.telefono = 'Ingresa un teléfono válido, por ejemplo 01 444 5555.';
        }
        if (f.descripcion.length > MAX_DESCRIPCION) {
            errores.descripcion = `Máximo ${MAX_DESCRIPCION} caracteres.`;
        }
        if (!TELEFONO.test(f.telefonoRepresentante.trim())) {
            errores.telefonoRepresentante = 'Ingresa un teléfono válido para el representante.';
        }
        if (!f.cargoRepresentante.trim()) errores.cargoRepresentante = 'El cargo del representante es obligatorio.';
        else if (f.cargoRepresentante.trim().length > 100) errores.cargoRepresentante = 'Máximo 100 caracteres.';
        return errores;
    };

    const guardarCambios = async () => {
        if (!formulario || guardando) return;

        const errores = validarFormulario(formulario);
        setErroresCampo(errores);
        if (Object.keys(errores).length > 0) {
            setMensajeError('Revisa los campos marcados.');
            return;
        }

        const datos: ActualizarPerfilEmpresa = {
            idSectorMinero: Number(formulario.idSectorMinero),
            telefono: formulario.telefono.trim(),
            descripcion: formulario.descripcion.trim(),
            telefonoRepresentante: formulario.telefonoRepresentante.trim(),
            cargoRepresentante: formulario.cargoRepresentante.trim(),
        };

        setGuardando(true);
        setMensajeError('');
        try {
            const actualizado = await actualizarPerfilEmpresa(datos);
            setPerfil(actualizado);
            setFormulario(aFormulario(actualizado));
            setEditando(false);
            setMensajeExito('Perfil actualizado correctamente.');
        } catch (error) {
            setErroresCampo(erroresPorCampo(error));
            setMensajeError(textoError(error));
        } finally {
            setGuardando(false);
        }
    };

    const cancelarEdicion = () => {
        if (perfil) setFormulario(aFormulario(perfil));
        setEditando(false);
        setErroresCampo({});
        setMensajeError('');
        setMensajeExito('');
    };

    if (errorCarga) {
        return (
            <div className="empresa-perfil">
                <div className="empresa-mensaje-error"><span>No pudimos cargar el perfil: {errorCarga}</span></div>
            </div>
        );
    }

    if (!perfil || !formulario) {
        return <div className="empresa-perfil"><Cargando tipo="montana" mensaje="Cargando el perfil de tu empresa" /></div>;
    }

    const valor = (campo: keyof Formulario) => (editando ? formulario[campo] : aFormulario(perfil)[campo]);
    const errorDe = (campo: string) => erroresCampo[campo] && (
        <small className="empresa-mensaje-error" role="alert">{erroresCampo[campo]}</small>
    );
    const nombreRepresentante = `${perfil.nombresRepresentante} ${perfil.apellidosRepresentante}`.trim();

    return (

        <div className="empresa-perfil">
            <section className="empresa-perfil-header">
                <div className="empresa-perfil-header-izquierda">
                    <div className="empresa-perfil-header-icono">
                        <Building2 size={27} />
                    </div>
                    <div>
                        <span className="empresa-perfil-etiqueta">
                            PANEL DE EMPRESA
                        </span>
                        <h1>
                            {editando
                                ? 'Editar perfil de empresa'
                                : 'Mi perfil'}
                        </h1>
                        <p>
                            Administra la información institucional
                            y de contacto de tu empresa.
                        </p>
                    </div>
                </div>
                {puedeEditar && !editando && (
                    <button type="button" className="empresa-boton-editar" onClick={iniciarEdicion}>
                        <Pencil size={16} />

                        Editar perfil
                    </button>
                )}
            </section>
            {mensajeExito && (
                <div className="empresa-mensaje-exito" role="status">
                    <CheckCircle2 size={18} />
                    <span>
                        {mensajeExito}
                    </span>
                </div>
            )}
            {mensajeError && (
                <div className="empresa-mensaje-error" role="alert">
                    <span>
                        {mensajeError}
                    </span>
                </div>
            )}
            <section className="empresa-perfil-card empresa-identidad-principal">
                <div className="empresa-identidad-empresa">
                    <div className="empresa-logo-area">
                        <div className="empresa-logo">
                            <Building2 size={39} />
                        </div>
                    </div>
                    <div className="empresa-identidad-info">
                        <span className="empresa-identidad-mini">
                            PERFIL EMPRESARIAL
                        </span>
                        <h2>
                            {perfil.razonSocial}
                        </h2>
                        <div className="empresa-identidad-detalles">
                            {perfil.sectorMinero && (
                                <span>
                                    <Pickaxe size={15} />
                                    {perfil.sectorMinero}
                                </span>
                            )}
                            {perfil.direccion && (
                                <span>
                                    <MapPin size={15} />
                                    {perfil.direccion}
                                </span>
                            )}
                        </div>
                        <div className="empresa-verificada">
                            <ShieldCheck size={15} />

                            Información corporativa verificada

                        </div>
                    </div>
                </div>
                <div className="empresa-representante-integrado">
                    <div className="empresa-representante-avatar">
                        <UserRound size={24} />
                    </div>
                    <div className="empresa-representante-info">
                        <span>
                            REPRESENTANTE DE LA EMPRESA
                        </span>
                        <strong>
                            {nombreRepresentante}
                        </strong>
                        <p>
                            {perfil.cargoRepresentante}
                        </p>
                    </div>
                </div>
            </section>
            <section className="empresa-perfil-card">
                <div className="empresa-card-titulo">
                    <div className="empresa-card-icono">
                        <Building2 size={19} />
                    </div>
                    <div>
                        <span>
                            DATOS CORPORATIVOS
                        </span>
                        <h2>
                            Información de la empresa
                        </h2>
                    </div>
                </div>
                <div className="empresa-form-grid">
                    <div className="empresa-campo">
                        <div className="empresa-label-linea">
                            <label htmlFor="empresa-ruc">
                                <FileText size={14} />
                                {nombreIdentificadorFiscal(perfil.pais)} · {nombrePais(perfil.pais)}
                            </label>
                            <span className="empresa-etiqueta-protegido">
                                Dato validado
                            </span>
                        </div>
                        <input id="empresa-ruc" value={perfil.ruc} disabled />
                    </div>
                    <div className="empresa-campo">
                        <div className="empresa-label-linea">
                            <label htmlFor="empresa-razon">
                                Razón social
                            </label>
                            <span className="empresa-etiqueta-protegido">
                                Dato validado
                            </span>
                        </div>
                        <input id="empresa-razon" value={perfil.razonSocial} disabled />
                    </div>
                    <div className="empresa-campo">
                        <label htmlFor="empresa-sector">
                            <Pickaxe size={14} />
                            Sector minero
                        </label>
                        <select
                            id="empresa-sector"
                            name="idSectorMinero"
                            value={valor('idSectorMinero')}
                            onChange={cambiarCampo}
                            disabled={!editando}
                        >
                            <option value="">Selecciona un sector</option>
                            {sectores.map((s) => (
                                <option key={s.id} value={String(s.id)}>
                                    {s.nombre}
                                </option>
                            ))}
                            {perfil.idSectorMinero && !sectores.some((s) => s.id === perfil.idSectorMinero) && (
                                <option value={String(perfil.idSectorMinero)}>{perfil.sectorMinero}</option>
                            )}
                        </select>
                        {editando && (
                            <small className="empresa-campo-editable-texto">
                                Campo editable
                            </small>
                        )}
                        {errorDe('idSectorMinero')}
                    </div>
                    <div className="empresa-campo">
                        <div className="empresa-label-linea">
                            <label htmlFor="empresa-direccion">
                                <MapPin size={14} />
                                Dirección fiscal
                            </label>
                            <span className="empresa-etiqueta-protegido">
                                Dato validado
                            </span>
                        </div>
                        <input id="empresa-direccion" value={perfil.direccion ?? ''} disabled />
                    </div>
                </div>
            </section>
            <section className="empresa-perfil-card">
                <div className="empresa-card-titulo">
                    <div className="empresa-card-icono">
                        <Phone size={19} />
                    </div>
                    <div>
                        <span>
                            CONTACTO
                        </span>
                        <h2>
                            Contacto de la empresa
                        </h2>
                    </div>
                </div>
                <div className="empresa-form-grid">
                    <div className="empresa-campo">
                        <label htmlFor="empresa-telefono">
                            <Phone size={14} />
                            Teléfono empresarial
                        </label>
                        <input
                            id="empresa-telefono"
                            type="tel"
                            name="telefono"
                            value={valor('telefono')}
                            onChange={cambiarCampo}
                            disabled={!editando}
                            maxLength={20}
                        />
                        {editando && (
                            <small className="empresa-campo-editable-texto">
                                Campo editable
                            </small>
                        )}
                        {errorDe('telefono')}
                    </div>
                    <div className="empresa-campo">
                        <div className="empresa-label-linea">
                            <label htmlFor="empresa-correo">
                                <Mail size={14} />
                                Correo corporativo
                            </label>
                            <span className="empresa-etiqueta-protegido">
                                Protegido
                            </span>
                        </div>
                        <input id="empresa-correo" type="email" value={perfil.correoCorporativo} disabled />
                    </div>
                </div>
            </section>
            <section className="empresa-perfil-card">
                <div className="empresa-card-titulo">
                    <div className="empresa-card-icono">
                        <UserRound size={19} />
                    </div>
                    <div>
                        <span>
                            REPRESENTANTE
                        </span>
                        <h2>
                            Persona que representa a la empresa
                        </h2>
                    </div>
                </div>
                <div className="empresa-form-grid">
                    <div className="empresa-campo">
                        <div className="empresa-label-linea">
                            <label htmlFor="representante-nombre">
                                Nombres y apellidos
                            </label>
                            <span className="empresa-etiqueta-protegido">
                                Dato registrado
                            </span>
                        </div>
                        <input
                            id="representante-nombre"
                            value={`${nombreRepresentante} · ${nombreTipoDocumento(perfil.tipoDocumentoRepresentante)} ${perfil.dniRepresentante}`}
                            disabled
                        />
                    </div>
                    <div className="empresa-campo">
                        <label htmlFor="representante-cargo">
                            Cargo
                        </label>
                        <input
                            id="representante-cargo"
                            name="cargoRepresentante"
                            value={valor('cargoRepresentante')}
                            onChange={cambiarCampo}
                            disabled={!editando}
                            maxLength={100}
                        />
                        {errorDe('cargoRepresentante')}
                    </div>
                    <div className="empresa-campo">
                        <label htmlFor="representante-telefono">
                            <Phone size={14} />
                            Teléfono del representante
                        </label>
                        <input
                            id="representante-telefono"
                            type="tel"
                            name="telefonoRepresentante"
                            value={valor('telefonoRepresentante')}
                            onChange={cambiarCampo}
                            disabled={!editando}
                            maxLength={20}
                        />
                        {errorDe('telefonoRepresentante')}
                    </div>
                </div>
            </section>
            <section className="empresa-perfil-card empresa-sobre-card">
                <div className="empresa-card-titulo">
                    <div className="empresa-card-icono">
                        <FileText size={19} />
                    </div>
                    <div>
                        <span>
                            PERFIL INSTITUCIONAL
                        </span>
                        <h2>
                            Sobre la empresa
                        </h2>
                    </div>
                </div>
                <div className="empresa-campo">
                    <label htmlFor="empresa-descripcion">
                        Descripción institucional
                    </label>
                    <textarea
                        id="empresa-descripcion"
                        name="descripcion"
                        value={valor('descripcion')}
                        onChange={cambiarCampo}
                        disabled={!editando}
                        rows={3}
                        maxLength={MAX_DESCRIPCION}
                        placeholder={editando ? '' : 'Aún no hay una descripción.'}
                    />
                    {editando && (
                        <div className="empresa-descripcion-ayuda">
                            <small>
                                Resume la actividad y propuesta
                                institucional de la empresa.
                            </small>
                            <small>
                                {formulario.descripcion.length}/{MAX_DESCRIPCION}
                            </small>
                        </div>
                    )}
                    {errorDe('descripcion')}
                </div>
            </section>
            {puedeEditar && editando && (
                <div className="empresa-acciones-finales">
                    <button
                        type="button"
                        className="empresa-boton-cancelar"
                        onClick={cancelarEdicion}
                        disabled={guardando}
                    >
                        <X size={17} />

                        Cancelar
                    </button>
                    <button
                        type="button"
                        className="empresa-boton-guardar"
                        onClick={() => void guardarCambios()}
                        disabled={guardando}
                    >
                        <Save size={17} />
                        {guardando ? 'Guardando...' : 'Guardar cambios'}
                    </button>
                </div>
            )}
        </div>
    );
};

export default EmpresaPerfil;
