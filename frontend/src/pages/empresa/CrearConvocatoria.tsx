import Cargando from '../../components/Cargando';
import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    BriefcaseBusiness,
    CalendarDays,
    ChevronLeft,
    ChevronRight,
    CircleCheck,
    ClipboardList,
    Eye,
    FilePenLine,
    FileText,
    Folder,
    FolderInput,
    FolderOpen,
    FolderPlus,
    Lock,
    Pencil,
    Plus,
    Send,
    Trash2,
    Users,
    X,
} from 'lucide-react';
import {
    actualizarConvocatoria,
    cerrarConvocatoria,
    cambiarFechaCierre,
    crearBusqueda,
    crearConvocatoria,
    eliminarBusqueda,
    eliminarConvocatoria as eliminarConvocatoriaApi,
    listarBusquedas,
    listarMisConvocatorias,
    moverConvocatoria,
    publicarConvocatoria,
    renombrarBusqueda,
    type Busqueda,
    type Convocatoria,
    type ConvocatoriaRequest,
    type TipoConvocatoria,
} from '../../api/convocatorias';
import { listarTiposCertificacion, type TipoCertificacion } from '../../api/certificaciones';
import { formatearUbicacion } from '../../api/ubigeo';
import { errorFormulario } from '../../api/client';
import SelectorUbigeo, { UBIGEO_VACIO, ubigeoDesdeDistrito, type ValorUbigeo } from '../../components/SelectorUbigeo';
import './CrearConvocatoria.css';

interface CertificadoFormulario {
    idCatalogoCertificacion: number | null;
    requierePdf: boolean;
}

interface FormularioConvocatoria {
    puesto: string;
    tipo: TipoConvocatoria;
    ubigeo: ValorUbigeo;
    direccion: string;
    referencia: string;
    campamento: string;
    turno: string;
    descripcion: string;
    experiencia: string;
    requisitosObligatorios: string;
    requisitosDeseables: string;
    certificados: CertificadoFormulario[];
    fechaCierre: string;
    fechaInicio: string;
    fechaFin: string;
    vacantes: string;
    idBusqueda: number | null;
}

const formularioInicial: FormularioConvocatoria = {
    puesto: '',
    tipo: 'PLANILLA_COMPLETA',
    ubigeo: UBIGEO_VACIO,
    direccion: '',
    referencia: '',
    campamento: '',
    turno: '',
    descripcion: '',
    experiencia: '',
    requisitosObligatorios: '',
    requisitosDeseables: '',
    certificados: [],
    fechaCierre: '',
    fechaInicio: '',
    fechaFin: '',
    vacantes: '1',
    idBusqueda: null,
};

const aFormulario = (c: Convocatoria): FormularioConvocatoria => ({
    puesto: c.puesto ?? '',
    tipo: c.tipo,
    ubigeo: ubigeoDesdeDistrito(c.ubicacion?.codigo),
    direccion: c.direccion ?? '',
    referencia: c.referencia ?? '',
    campamento: c.campamento ?? '',
    turno: c.turno ?? '',
    descripcion: c.descripcion ?? '',
    experiencia: c.experienciaMinimaAnios === null ? '' : String(c.experienciaMinimaAnios),
    requisitosObligatorios: c.requisitosObligatorios ?? '',
    requisitosDeseables: c.requisitosDeseables ?? '',
    certificados: c.certificadosRequeridos.map(({ idCatalogoCertificacion, requierePdf }) => ({
        idCatalogoCertificacion,
        requierePdf,
    })),
    fechaCierre: c.fechaCierre ?? '',
    fechaInicio: c.fechaInicioLabores ?? '',
    fechaFin: c.fechaFinLabores ?? '',
    vacantes: String(c.vacantes ?? 1),
    idBusqueda: c.busqueda?.id ?? null,
});

const plantillaDePuesto = (base: Convocatoria | undefined, idBusqueda: number): FormularioConvocatoria => {
    if (!base) return { ...formularioInicial, idBusqueda };
    const datos = aFormulario(base);
    return {
        ...datos,
        puesto: '',
        descripcion: '',
        experiencia: '',
        requisitosObligatorios: '',
        requisitosDeseables: '',
        fechaCierre: datos.fechaCierre && datos.fechaCierre >= obtenerFechaActual() ? datos.fechaCierre : '',
        vacantes: '1',
        idBusqueda,
    };
};

const vacioANull = (valor: string): string | null => (valor.trim() === '' ? null : valor.trim());

const aRequest = (f: FormularioConvocatoria): ConvocatoriaRequest => ({
    tipo: f.tipo,
    puesto: f.puesto.trim(),
    campamento: f.campamento.trim(),
    turno: f.turno.trim(),
    descripcion: f.descripcion.trim(),
    requisitosObligatorios: f.requisitosObligatorios.trim(),
    requisitosDeseables: vacioANull(f.requisitosDeseables),
    experienciaMinimaAnios: f.experiencia === '' ? null : Number(f.experiencia),
    fechaInicioLabores: f.tipo === 'PARADA_PLANTA' ? vacioANull(f.fechaInicio) : null,
    fechaFinLabores: f.tipo === 'PARADA_PLANTA' ? vacioANull(f.fechaFin) : null,
    fechaCierre: vacioANull(f.fechaCierre),
    codigoUbigeo: f.ubigeo.distrito,
    direccion: vacioANull(f.direccion),
    referencia: vacioANull(f.referencia),
    certificadosRequeridos: f.certificados.flatMap((c) =>
        c.idCatalogoCertificacion === null
            ? []
            : [{ idCatalogoCertificacion: c.idCatalogoCertificacion, requierePdf: c.requierePdf }]
    ),
    vacantes: f.vacantes === '' ? 1 : Number(f.vacantes),
    idBusqueda: f.idBusqueda,
});

const obtenerFechaActual = (): string => {
    const hoy = new Date();
    const mes = String(hoy.getMonth() + 1).padStart(2, '0');
    const dia = String(hoy.getDate()).padStart(2, '0');
    return `${hoy.getFullYear()}-${mes}-${dia}`;
};

const convertirFecha = (valor: string): Date | null => {
    if (!valor) return null;
    const partes = valor.split('-').map(Number);
    if (partes.length !== 3) return null;
    const [anio, mes, dia] = partes;
    const fecha = new Date(anio, mes - 1, dia);
    if (fecha.getFullYear() !== anio || fecha.getMonth() !== mes - 1 || fecha.getDate() !== dia) return null;
    return fecha;
};

const mostrarFecha = (valor: string): string => {
    const fecha = convertirFecha(valor);
    if (!fecha) return 'No especificada';
    return fecha.toLocaleDateString('es-PE', { day: '2-digit', month: 'long', year: 'numeric' });
};

const nombreTipo = (tipo: TipoConvocatoria): string => (tipo === 'PARADA_PLANTA' ? 'Parada de planta' : 'Planilla completa');

const SIN_CAMPAMENTO = 'No aplica';

interface CalendarioProps {
    etiqueta: string;
    valor: string;
    onChange: (valor: string) => void;
    minFecha?: string;
    obligatorio?: boolean;
}

const CalendarioPersonalizado: React.FC<CalendarioProps> = ({ etiqueta, valor, onChange, minFecha, obligatorio = false }) => {
    const [abierto, setAbierto] = useState(false);
    const referencia = useRef<HTMLDivElement>(null);
    const fechaSeleccionada = convertirFecha(valor);
    const [mesVisible, setMesVisible] = useState<Date>(fechaSeleccionada ?? new Date());

    useEffect(() => {
        if (fechaSeleccionada) {
            setMesVisible(new Date(fechaSeleccionada.getFullYear(), fechaSeleccionada.getMonth(), 1));
        }
    }, [valor]);

    useEffect(() => {
        if (!abierto) return;
        const cerrarFuera = (evento: MouseEvent) => {
            if (referencia.current && !referencia.current.contains(evento.target as Node)) setAbierto(false);
        };
        const cerrarEscape = (evento: KeyboardEvent) => {
            if (evento.key === 'Escape') setAbierto(false);
        };
        document.addEventListener('mousedown', cerrarFuera);
        document.addEventListener('keydown', cerrarEscape);
        return () => {
            document.removeEventListener('mousedown', cerrarFuera);
            document.removeEventListener('keydown', cerrarEscape);
        };
    }, [abierto]);

    const anioVisible = mesVisible.getFullYear();
    const mesNumero = mesVisible.getMonth();
    const cambiarMes = (cantidad: number) => setMesVisible(new Date(anioVisible, mesNumero + cantidad, 1));
    const desplazamiento = (new Date(anioVisible, mesNumero, 1).getDay() + 6) % 7;
    const cantidadDias = new Date(anioVisible, mesNumero + 1, 0).getDate();
    const celdas: Array<number | null> = [
        ...Array.from({ length: desplazamiento }, () => null),
        ...Array.from({ length: cantidadDias }, (_, indice) => indice + 1),
    ];
    const fechaMinima = convertirFecha(minFecha ?? '');
    const tituloMes = mesVisible.toLocaleDateString('es-PE', { month: 'long', year: 'numeric' });

    const elegirDia = (dia: number) => {
        onChange(`${anioVisible}-${String(mesNumero + 1).padStart(2, '0')}-${String(dia).padStart(2, '0')}`);
        setAbierto(false);
    };

    return (
        <div className="cv-campo cv-calendario-campo" ref={referencia}>
            <label>
                {etiqueta}{' '}
                {obligatorio && <span className="cv-obligatorio">*</span>}
            </label>
            <button
                type="button"
                className={`cv-fecha-boton ${abierto ? 'cv-fecha-activa' : ''}`}
                onClick={() => setAbierto(!abierto)}
                aria-expanded={abierto}
                aria-label={`Seleccionar ${etiqueta}`}
            >
                <span className={valor ? '' : 'cv-fecha-placeholder'}>
                    {valor ? mostrarFecha(valor) : 'Seleccionar fecha'}
                </span>
                <CalendarDays size={19} />
            </button>
            {abierto && (
                <div className="cv-calendario">
                    <div className="cv-calendario-encabezado">
                        <button type="button" onClick={() => cambiarMes(-1)} aria-label="Mes anterior">
                            <ChevronLeft size={20} />
                        </button>
                        <strong>{tituloMes}</strong>
                        <button type="button" onClick={() => cambiarMes(1)} aria-label="Mes siguiente">
                            <ChevronRight size={20} />
                        </button>
                    </div>
                    <div className="cv-calendario-semana">
                        {['Lu', 'Ma', 'Mi', 'Ju', 'Vi', 'Sa', 'Do'].map((dia) => <span key={dia}>{dia}</span>)}
                    </div>
                    <div className="cv-calendario-dias">
                        {celdas.map((dia, indice) => {
                            if (dia === null) return <span key={`vacio-${indice}`} />;
                            const fechaDia = new Date(anioVisible, mesNumero, dia);
                            const seleccionado = fechaSeleccionada?.getTime() === fechaDia.getTime();
                            const esHoy = convertirFecha(obtenerFechaActual())?.getTime() === fechaDia.getTime();
                            return (
                                <button
                                    type="button"
                                    key={`${anioVisible}-${mesNumero}-${dia}`}
                                    disabled={fechaMinima !== null && fechaDia < fechaMinima}
                                    className={[seleccionado ? 'cv-dia-seleccionado' : '', esHoy ? 'cv-dia-hoy' : ''].join(' ')}
                                    onClick={() => elegirDia(dia)}
                                    aria-label={`${dia} de ${tituloMes}`}
                                    aria-pressed={seleccionado}
                                >
                                    {dia}
                                </button>
                            );
                        })}
                    </div>
                    <div className="cv-calendario-pie">
                        <button type="button" onClick={() => setMesVisible(new Date())}>
                            Ir al mes actual
                        </button>
                        {valor && (
                            <button
                                type="button"
                                onClick={() => {
                                    onChange('');
                                    setAbierto(false);
                                }}
                            >
                                Limpiar
                            </button>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
};

interface ModalConfirmarProps {
    etiqueta: string;
    Icono: typeof Trash2;
    titulo: string;
    texto: string;
    textoBoton: string;
    enviando: boolean;
    onCancelar: () => void;
    onConfirmar: () => void;
}

const ModalConfirmar: React.FC<ModalConfirmarProps> = ({
    etiqueta, Icono, titulo, texto, textoBoton, enviando, onCancelar, onConfirmar,
}) => (
    <div className="cv-modal-fondo">
        <div className="cv-modal cv-modal-eliminar" role="alertdialog" aria-modal="true" aria-label={etiqueta}>
            <div className="cv-eliminar-icono">
                <Icono size={28} />
            </div>
            <h2>{titulo}</h2>
            <p>{texto}</p>
            <div className="cv-modal-acciones">
                <button type="button" className="cv-boton cv-boton-cancelar" onClick={onCancelar} disabled={enviando}>
                    Cancelar
                </button>
                <button type="button" className="cv-boton cv-boton-eliminar" onClick={onConfirmar} disabled={enviando}>
                    <Icono size={17} />
                    {textoBoton}
                </button>
            </div>
        </div>
    </div>
);

const Etiqueta: React.FC<{ id: string; obligatorio?: boolean; children: React.ReactNode }> = ({
    id, obligatorio = true, children,
}) => (
    <label htmlFor={id}>
        {children}{' '}
        {obligatorio && <span className="cv-obligatorio">*</span>}
    </label>
);

const CrearConvocatoria: React.FC = () => {
    const navigate = useNavigate();

    const [convocatorias, setConvocatorias] = useState<Convocatoria[]>([]);
    const [cargando, setCargando] = useState(true);
    const [errorCarga, setErrorCarga] = useState('');
    const [tiposCertificacion, setTiposCertificacion] = useState<TipoCertificacion[]>([]);
    const [formulario, setFormulario] = useState<FormularioConvocatoria>(formularioInicial);
    const [mostrarFormulario, setMostrarFormulario] = useState(false);
    const [editandoId, setEditandoId] = useState<number | null>(null);
    const [resumen, setResumen] = useState<Convocatoria | null>(null);
    const [eliminarId, setEliminarId] = useState<number | null>(null);
    const [cerrarId, setCerrarId] = useState<number | null>(null);
    const [modalFecha, setModalFecha] = useState<{ convocatoria: Convocatoria; fecha: string } | null>(null);
    const [errorFecha, setErrorFecha] = useState('');
    const [mensaje, setMensaje] = useState('');
    const [error, setError] = useState('');
    const [enviando, setEnviando] = useState(false);
    const [pestanaActiva, setPestanaActiva] = useState<'sueltas' | 'busquedas'>('sueltas');
    const [busquedas, setBusquedas] = useState<Busqueda[]>([]);
    const [carpetasAbiertas, setCarpetasAbiertas] = useState<Set<number>>(new Set());
    const [menuCrear, setMenuCrear] = useState(false);
    const [modalBusqueda, setModalBusqueda] = useState<{ id: number | null; nombre: string } | null>(null);
    const [errorBusqueda, setErrorBusqueda] = useState('');
    const [eliminarBusquedaId, setEliminarBusquedaId] = useState<number | null>(null);
    const [filtroEstado, setFiltroEstado] = useState<'TODAS' | 'PUBLICADA' | 'CERRADA' | 'BORRADOR'>('TODAS');

    useEffect(() => {
        listarTiposCertificacion().then(setTiposCertificacion).catch(() => setTiposCertificacion([]));
    }, []);

    const cargar = useCallback(async () => {
        setErrorCarga('');
        try {
            const [lista, carpetas] = await Promise.all([listarMisConvocatorias(), listarBusquedas()]);
            setConvocatorias(lista);
            setBusquedas(carpetas);
        } catch (e) {
            setErrorCarga(errorFormulario(e));
        } finally {
            setCargando(false);
        }
    }, []);

    useEffect(() => {
        void cargar();
    }, [cargar]);

    const pasaFiltro = (c: Convocatoria) => filtroEstado === 'TODAS' || c.estado === filtroEstado;
    const sueltas = convocatorias.filter((c) => !c.busqueda && pasaFiltro(c));
    const abrirCarpeta = (id: number) => setCarpetasAbiertas((a) => new Set(a).add(id));

    const alternarCarpeta = (id: number) =>
        setCarpetasAbiertas((actual) => {
            const nuevo = new Set(actual);
            if (nuevo.has(id)) nuevo.delete(id);
            else nuevo.add(id);
            return nuevo;
        });

    const guardarBusqueda = async () => {
        if (!modalBusqueda || enviando) return;
        const nombre = modalBusqueda.nombre.trim();
        if (!nombre) {
            setErrorBusqueda('Escribe un nombre, por ejemplo «Parada octubre 2026».');
            return;
        }
        setEnviando(true);
        try {
            if (modalBusqueda.id === null) {
                const creada = await crearBusqueda(nombre);
                abrirCarpeta(creada.id);
                setMensaje(`Búsqueda «${creada.nombre}» creada. Agrégale puestos.`);
            } else {
                await renombrarBusqueda(modalBusqueda.id, nombre);
                setMensaje('Búsqueda renombrada.');
            }
            setModalBusqueda(null);
            await cargar();
        } catch (e) {
            setErrorBusqueda(errorFormulario(e));
        } finally {
            setEnviando(false);
        }
    };

    const ejecutar = async (accion: () => Promise<unknown>, exito: string, alTerminar: () => void, cerrarResumen = true) => {
        setEnviando(true);
        try {
            await accion();
            await cargar();
            if (cerrarResumen) setResumen(null);
            setMensaje(exito);
        } catch (e) {
            setErrorCarga(errorFormulario(e));
        } finally {
            alTerminar();
            setEnviando(false);
        }
    };

    const confirmarEliminarBusqueda = async () => {
        if (!eliminarBusquedaId || enviando) return;
        await ejecutar(() => eliminarBusqueda(eliminarBusquedaId),
            'Carpeta eliminada. Sus convocatorias quedaron como sueltas.', () => setEliminarBusquedaId(null), false);
    };

    const confirmarCierre = async () => {
        if (!cerrarId || enviando) return;
        await ejecutar(() => cerrarConvocatoria(cerrarId),
            'Convocatoria cerrada. Ya no recibe postulaciones; las recibidas se conservan.', () => setCerrarId(null));
    };

    const eliminarConvocatoria = async () => {
        if (!eliminarId || enviando) return;
        await ejecutar(() => eliminarConvocatoriaApi(eliminarId),
            'Convocatoria eliminada correctamente.', () => setEliminarId(null));
    };

    const abrirFormulario = (datos: FormularioConvocatoria, id: number | null = null) => {
        setFormulario(datos);
        setEditandoId(id);
        setError('');
        setMensaje('');
        setMostrarFormulario(true);
    };

    const agregarPuesto = (b: Busqueda, puestos: Convocatoria[]) => {
        abrirFormulario(plantillaDePuesto(puestos[0], b.id));
        window.scrollTo({ top: 0, behavior: 'smooth' });
    };

    const nuevaConvocatoria = () => abrirFormulario(formularioInicial);

    const editarConvocatoria = (convocatoria: Convocatoria) => {
        if (convocatoria.estado !== 'BORRADOR') return;
        abrirFormulario(aFormulario(convocatoria), convocatoria.id);
        setResumen(null);
        window.scrollTo({ top: 0, behavior: 'smooth' });
    };

    const mover = async (c: Convocatoria, idBusqueda: number | null) => {
        try {
            await moverConvocatoria(c.id, idBusqueda);
            if (idBusqueda) abrirCarpeta(idBusqueda);
            await cargar();
            setMensaje(idBusqueda ? `«${c.puesto}» se movió a la búsqueda.` : `«${c.puesto}» ahora es una convocatoria suelta.`);
        } catch (e) {
            setErrorCarga(errorFormulario(e));
        }
    };

    const actualizarCampo = <K extends keyof FormularioConvocatoria>(campo: K, valor: FormularioConvocatoria[K]) => {
        setFormulario((anterior) => ({ ...anterior, [campo]: valor }));
        setError('');
    };

    const alEscribir = (campo: 'puesto' | 'direccion' | 'referencia' | 'campamento' | 'turno' | 'descripcion'
        | 'requisitosObligatorios' | 'requisitosDeseables') =>
        (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => actualizarCampo(campo, e.target.value);

    const limpiarFormulario = () => {
        setFormulario(formularioInicial);
        setEditandoId(null);
        setMostrarFormulario(false);
        setError('');
    };

    const validarPublicacion = (): string | null => {
        if (!formulario.puesto.trim()) return 'Ingresa el puesto de trabajo.';
        if (!/^\d+$/.test(formulario.vacantes) || Number(formulario.vacantes) < 1) return 'Indica al menos 1 vacante.';
        if (!formulario.ubigeo.distrito) return 'Elige el departamento, la provincia y el distrito.';
        if (!formulario.campamento.trim()) return 'Ingresa el campamento o marca «No requiere campamento».';
        if (!formulario.turno.trim()) {
            return formulario.tipo === 'PARADA_PLANTA'
                ? 'Ingresa el turno y horario de la parada.'
                : 'Ingresa el régimen de trabajo.';
        }
        if (!formulario.descripcion.trim()) return 'Ingresa la descripción del puesto.';
        if (!formulario.experiencia.trim()) return 'Ingresa los años de experiencia.';
        if (!/^\d+$/.test(formulario.experiencia)) return 'Los años de experiencia deben ser números enteros.';
        if (!formulario.requisitosObligatorios.trim()) return 'Ingresa los requisitos obligatorios.';
        if (!formulario.fechaCierre) return 'Selecciona la fecha de cierre.';
        if (formulario.fechaCierre < obtenerFechaActual()) return 'La fecha de cierre no puede ser anterior a hoy.';
        if (formulario.tipo === 'PARADA_PLANTA') {
            if (!formulario.fechaInicio || !formulario.fechaFin) {
                return 'Selecciona las fechas de inicio y fin de la parada de planta.';
            }
            if (formulario.fechaFin < formulario.fechaInicio) return 'La fecha de fin no puede ser anterior a la fecha de inicio.';
            if (!formulario.direccion.trim()) {
                return 'Ingresa la dirección de la planta (obligatoria en una parada de planta).';
            }
        }
        const tipos = formulario.certificados.map((c) => c.idCatalogoCertificacion).filter((id) => id !== null);
        if (new Set(tipos).size !== tipos.length) return 'Hay certificados repetidos en la lista.';
        return null;
    };

    const validarBorrador = (): string | null => {
        if (!formulario.puesto.trim()) return 'Ingresa el puesto de trabajo.';
        if (!/^\d+$/.test(formulario.vacantes) || Number(formulario.vacantes) < 1) return 'Indica al menos 1 vacante.';
        if (!formulario.ubigeo.distrito) return 'Elige el departamento, la provincia y el distrito.';
        if (!formulario.campamento.trim()) return 'Ingresa el campamento.';
        if (!formulario.turno.trim()) return 'Ingresa el turno de trabajo.';
        if (!formulario.descripcion.trim()) return 'Ingresa la descripción del puesto.';
        if (!formulario.requisitosObligatorios.trim()) return 'Ingresa los requisitos obligatorios.';
        return null;
    };

    const cambiarCertificados = (cambio: (certificados: CertificadoFormulario[]) => CertificadoFormulario[]) =>
        setFormulario((anterior) => ({ ...anterior, certificados: cambio(anterior.certificados) }));

    const actualizarCertificado = (indice: number, cambio: Partial<CertificadoFormulario>) => {
        cambiarCertificados((lista) => lista.map((c, i) => (i === indice ? { ...c, ...cambio } : c)));
        setError('');
    };

    const guardar = async (publicar: boolean) => {
        if (enviando) return;
        setError('');
        setMensaje('');
        const mensajeError = publicar ? validarPublicacion() : validarBorrador();
        if (mensajeError) {
            setError(mensajeError);
            return;
        }
        setEnviando(true);
        try {
            const datos = aRequest(formulario);
            const guardada = editandoId ? await actualizarConvocatoria(editandoId, datos) : await crearConvocatoria(datos);
            if (publicar) {
                try {
                    await publicarConvocatoria(guardada.id);
                } catch (e) {
                    setEditandoId(guardada.id);
                    setError(`Se guardó como borrador, pero no se pudo publicar: ${errorFormulario(e)}`);
                    void cargar();
                    return;
                }
            }
            limpiarFormulario();
            await cargar();
            setMensaje(
                publicar
                    ? 'Convocatoria publicada. Ya es visible para los postulantes.'
                    : editandoId
                        ? 'Cambios guardados correctamente.'
                        : 'Borrador guardado. No es visible hasta que lo publiques.'
            );
        } catch (e) {
            setError(errorFormulario(e));
        } finally {
            setEnviando(false);
        }
    };

    const confirmarFecha = async () => {
        if (!modalFecha || enviando) return;
        if (!modalFecha.fecha || modalFecha.fecha < obtenerFechaActual()) {
            setErrorFecha('La fecha de cierre debe ser hoy o posterior.');
            return;
        }
        const reabre = modalFecha.convocatoria.estado === 'CERRADA';
        setEnviando(true);
        try {
            await cambiarFechaCierre(modalFecha.convocatoria.id, modalFecha.fecha);
            await cargar();
            setResumen(null);
            setModalFecha(null);
            setMensaje(reabre
                ? 'Convocatoria reabierta: vuelve a recibir postulaciones hasta la nueva fecha.'
                : 'Fecha de cierre actualizada.');
        } catch (e) {
            setErrorFecha(errorFormulario(e));
        } finally {
            setEnviando(false);
        }
    };

    const verPostulantes = (convocatoria: Convocatoria) =>
        navigate(`/empresa?modulo=postulantes&convocatoria=${encodeURIComponent(convocatoria.id)}`);

    const contarEstado = (estado: Convocatoria['estado']) => convocatorias.filter((item) => item.estado === estado).length;

    const etiquetaEstado = (c: Convocatoria) => {
        if (c.estado === 'BORRADOR') return 'Borrador';
        if (c.estado === 'PUBLICADA') return 'Publicada';
        if ((c.postulacionesPendientes ?? 0) > 0) return `Cerrada · en evaluación (${c.postulacionesPendientes} pendiente(s))`;
        return (c.totalPostulaciones ?? 0) > 0 ? 'Finalizada' : 'Cerrada';
    };

    const selectorCarpeta = (valor: number | null | undefined, onCambiar: (id: number | null) => void, props: object) => (
        <select value={valor ?? ''} onChange={(e) => onCambiar(e.target.value === '' ? null : Number(e.target.value))} {...props}>
            <option value="">Sin carpeta (suelta)</option>
            {busquedas.map((b) => <option key={b.id} value={b.id}>{b.nombre}</option>)}
        </select>
    );

    const tarjeta = (c: Convocatoria) => (
        <div className="cv-oferta" key={c.id}>
            <div className="cv-oferta-superior">
                <div>
                    <h3>{c.puesto || 'Convocatoria sin título'}</h3>
                    <p>{nombreTipo(c.tipo)} · {formatearUbicacion(c.ubicacion) || 'Ubicación pendiente'}</p>
                </div>
                <span className={`cv-estado cv-estado-${c.estado.toLowerCase()}`}>{etiquetaEstado(c)}</span>
            </div>
            <div className="cv-oferta-datos">
                <span>
                    <CalendarDays size={16} />
                    Cierre: {mostrarFecha(c.fechaCierre ?? '')}
                </span>
                <span>
                    <BriefcaseBusiness size={16} />
                    {c.experienciaMinimaAnios ?? 0} años de experiencia
                </span>
                <span>
                    <Users size={16} />
                    {c.estado !== 'BORRADOR' ? `${c.totalPostulaciones ?? 0} postulante(s) · ` : ''}
                    {c.vacantes ?? 1} vacante(s)
                </span>
                {c.certificadosRequeridos.length > 0 && (
                    <span>
                        <FileText size={16} />
                        {c.certificadosRequeridos.length} certificado(s) exigido(s)
                    </span>
                )}
                {busquedas.length > 0 && (
                    <label className="cv-mover">
                        <FolderInput size={16} />
                        {selectorCarpeta(c.busqueda?.id, (id) => void mover(c, id), { 'aria-label': 'Carpeta de la convocatoria' })}
                    </label>
                )}
            </div>
            <div className="cv-oferta-acciones">
                {c.estado !== 'BORRADOR' && (
                    <button type="button" className="cv-boton cv-boton-primario" onClick={() => verPostulantes(c)}>
                        <Users size={17} />
                        Ver postulantes
                    </button>
                )}
                <button type="button" className="cv-boton cv-boton-suave" onClick={() => setResumen(c)}>
                    <Eye size={17} />
                    Ver resumen
                </button>
                {c.estado === 'PUBLICADA' && (
                    <button type="button" className="cv-boton cv-boton-suave" onClick={() => setCerrarId(c.id)}>
                        <Lock size={16} />
                        Cerrar
                    </button>
                )}
                {c.estado !== 'BORRADOR' && (
                    <button
                        type="button"
                        className="cv-boton cv-boton-suave"
                        onClick={() => {
                            setErrorFecha('');
                            setModalFecha({ convocatoria: c, fecha: '' });
                        }}
                    >
                        <CalendarDays size={16} />
                        {c.estado === 'CERRADA' ? 'Reabrir' : 'Cambiar fecha'}
                    </button>
                )}
                {c.estado !== 'BORRADOR' && (c.totalPostulaciones ?? 0) === 0 && (
                    <button type="button" className="cv-boton cv-boton-peligro" onClick={() => setEliminarId(c.id)}>
                        <Trash2 size={17} />
                        Eliminar
                    </button>
                )}
                {c.estado === 'BORRADOR' && (
                    <>
                        <button type="button" className="cv-boton cv-boton-suave" onClick={() => editarConvocatoria(c)}>
                            <FilePenLine size={17} />
                            Editar
                        </button>
                        <button type="button" className="cv-boton cv-boton-publicar" onClick={() => editarConvocatoria(c)}>
                            <Send size={16} />
                            Publicar
                        </button>
                        <button type="button" className="cv-boton cv-boton-peligro" onClick={() => setEliminarId(c.id)}>
                            <Trash2 size={17} />
                            Eliminar
                        </button>
                    </>
                )}
            </div>
        </div>
    );

    const datosResumen = (r: Convocatoria): Array<[string, React.ReactNode] | false> => [
        ['Ubicación', formatearUbicacion(r.ubicacion) || 'No especificada'],
        !!r.direccion && ['Dirección', `${r.direccion}${r.referencia ? ` (${r.referencia})` : ''}`],
        ['Campamento', r.campamento || 'No especificado'],
        ['Turno', r.turno || 'No especificado'],
        ['Experiencia', r.experienciaMinimaAnios !== null ? `${r.experienciaMinimaAnios} años` : 'No especificada'],
        ['Fecha de cierre', mostrarFecha(r.fechaCierre ?? '')],
        ['Vacantes', r.vacantes ?? 1],
        !!r.busqueda && ['Búsqueda', r.busqueda.nombre],
        r.estado !== 'BORRADOR' && ['Postulantes', r.totalPostulaciones ?? 0],
        ['Fecha de creación', new Date(r.fechaCreacion).toLocaleDateString('es-PE')],
        r.tipo === 'PARADA_PLANTA' && ['Inicio de labores', mostrarFecha(r.fechaInicioLabores ?? '')],
        r.tipo === 'PARADA_PLANTA' && ['Fin de labores', mostrarFecha(r.fechaFinLabores ?? '')],
    ];

    const esParada = formulario.tipo === 'PARADA_PLANTA';

    return (
        <div className="cv-contenedor">
            <div className="cv-cabecera">
                <div>
                    <div className="cv-titulo-linea">
                        <BriefcaseBusiness size={27} />
                        <h1>Mis convocatorias</h1>
                    </div>
                    <p>Administra las oportunidades laborales de tu empresa minera.</p>
                </div>
                {!mostrarFormulario && (
                    <div className="cv-crear-menu">
                        <button
                            type="button"
                            className="cv-boton cv-boton-primario"
                            onClick={() => setMenuCrear((v) => !v)}
                            aria-expanded={menuCrear}
                        >
                            <Plus size={19} />
                            Crear
                        </button>
                        {menuCrear && (
                            <div className="cv-crear-opciones" role="menu">
                                <button
                                    type="button"
                                    role="menuitem"
                                    onClick={() => {
                                        setMenuCrear(false);
                                        nuevaConvocatoria();
                                    }}
                                >
                                    <BriefcaseBusiness size={18} />
                                    <span>
                                        <strong>Convocatoria suelta</strong>
                                        <small>Un solo puesto, como siempre.</small>
                                    </span>
                                </button>
                                <button
                                    type="button"
                                    role="menuitem"
                                    onClick={() => {
                                        setMenuCrear(false);
                                        setErrorBusqueda('');
                                        setModalBusqueda({ id: null, nombre: '' });
                                    }}
                                >
                                    <FolderPlus size={18} />
                                    <span>
                                        <strong>Nueva búsqueda</strong>
                                        <small>Carpeta para agrupar varios puestos.</small>
                                    </span>
                                </button>
                            </div>
                        )}
                    </div>
                )}
            </div>
            {mensaje && (
                <div className="cv-mensaje cv-mensaje-exito">
                    <CircleCheck size={19} />
                    <span>{mensaje}</span>
                    <button type="button" onClick={() => setMensaje('')}>
                        <X size={17} />
                    </button>
                </div>
            )}
            {!mostrarFormulario && (
                <>
                    <div className="cv-estadisticas">
                        {([
                            ['Total de convocatorias', convocatorias.length],
                            ['Publicadas', contarEstado('PUBLICADA')],
                            ['Borradores', contarEstado('BORRADOR')],
                            ['Cerradas', contarEstado('CERRADA')],
                        ] as const).map(([texto, valor]) => (
                            <div className="cv-estadistica" key={texto}>
                                <span>{texto}</span>
                                <strong>{valor}</strong>
                            </div>
                        ))}
                    </div>
                    <div className="cv-tarjeta">
                        <div className="cv-seccion-titulo">
                            <ClipboardList size={21} />
                            <h2>Listado de convocatorias</h2>
                        </div>
                        {errorCarga && <div className="cv-mensaje cv-mensaje-error">{errorCarga}</div>}
                        {cargando ? (
                            <div className="cv-vacio">
                                <Cargando tipo="circulo" mensaje="Cargando convocatorias" />
                            </div>
                        ) : convocatorias.length === 0 ? (
                            <div className="cv-vacio">
                                <BriefcaseBusiness size={45} />
                                <h3>Aún no tienes convocatorias</h3>
                                <p>Crea tu primera oferta laboral para comenzar a recibir postulaciones.</p>
                                <button type="button" className="cv-boton cv-boton-primario" onClick={nuevaConvocatoria}>
                                    <Plus size={18} />
                                    Crear convocatoria
                                </button>
                            </div>
                        ) : (
                            <div className="cv-listado">
                                <div className="cv-pestanas">
                                    <button
                                        type="button"
                                        className={`cv-pestana ${pestanaActiva === 'sueltas' ? 'cv-pestana-activa' : ''}`}
                                        onClick={() => setPestanaActiva('sueltas')}
                                    >
                                        <BriefcaseBusiness size={18} />
                                        Convocatorias sueltas ({sueltas.length})
                                    </button>
                                    <button
                                        type="button"
                                        className={`cv-pestana ${pestanaActiva === 'busquedas' ? 'cv-pestana-activa' : ''}`}
                                        onClick={() => setPestanaActiva('busquedas')}
                                    >
                                        <Folder size={18} />
                                        Búsquedas ({busquedas.length})
                                    </button>
                                    <select
                                        className="cv-filtro-estado"
                                        value={filtroEstado}
                                        onChange={(e) => setFiltroEstado(e.target.value as typeof filtroEstado)}
                                        aria-label="Filtrar por estado"
                                    >
                                        <option value="TODAS">Todas</option>
                                        <option value="PUBLICADA">Vigentes</option>
                                        <option value="CERRADA">Cerradas</option>
                                        <option value="BORRADOR">Borradores</option>
                                    </select>
                                </div>
                                {pestanaActiva === 'sueltas' && (
                                    <div className="cv-contenido-pestana">
                                        {sueltas.length === 0 ? (
                                            <p className="cv-carpeta-vacia">No hay convocatorias sueltas{filtroEstado === 'TODAS' ? '' : ' con este estado'}.</p>
                                        ) : (
                                            sueltas.map(tarjeta)
                                        )}
                                    </div>
                                )}
                                {pestanaActiva === 'busquedas' && (
                                    <div className="cv-contenido-pestana">
                                        {busquedas.length === 0 ? (
                                            <p className="cv-carpeta-vacia">No hay búsquedas creadas.</p>
                                        ) : (
                                            busquedas.map((b) => {
                                                const puestos = convocatorias.filter((c) => c.busqueda?.id === b.id && pasaFiltro(c));
                                                const vacantes = puestos.reduce((s, c) => s + (c.vacantes ?? 1), 0);
                                                const postulantes = puestos.reduce((s, c) => s + (c.totalPostulaciones ?? 0), 0);
                                                const abierta = carpetasAbiertas.has(b.id);
                                                return (
                                                    <div className="cv-carpeta" key={`b-${b.id}`}>
                                                        <div className="cv-carpeta-cabecera">
                                                            <button
                                                                type="button"
                                                                className="cv-carpeta-toggle"
                                                                onClick={() => alternarCarpeta(b.id)}
                                                                aria-expanded={abierta}
                                                            >
                                                                {abierta ? <FolderOpen size={22} /> : <Folder size={22} />}
                                                                <span>
                                                                    <strong>{b.nombre}</strong>
                                                                    <small>
                                                                        {puestos.length} puesto(s) · {vacantes} vacante(s) · {postulantes} postulante(s)
                                                                    </small>
                                                                </span>
                                                            </button>
                                                            <div className="cv-carpeta-acciones">
                                                                <button
                                                                    type="button"
                                                                    className="cv-boton cv-boton-publicar"
                                                                    onClick={() => agregarPuesto(b, puestos)}
                                                                >
                                                                    <Plus size={16} />
                                                                    Agregar puesto
                                                                </button>
                                                                <button
                                                                    type="button"
                                                                    className="cv-icono-boton"
                                                                    onClick={() => setModalBusqueda({ id: b.id, nombre: b.nombre })}
                                                                    aria-label={`Renombrar ${b.nombre}`}
                                                                    title="Renombrar"
                                                                >
                                                                    <Pencil size={16} />
                                                                </button>
                                                                <button
                                                                    type="button"
                                                                    className="cv-icono-boton cv-icono-peligro"
                                                                    onClick={() => setEliminarBusquedaId(b.id)}
                                                                    aria-label={`Eliminar ${b.nombre}`}
                                                                    title="Eliminar carpeta"
                                                                >
                                                                    <Trash2 size={16} />
                                                                </button>
                                                            </div>
                                                        </div>
                                                        {abierta && (
                                                            <div className="cv-carpeta-contenido">
                                                                {puestos.length === 0 ? (
                                                                    <p className="cv-carpeta-vacia">
                                                                        Aún no hay puestos. Usa «Agregar puesto» o mueve aquí una convocatoria suelta.
                                                                    </p>
                                                                ) : (
                                                                    puestos.map(tarjeta)
                                                                )}
                                                            </div>
                                                        )}
                                                    </div>
                                                );
                                            })
                                        )}
                                    </div>
                                )}
                            </div>
                        )}
                    </div>
                </>
            )}
            {mostrarFormulario && (
                <div className="cv-tarjeta cv-formulario-tarjeta">
                    <div className="cv-formulario-encabezado">
                        <div>
                            <h2>
                                {editandoId
                                    ? 'Editar convocatoria'
                                    : formulario.idBusqueda
                                        ? `Nuevo puesto en «${busquedas.find((b) => b.id === formulario.idBusqueda)?.nombre ?? 'la búsqueda'}»`
                                        : 'Nueva convocatoria'}
                            </h2>
                            <p>Completa los datos de la oportunidad laboral.</p>
                        </div>
                        <button type="button" className="cv-cerrar" onClick={limpiarFormulario} aria-label="Cerrar formulario">
                            <X size={22} />
                        </button>
                    </div>
                    {error && <div className="cv-mensaje cv-mensaje-error">{error}</div>}
                    <form
                        onSubmit={(evento) => {
                            evento.preventDefault();
                            void guardar(true);
                        }}
                    >
                        <div className="cv-bloque">
                            <h3>Información general</h3>
                            <div className="cv-grid">
                                <div className="cv-campo cv-ancho-completo">
                                    <Etiqueta id="cv-puesto">Puesto de trabajo</Etiqueta>
                                    <input
                                        id="cv-puesto"
                                        value={formulario.puesto}
                                        onChange={alEscribir('puesto')}
                                        placeholder="Ej. Ingeniero de minas"
                                        maxLength={120}
                                    />
                                </div>
                                <div className="cv-campo">
                                    <Etiqueta id="cv-tipo">Tipo de convocatoria</Etiqueta>
                                    <select
                                        id="cv-tipo"
                                        value={formulario.tipo}
                                        onChange={(e) => actualizarCampo('tipo', e.target.value as TipoConvocatoria)}
                                    >
                                        <option value="PLANILLA_COMPLETA">Planilla completa</option>
                                        <option value="PARADA_PLANTA">Parada de planta</option>
                                    </select>
                                </div>
                                <div className="cv-campo">
                                    <Etiqueta id="cv-experiencia">Años de experiencia</Etiqueta>
                                    <input
                                        id="cv-experiencia"
                                        type="text"
                                        inputMode="numeric"
                                        pattern="[0-9]*"
                                        value={formulario.experiencia}
                                        onChange={(e) => actualizarCampo('experiencia', e.target.value.replace(/\D/g, ''))}
                                        placeholder="Ej. 3"
                                        maxLength={2}
                                        autoComplete="off"
                                    />
                                    <small>Solo números enteros, por ejemplo: 0, 2 o 5.</small>
                                </div>
                                <div className="cv-campo">
                                    <Etiqueta id="cv-vacantes">Vacantes</Etiqueta>
                                    <input
                                        id="cv-vacantes"
                                        type="text"
                                        inputMode="numeric"
                                        value={formulario.vacantes}
                                        onChange={(e) => actualizarCampo('vacantes', e.target.value.replace(/\D/g, '').slice(0, 3))}
                                        placeholder="Ej. 10"
                                        autoComplete="off"
                                    />
                                    <small>¿Cuántas personas contratarás para este puesto?</small>
                                </div>
                                {busquedas.length > 0 && (
                                    <div className="cv-campo">
                                        <label htmlFor="cv-busqueda">Búsqueda (opcional)</label>
                                        {selectorCarpeta(formulario.idBusqueda, (id) => actualizarCampo('idBusqueda', id), { id: 'cv-busqueda' })}
                                        <small>Solo sirve para organizar tus convocatorias.</small>
                                    </div>
                                )}
                                <SelectorUbigeo
                                    valor={formulario.ubigeo}
                                    onChange={(valor) => actualizarCampo('ubigeo', valor)}
                                    etiquetas={['Departamento', 'Provincia', 'Distrito']}
                                    textosVacios={['Elige el departamento', 'Elige la provincia', 'Elige el distrito']}
                                    obligatorio
                                    claseCampo="cv-campo"
                                    idBase="cv-ubigeo"
                                />
                                <div className="cv-campo">
                                    <Etiqueta id="cv-direccion" obligatorio={esParada}>Dirección</Etiqueta>
                                    <input
                                        id="cv-direccion"
                                        value={formulario.direccion}
                                        onChange={alEscribir('direccion')}
                                        placeholder="Ej. Km 45 Carretera Central"
                                        maxLength={255}
                                    />
                                    <small>
                                        {esParada ? 'Obligatoria: dónde se presenta el personal de la parada.' : 'Opcional.'}
                                    </small>
                                </div>
                                <div className="cv-campo">
                                    <label htmlFor="cv-referencia">Referencia</label>
                                    <input
                                        id="cv-referencia"
                                        value={formulario.referencia}
                                        onChange={alEscribir('referencia')}
                                        placeholder="Ej. Frente al grifo de la entrada"
                                        maxLength={255}
                                    />
                                </div>
                                <div className="cv-campo">
                                    <Etiqueta id="cv-campamento">Campamento</Etiqueta>
                                    <input
                                        id="cv-campamento"
                                        value={formulario.campamento}
                                        disabled={formulario.campamento === SIN_CAMPAMENTO}
                                        onChange={alEscribir('campamento')}
                                        placeholder="Ej. Unidad minera"
                                    />
                                    <label className="cv-certificado-pdf">
                                        <input
                                            type="checkbox"
                                            checked={formulario.campamento === SIN_CAMPAMENTO}
                                            onChange={(e) => actualizarCampo('campamento', e.target.checked ? SIN_CAMPAMENTO : '')}
                                        />
                                        No requiere campamento (puesto de oficina)
                                    </label>
                                </div>
                                <div className="cv-campo cv-ancho-completo">
                                    <Etiqueta id="cv-turno">{esParada ? 'Turno y horario' : 'Régimen de trabajo'}</Etiqueta>
                                    <input
                                        id="cv-turno"
                                        value={formulario.turno}
                                        onChange={alEscribir('turno')}
                                        placeholder={esParada ? 'Ej. Turnos de 12 horas, diurno o nocturno' : 'Ej. Régimen 14x7, 21x7 o 5x2'}
                                    />
                                </div>
                                <div className="cv-campo cv-ancho-completo">
                                    <Etiqueta id="cv-descripcion">Descripción del puesto</Etiqueta>
                                    <textarea
                                        id="cv-descripcion"
                                        rows={4}
                                        value={formulario.descripcion}
                                        onChange={alEscribir('descripcion')}
                                        placeholder="Describe las principales responsabilidades del puesto..."
                                    />
                                </div>
                            </div>
                        </div>
                        <div className="cv-bloque">
                            <h3>Requisitos del puesto</h3>
                            <div className="cv-grid">
                                <div className="cv-campo cv-ancho-completo">
                                    <Etiqueta id="cv-obligatorios">Requisitos obligatorios</Etiqueta>
                                    <textarea
                                        id="cv-obligatorios"
                                        rows={4}
                                        value={formulario.requisitosObligatorios}
                                        onChange={alEscribir('requisitosObligatorios')}
                                        placeholder="Ej. Título profesional, experiencia en mina..."
                                    />
                                </div>
                                <div className="cv-campo cv-ancho-completo">
                                    <label htmlFor="cv-deseables">Requisitos deseables</label>
                                    <textarea
                                        id="cv-deseables"
                                        rows={3}
                                        value={formulario.requisitosDeseables}
                                        onChange={alEscribir('requisitosDeseables')}
                                        placeholder="Ej. Manejo de software especializado..."
                                    />
                                </div>
                                <div className="cv-campo cv-ancho-completo">
                                    <label>Certificados exigidos</label>
                                    <small>
                                        El postulante deberá indicar qué certificado de su perfil cumple cada uno. Marca «PDF
                                        obligatorio» si debe tener el documento subido.
                                    </small>
                                    <div className="cv-certificados">
                                        {formulario.certificados.map((cert, indice) => (
                                            <div className="cv-certificado-fila" key={indice}>
                                                <select
                                                    value={cert.idCatalogoCertificacion ?? ''}
                                                    onChange={(e) => actualizarCertificado(indice, {
                                                        idCatalogoCertificacion: e.target.value === '' ? null : Number(e.target.value),
                                                    })}
                                                    aria-label={`Certificado ${indice + 1}`}
                                                >
                                                    <option value="">Elige un certificado</option>
                                                    {tiposCertificacion.map((t) => <option key={t.id} value={t.id}>{t.nombre}</option>)}
                                                </select>
                                                <label className="cv-certificado-pdf">
                                                    <input
                                                        type="checkbox"
                                                        checked={cert.requierePdf}
                                                        onChange={(e) => actualizarCertificado(indice, { requierePdf: e.target.checked })}
                                                    />
                                                    PDF obligatorio
                                                </label>
                                                <button
                                                    type="button"
                                                    className="cv-certificado-quitar"
                                                    onClick={() => cambiarCertificados((lista) => lista.filter((_, i) => i !== indice))}
                                                    aria-label={`Quitar certificado ${indice + 1}`}
                                                >
                                                    <X size={17} />
                                                </button>
                                            </div>
                                        ))}

                                        {formulario.certificados.length < 20 && (
                                            <button
                                                type="button"
                                                className="cv-boton cv-boton-suave"
                                                onClick={() => cambiarCertificados((lista) =>
                                                    [...lista, { idCatalogoCertificacion: null, requierePdf: false }])}
                                            >
                                                <Plus size={17} />
                                                Agregar certificado
                                            </button>
                                        )}
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div className="cv-bloque">
                            <h3>Fechas de la convocatoria</h3>
                            <div className="cv-grid">
                                <CalendarioPersonalizado
                                    etiqueta="Fecha de cierre de postulaciones"
                                    valor={formulario.fechaCierre}
                                    onChange={(valor) => actualizarCampo('fechaCierre', valor)}
                                    minFecha={obtenerFechaActual()}
                                    obligatorio
                                />
                                {esParada && (
                                    <>
                                        <CalendarioPersonalizado
                                            etiqueta="Inicio de labores"
                                            valor={formulario.fechaInicio}
                                            onChange={(valor) => setFormulario((anterior) => ({
                                                ...anterior,
                                                fechaInicio: valor,
                                                fechaFin: anterior.fechaFin && valor && anterior.fechaFin < valor ? '' : anterior.fechaFin,
                                            }))}
                                            obligatorio
                                        />
                                        <CalendarioPersonalizado
                                            etiqueta="Fin de labores"
                                            valor={formulario.fechaFin}
                                            onChange={(valor) => actualizarCampo('fechaFin', valor)}
                                            minFecha={formulario.fechaInicio || undefined}
                                            obligatorio
                                        />
                                    </>
                                )}
                            </div>
                        </div>
                        <div className="cv-formulario-acciones">
                            <button type="button" className="cv-boton cv-boton-cancelar" onClick={limpiarFormulario}>
                                Cancelar
                            </button>
                            <button
                                type="button"
                                className="cv-boton cv-boton-suave"
                                onClick={() => void guardar(false)}
                                disabled={enviando}
                            >
                                {editandoId ? 'Guardar cambios' : 'Guardar como borrador'}
                            </button>
                            <button type="submit" className="cv-boton cv-boton-primario" disabled={enviando}>
                                <Send size={18} />
                                {enviando ? 'Guardando...' : editandoId ? 'Guardar y publicar' : 'Publicar convocatoria'}
                            </button>
                        </div>
                    </form>
                </div>
            )}
            {resumen && (
                <div
                    className="cv-modal-fondo"
                    onMouseDown={(e) => {
                        if (e.target === e.currentTarget) setResumen(null);
                    }}
                >
                    <div className="cv-modal cv-modal-resumen" role="dialog" aria-modal="true" aria-label="Resumen de convocatoria">
                        <div className="cv-modal-cabecera">
                            <div>
                                <span className="cv-modal-subtitulo">RESUMEN DE CONVOCATORIA</span>
                                <h2>{resumen.puesto || 'Convocatoria sin título'}</h2>
                            </div>
                            <button type="button" className="cv-cerrar" onClick={() => setResumen(null)} aria-label="Cerrar resumen">
                                <X size={22} />
                            </button>
                        </div>
                        <div className="cv-modal-contenido">
                            <div className="cv-resumen-etiquetas">
                                <span className={`cv-estado cv-estado-${resumen.estado.toLowerCase()}`}>{resumen.estado}</span>
                                <span className="cv-tipo-etiqueta">{nombreTipo(resumen.tipo)}</span>
                            </div>
                            <div className="cv-resumen-grid">
                                {datosResumen(resumen).map((dato) => dato && (
                                    <div key={dato[0]}>
                                        <span>{dato[0]}</span>
                                        <strong>{dato[1]}</strong>
                                    </div>
                                ))}
                            </div>

                            {([
                                ['Descripción del puesto', resumen.descripcion || 'No especificada'],
                                ['Requisitos obligatorios', resumen.requisitosObligatorios || 'No especificados'],
                                ['Requisitos deseables', resumen.requisitosDeseables || 'No especificados'],
                            ] as const).map(([titulo, texto]) => (
                                <div className="cv-resumen-seccion" key={titulo}>
                                    <h3>{titulo}</h3>
                                    <p>{texto}</p>
                                </div>
                            ))}

                            <div className="cv-resumen-seccion">
                                <h3>Certificados exigidos</h3>
                                {resumen.certificadosRequeridos.length === 0 ? (
                                    <p>Ninguno</p>
                                ) : (
                                    <ul>
                                        {resumen.certificadosRequeridos.map((c) => (
                                            <li key={c.idRequisito}>
                                                {c.nombre}
                                                {c.requierePdf && ' — PDF obligatorio'}
                                            </li>
                                        ))}
                                    </ul>
                                )}
                            </div>
                        </div>
                        <div className="cv-modal-acciones">
                            <button type="button" className="cv-boton cv-boton-cancelar" onClick={() => setResumen(null)}>
                                Cerrar
                            </button>
                            {resumen.estado !== 'BORRADOR' ? (
                                <button type="button" className="cv-boton cv-boton-suave" onClick={() => verPostulantes(resumen)}>
                                    <Users size={17} />
                                    Ver postulantes
                                </button>
                            ) : (
                                <button type="button" className="cv-boton cv-boton-primario" onClick={() => editarConvocatoria(resumen)}>
                                    <FilePenLine size={17} />
                                    Editar convocatoria
                                </button>
                            )}
                        </div>
                    </div>
                </div>
            )}
            {eliminarId && (
                <ModalConfirmar
                    etiqueta="Confirmar eliminación"
                    Icono={Trash2}
                    titulo="¿Eliminar convocatoria?"
                    texto="Dejará de verse en todas las listas. No podrás recuperarla después."
                    textoBoton="Sí, eliminar"
                    enviando={enviando}
                    onCancelar={() => setEliminarId(null)}
                    onConfirmar={() => void eliminarConvocatoria()}
                />
            )}
            {modalBusqueda && (
                <div
                    className="cv-modal-fondo"
                    onMouseDown={(e) => {
                        if (e.target === e.currentTarget && !enviando) setModalBusqueda(null);
                    }}
                >
                    <form
                        className="cv-modal cv-modal-busqueda"
                        role="dialog"
                        aria-modal="true"
                        aria-label={modalBusqueda.id === null ? 'Nueva búsqueda' : 'Renombrar búsqueda'}
                        onSubmit={(e) => {
                            e.preventDefault();
                            void guardarBusqueda();
                        }}
                    >
                        <div className="cv-eliminar-icono cv-icono-carpeta">
                            <FolderPlus size={28} />
                        </div>
                        <h2>{modalBusqueda.id === null ? 'Nueva búsqueda' : 'Renombrar búsqueda'}</h2>
                        <p>
                            Agrupa los puestos de una misma campaña. Cada puesto sigue siendo una convocatoria con sus propios
                            requisitos y postulantes.
                        </p>
                        <div className="cv-campo">
                            <label htmlFor="cv-nombre-busqueda">Nombre</label>
                            <input
                                id="cv-nombre-busqueda"
                                value={modalBusqueda.nombre}
                                onChange={(e) => {
                                    setModalBusqueda({ ...modalBusqueda, nombre: e.target.value });
                                    setErrorBusqueda('');
                                }}
                                placeholder="Ej. Parada octubre 2026"
                                maxLength={120}
                                autoFocus
                            />
                        </div>
                        {errorBusqueda && <div className="cv-mensaje cv-mensaje-error">{errorBusqueda}</div>}
                        <div className="cv-modal-acciones">
                            <button
                                type="button"
                                className="cv-boton cv-boton-cancelar"
                                onClick={() => setModalBusqueda(null)}
                                disabled={enviando}
                            >
                                Cancelar
                            </button>
                            <button type="submit" className="cv-boton cv-boton-primario" disabled={enviando}>
                                {enviando ? 'Guardando...' : modalBusqueda.id === null ? 'Crear búsqueda' : 'Guardar'}
                            </button>
                        </div>
                    </form>
                </div>
            )}
            {eliminarBusquedaId && (
                <ModalConfirmar
                    etiqueta="Confirmar eliminación de carpeta"
                    Icono={Trash2}
                    titulo="¿Eliminar la carpeta?"
                    texto="Solo se elimina la carpeta. Sus convocatorias y postulantes se conservan y pasan a «Convocatorias sueltas»."
                    textoBoton="Sí, eliminar carpeta"
                    enviando={enviando}
                    onCancelar={() => setEliminarBusquedaId(null)}
                    onConfirmar={() => void confirmarEliminarBusqueda()}
                />
            )}
            {modalFecha && (
                <div className="cv-modal-fondo">
                    <form
                        className="cv-modal cv-modal-busqueda"
                        role="dialog"
                        aria-modal="true"
                        aria-label="Cambiar fecha de cierre"
                        onSubmit={(e) => {
                            e.preventDefault();
                            void confirmarFecha();
                        }}
                    >
                        <div className="cv-eliminar-icono cv-icono-carpeta">
                            <CalendarDays size={28} />
                        </div>
                        <h2>{modalFecha.convocatoria.estado === 'CERRADA' ? 'Reabrir con nueva fecha' : 'Cambiar fecha de cierre'}</h2>
                        <p>
                            {modalFecha.convocatoria.estado === 'CERRADA'
                                ? 'La convocatoria volverá a mostrarse a los postulantes y recibirá postulaciones hasta la nueva fecha. Quienes ya postularon conservan su postulación.'
                                : 'Los postulantes podrán postular hasta la nueva fecha. Al pasarla, se cerrará sola.'}
                        </p>
                        {(modalFecha.convocatoria.postulacionesAceptadas ?? 0) >= (modalFecha.convocatoria.vacantes ?? 1) && (
                            <div className="cv-mensaje cv-mensaje-error">
                                Ya aceptaste {modalFecha.convocatoria.postulacionesAceptadas} postulante(s) para{' '}
                                {modalFecha.convocatoria.vacantes ?? 1} vacante(s): todas están cubiertas.
                            </div>
                        )}
                        <div className="cv-campo">
                            <label htmlFor="cv-nueva-fecha-cierre">
                                Nueva fecha de cierre (actual: {mostrarFecha(modalFecha.convocatoria.fechaCierre ?? '')})
                            </label>
                            <input
                                id="cv-nueva-fecha-cierre"
                                type="date"
                                min={obtenerFechaActual()}
                                value={modalFecha.fecha}
                                onChange={(e) => {
                                    setModalFecha({ ...modalFecha, fecha: e.target.value });
                                    setErrorFecha('');
                                }}
                                autoFocus
                            />
                        </div>
                        {errorFecha && <div className="cv-mensaje cv-mensaje-error">{errorFecha}</div>}
                        <div className="cv-modal-acciones">
                            <button
                                type="button"
                                className="cv-boton cv-boton-cancelar"
                                onClick={() => setModalFecha(null)}
                                disabled={enviando}
                            >
                                Cancelar
                            </button>
                            <button type="submit" className="cv-boton cv-boton-primario" disabled={enviando || !modalFecha.fecha}>
                                <CalendarDays size={17} />
                                {modalFecha.convocatoria.estado === 'CERRADA' ? 'Reabrir' : 'Guardar fecha'}
                            </button>
                        </div>
                    </form>
                </div>
            )}
            {cerrarId && (
                <ModalConfirmar
                    etiqueta="Confirmar cierre"
                    Icono={Lock}
                    titulo="¿Cerrar convocatoria?"
                    texto="Dejará de mostrarse a los postulantes y no recibirá nuevas postulaciones. Las ya recibidas se conservan. Podrás reabrirla más adelante con una nueva fecha de cierre."
                    textoBoton="Sí, cerrar"
                    enviando={enviando}
                    onCancelar={() => setCerrarId(null)}
                    onConfirmar={() => void confirmarCierre()}
                />
            )}
        </div>
    );
};

export default CrearConvocatoria;
