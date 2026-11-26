import Cargando from '../../components/Cargando';
import { useEffect, useRef, useState, type Dispatch, type FormEvent, type ReactNode, type SetStateAction } from 'react'
import { useNavigate } from 'react-router-dom'
import { listarTiposCertificacion, type TipoCertificacion } from '../../api/certificaciones.ts'
import {
    listarEspecialidadesCatalogo,
    listarHabilidadesCatalogo,
    type EspecialidadCatalogo,
    type HabilidadCatalogo,
} from '../../api/catalogos.ts'
import { textoError, erroresPorCampo } from '../../api/client.ts'
import {
    guardarPerfilProfesional,
    MAX_ELEMENTOS_PERFIL,
    obtenerPerfilProfesional,
    type PerfilProfesionalRequest,
} from '../../api/postulante.ts'
import { useAuth } from '../../auth/AuthContext.tsx'
import { Campo, CampoTexto } from '../../components/CampoFormulario.tsx'
import './FormularioPerfil.css'

interface Especialidad {
    uid: string
    nombre: string
    otra?: boolean
}

const OTRA = '__otra__'
interface Habilidad {
    uid: string
    nombre: string
}
interface Experiencia {
    uid: string
    puesto: string
    empresa: string
    fechaInicio: string
    fechaFin: string
    actual: boolean
    descripcion: string
}
interface Formacion {
    uid: string
    institucion: string
    carreraGrado: string
    fechaInicio: string
    fechaFin: string
    enCurso: boolean
}
interface Certificacion {
    uid: string
    id?: number
    idCatalogoCertificacion: number | null
    nombre: string
    entidadEmisora: string
    fechaEmision: string
}

type Errores = Record<string, string>

let contador = 0
const nuevoUid = () => `f${++contador}`

function hoyISO(): string {
    const ahora = new Date()
    ahora.setMinutes(ahora.getMinutes() - ahora.getTimezoneOffset())
    return ahora.toISOString().slice(0, 10)
}

function actualizar<T extends { uid: string }>(filas: T[], uid: string, cambios: Partial<T>): T[] {
    return filas.map((fila) => (fila.uid === uid ? { ...fila, ...cambios } : fila))
}

function validarTexto(errores: Errores, id: string, valor: string, max: number, vacio: string) {
    const texto = valor.trim()
    if (!texto) errores[id] = vacio
    else if (texto.length > max) errores[id] = `Máximo ${max} caracteres.`
}

function validarPeriodo(
    errores: Errores,
    prefijo: string,
    inicio: string,
    fin: string,
    abierto: boolean,
    hoy: string,
) {
    if (!inicio) errores[`${prefijo}-inicio`] = 'Indica la fecha de inicio.'
    else if (inicio > hoy) errores[`${prefijo}-inicio`] = 'La fecha de inicio no puede ser futura.'

    if (abierto) return
    if (!fin) errores[`${prefijo}-fin`] = 'Indica la fecha de fin.'
    else if (fin > hoy) errores[`${prefijo}-fin`] = 'La fecha de fin no puede ser futura.'
    else if (inicio && fin < inicio) errores[`${prefijo}-fin`] = 'La fecha de fin debe ser posterior a la de inicio.'
}

interface Fila {
    titulo: string
    quitarEtiqueta: string
    onQuitar?: () => void
    children: ReactNode
}

function FilaRepetible({ titulo, quitarEtiqueta, onQuitar, children }: Fila) {
    return (
        <div className="fila">
            <div className="fila-cabecera">
                <h3>{titulo}</h3>
                {onQuitar && (
                    <button type="button" className="quitar" aria-label={quitarEtiqueta} onClick={onQuitar}>
                        Quitar
                    </button>
                )}
            </div>
            {children}
        </div>
    )
}

interface FormularioPerfilProps {
    edicion?: { onGuardado: () => void; onCancelar: () => void }
}

export default function FormularioPerfil({ edicion }: FormularioPerfilProps = {}) {
    const hoy = hoyISO()

    const [primeraEspecialidad] = useState(nuevoUid)
    const [resumen, setResumen] = useState('')
    const [especialidades, setEspecialidades] = useState<Especialidad[]>([{ uid: primeraEspecialidad, nombre: '' }])
    const [principalUid, setPrincipalUid] = useState(primeraEspecialidad)
    const [habilidades, setHabilidades] = useState<Habilidad[]>([])
    const [habilidadNueva, setHabilidadNueva] = useState('')
    const [experiencias, setExperiencias] = useState<Experiencia[]>([])
    const [formaciones, setFormaciones] = useState<Formacion[]>([])
    const [certificaciones, setCertificaciones] = useState<Certificacion[]>([])
    const [tiposCertificacion, setTiposCertificacion] = useState<TipoCertificacion[]>([])
    const [catalogoEspecialidades, setCatalogoEspecialidades] = useState<EspecialidadCatalogo[]>([])
    const [catalogoHabilidades, setCatalogoHabilidades] = useState<HabilidadCatalogo[]>([])

    const [errores, setErrores] = useState<Errores>({})
    const [errorGeneral, setErrorGeneral] = useState<string | null>(null)
    const [detalleServidor, setDetalleServidor] = useState<string[]>([])
    const [enviando, setEnviando] = useState(false)
    const [guardado, setGuardado] = useState(false)
    const [cargando, setCargando] = useState(true)
    const tituloGuardado = useRef<HTMLHeadingElement>(null)
    const navigate = useNavigate()
    const { refrescar } = useAuth()

    useEffect(() => {
        if (guardado) tituloGuardado.current?.focus()
    }, [guardado])

    useEffect(() => {
        let vigente = true
        obtenerPerfilProfesional()
            .then((p) => {
                if (!vigente) return
                setResumen(p.resumenProfesional ?? '')
                if (p.especialidades.length > 0) {
                    const esp = p.especialidades.map((e) => ({ uid: nuevoUid(), nombre: e.nombre, principal: e.principal }))
                    setEspecialidades(esp.map(({ uid, nombre }) => ({ uid, nombre })))
                    setPrincipalUid((esp.find((e) => e.principal) ?? esp[0]).uid)
                }
                setHabilidades(p.habilidades.map((nombre) => ({ uid: nuevoUid(), nombre })))
                setExperiencias(p.experiencias.map((x) => ({
                    uid: nuevoUid(), puesto: x.puesto, empresa: x.empresa, fechaInicio: x.fechaInicio,
                    fechaFin: x.fechaFin ?? '', actual: x.fechaFin === null, descripcion: x.descripcion ?? '',
                })))
                setFormaciones(p.formaciones.map((x) => ({
                    uid: nuevoUid(), institucion: x.institucion, carreraGrado: x.carreraGrado, fechaInicio: x.fechaInicio,
                    fechaFin: x.fechaFin ?? '', enCurso: x.fechaFin === null,
                })))
                setCertificaciones(p.certificaciones.map((x) => ({
                    uid: nuevoUid(), id: x.id, idCatalogoCertificacion: x.idCatalogoCertificacion, nombre: x.nombre,
                    entidadEmisora: x.entidadEmisora, fechaEmision: x.fechaEmision ?? '',
                })))
            })
            .catch(() => {  })
            .finally(() => { if (vigente) setCargando(false) })
        listarTiposCertificacion()
            .then((tipos) => { if (vigente) setTiposCertificacion(tipos) })
            .catch(() => {  })
        listarEspecialidadesCatalogo()
            .then((lista) => { if (vigente) setCatalogoEspecialidades(lista) })
            .catch(() => {  })
        listarHabilidadesCatalogo()
            .then((lista) => { if (vigente) setCatalogoHabilidades(lista) })
            .catch(() => {  })
        return () => { vigente = false }
    }, [])

    async function irAlPanel() {
        await refrescar()
        navigate('/postulante', { replace: true })
    }

    useEffect(() => {
        if (!guardado) return
        const id = setTimeout(() => void irAlPanel(), 2500)
        return () => clearTimeout(id)
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [guardado])

    function limpiar(...ids: string[]) {
        setErrores((actuales) => {
            if (!ids.some((id) => id in actuales)) return actuales
            const copia = { ...actuales }
            for (const id of ids) delete copia[id]
            return copia
        })
    }

    function editar<T extends { uid: string }>(
        setFilas: Dispatch<SetStateAction<T[]>>,
        uid: string,
        cambios: Partial<T>,
        ...idsConError: string[]
    ) {
        setFilas((filas) => actualizar(filas, uid, cambios))
        limpiar(...idsConError)
    }

    function quitar<T extends { uid: string }>(setFilas: Dispatch<SetStateAction<T[]>>, uid: string) {
        setFilas((filas) => filas.filter((fila) => fila.uid !== uid))
    }

    function quitarEspecialidad(uid: string) {
        const restantes = especialidades.filter((e) => e.uid !== uid)
        setEspecialidades(restantes)
        if (uid === principalUid && restantes.length > 0) setPrincipalUid(restantes[0].uid)
    }

    function esOtraEspecialidad(esp: Especialidad): boolean {
        if (catalogoEspecialidades.length === 0 || esp.otra) return true
        const nombre = esp.nombre.trim().toLowerCase()
        return nombre !== '' && !catalogoEspecialidades.some((c) => c.nombre.toLowerCase() === nombre)
    }

    function elegirEspecialidad(uid: string, valor: string, idError: string) {
        if (valor === OTRA) editar(setEspecialidades, uid, { nombre: '', otra: true }, idError)
        else editar(setEspecialidades, uid, { nombre: valor, otra: false }, idError)
    }

    function agregarHabilidad(texto: string = habilidadNueva) {
        const nombre = texto.trim()
        if (!nombre) return
        if (habilidades.some((h) => h.nombre.toLowerCase() === nombre.toLowerCase())) {
            setErrores((actuales) => ({ ...actuales, 'habilidad-nueva': 'Ya agregaste esa habilidad.' }))
            return
        }
        if (habilidades.length >= MAX_ELEMENTOS_PERFIL) {
            setErrores((actuales) => ({ ...actuales, 'habilidad-nueva': `Máximo ${MAX_ELEMENTOS_PERFIL} habilidades.` }))
            return
        }
        if (nombre.length > 100) {
            setErrores((actuales) => ({ ...actuales, 'habilidad-nueva': 'Máximo 100 caracteres.' }))
            return
        }
        setHabilidades((actuales) => [...actuales, { uid: nuevoUid(), nombre }])
        setHabilidadNueva('')
        limpiar('habilidad-nueva')
    }

    function validar(): Errores {
        const e: Errores = {}

        if (resumen.length > 2000) e.resumen = 'El resumen no puede superar los 2000 caracteres.'

        const vistas = new Set<string>()
        for (const esp of especialidades) {
            const id = `esp-${esp.uid}-nombre`
            validarTexto(e, id, esp.nombre, 120, 'Escribe tu especialidad.')
            const clave = esp.nombre.trim().toLowerCase()
            if (!e[id] && vistas.has(clave)) e[id] = 'Ya agregaste esta especialidad.'
            vistas.add(clave)
        }

        if (habilidadNueva.trim()) {
            e['habilidad-nueva'] = 'Presiona "Agregar" para incluir esta habilidad o borra el texto.'
        } else if (habilidades.length === 0) {
            e['habilidad-nueva'] = 'Agrega al menos una habilidad.'
        }

        for (const x of experiencias) {
            const p = `exp-${x.uid}`
            validarTexto(e, `${p}-puesto`, x.puesto, 150, 'Escribe el puesto.')
            validarTexto(e, `${p}-empresa`, x.empresa, 150, 'Escribe la empresa.')
            validarPeriodo(e, p, x.fechaInicio, x.fechaFin, x.actual, hoy)
            if (x.descripcion.length > 2000) e[`${p}-descripcion`] = 'Máximo 2000 caracteres.'
        }

        for (const x of formaciones) {
            const p = `for-${x.uid}`
            validarTexto(e, `${p}-institucion`, x.institucion, 200, 'Escribe la institución.')
            validarTexto(e, `${p}-carrera`, x.carreraGrado, 150, 'Escribe la carrera o el grado.')
            validarPeriodo(e, p, x.fechaInicio, x.fechaFin, x.enCurso, hoy)
        }

        for (const x of certificaciones) {
            const p = `cer-${x.uid}`
            if (x.idCatalogoCertificacion === null) {
                validarTexto(e, `${p}-nombre`, x.nombre, 200, 'Escribe el nombre de la certificación.')
            }
            validarTexto(e, `${p}-entidad`, x.entidadEmisora, 150, 'Escribe la entidad que la emitió.')
            if (x.fechaEmision > hoy) e[`${p}-fecha`] = 'La fecha de emisión no puede ser futura.'
        }

        return e
    }

    async function guardar(evento: FormEvent<HTMLFormElement>) {
        evento.preventDefault()
        setErrorGeneral(null)
        setDetalleServidor([])

        const excedidas = [
            ['especialidades', especialidades.length],
            ['experiencias', experiencias.length],
            ['estudios', formaciones.length],
            ['certificaciones', certificaciones.length],
        ].filter(([, n]) => (n as number) > MAX_ELEMENTOS_PERFIL).map(([nombre]) => nombre)
        if (excedidas.length > 0) {
            setErrorGeneral(`Puedes registrar como máximo ${MAX_ELEMENTOS_PERFIL} ${excedidas.join(', ')}.`)
            return
        }

        const encontrados = validar()
        setErrores(encontrados)
        const primero = Object.keys(encontrados)[0]
        if (primero) {
            document.getElementById(primero)?.focus()
            return
        }

        const datos: PerfilProfesionalRequest = {
            resumenProfesional: resumen.trim() || null,
            especialidades: especialidades.map((e) => ({ nombre: e.nombre.trim(), principal: e.uid === principalUid })),
            habilidades: habilidades.map((h) => h.nombre),
            experiencias: experiencias.map((x) => ({
                puesto: x.puesto.trim(),
                empresa: x.empresa.trim(),
                fechaInicio: x.fechaInicio,
                fechaFin: x.actual ? null : x.fechaFin,
                descripcion: x.descripcion.trim() || null,
            })),
            formaciones: formaciones.map((x) => ({
                institucion: x.institucion.trim(),
                carreraGrado: x.carreraGrado.trim(),
                fechaInicio: x.fechaInicio,
                fechaFin: x.enCurso ? null : x.fechaFin,
            })),
            certificaciones: certificaciones.map((x) => ({
                id: x.id ?? null,
                idCatalogoCertificacion: x.idCatalogoCertificacion,
                nombre: x.idCatalogoCertificacion === null
                    ? x.nombre.trim()
                    : tiposCertificacion.find((t) => t.id === x.idCatalogoCertificacion)?.nombre ?? x.nombre.trim(),
                entidadEmisora: x.entidadEmisora.trim(),
                fechaEmision: x.fechaEmision || null,
            })),
        }

        setEnviando(true)
        try {
            const guardadoServidor = await guardarPerfilProfesional(datos)
            if (!guardadoServidor.perfilCompleto) {
                setErrorGeneral('El perfil se guardó, pero aún está incompleto: ' + guardadoServidor.pendientes.join(', ') + '.')
                return
            }
            if (edicion) {
                edicion.onGuardado()
                return
            }
            setGuardado(true)
        } catch (error) {
            setErrorGeneral(textoError(error))
            const delServidor = erroresPorCampo(error)
            const porCampo = erroresServidor(delServidor)
            setErrores(porCampo.ubicados)
            setDetalleServidor(porCampo.sinUbicar)
            const primero = Object.keys(porCampo.ubicados)[0]
            if (primero) document.getElementById(primero)?.focus()
        } finally {
            setEnviando(false)
        }
    }

    function erroresServidor(delServidor: Record<string, string>) {
        const ubicados: Errores = {}
        const sinUbicar: string[] = []
        const campos: Record<string, Record<string, string>> = {
            especialidades: { nombre: 'nombre' },
            experiencias: { puesto: 'puesto', empresa: 'empresa', fechaInicio: 'inicio', fechaFin: 'fin', descripcion: 'descripcion' },
            formaciones: { institucion: 'institucion', carreraGrado: 'carrera', fechaInicio: 'inicio', fechaFin: 'fin' },
            certificaciones: { nombre: 'nombre', entidadEmisora: 'entidad', fechaEmision: 'fecha' },
        }
        const filas: Record<string, { uid: string }[]> = { especialidades, experiencias, formaciones, certificaciones }
        const prefijos: Record<string, string> = { especialidades: 'esp', experiencias: 'exp', formaciones: 'for', certificaciones: 'cer' }
        for (const [clave, mensaje] of Object.entries(delServidor)) {
            const m = clave.match(/^(\w+)\[(\d+)\](?:\.(\w+))?$/)
            const fila = m ? filas[m[1]]?.[Number(m[2])] : undefined
            if (m && fila && m[3] && campos[m[1]]?.[m[3]]) {
                ubicados[`${prefijos[m[1]]}-${fila.uid}-${campos[m[1]][m[3]]}`] = mensaje
            } else if (clave === 'habilidades' || clave.startsWith('habilidades[')) {
                ubicados['habilidad-nueva'] = mensaje
            } else if (clave === 'resumenProfesional') {
                ubicados.resumen = mensaje
            } else {
                sinUbicar.push(mensaje)
            }
        }
        return { ubicados, sinUbicar }
    }

    if (cargando) {
        return <Cargando tipo="montana" mensaje="Cargando tu perfil profesional" />
    }

    const yaAgregadas = new Set(habilidades.map((h) => h.nombre.toLowerCase()))
    const habilidadesSugeridas = catalogoHabilidades.filter((h) => !yaAgregadas.has(h.nombre.toLowerCase()))

    if (guardado) {
        return (
            <section className="perfil-form">
                <h1 ref={tituloGuardado} tabIndex={-1}>
                    Perfil creado correctamente
                </h1>
                <p>Registramos tu especialidad, habilidades, experiencia, formación y certificaciones.</p>
                <p className="muted">Te llevamos a tu panel en unos segundos...</p>
                <div className="actions">
                    <button type="button" className="btn btn-primary" onClick={() => void irAlPanel()}>
                        Ir a mi panel
                    </button>
                </div>
            </section>
        )
    }

    return (
        <section className="perfil-form">
            {edicion ? (
                <p className="muted">
                    La especialidad principal y al menos una habilidad son obligatorias.
                    Tus certificaciones se gestionan en su propia sección de «Mi perfil».
                </p>
            ) : (
                <>
                    <h1>Completa tu perfil profesional</h1>
                    <p className="muted">
                        Registra tu especialidad, habilidades, experiencia, formación
                        y certificaciones para postular a oportunidades laborales
                        en el sector minero.
                        La especialidad principal y al menos una habilidad son
                        obligatorias. Las demás secciones son opcionales.
                    </p>
                </>
            )}
            <form onSubmit={guardar} noValidate>
                {errorGeneral && (
                    <div className="form-error" role="alert">
                        <p>{errorGeneral}</p>
                        {detalleServidor.length > 0 && (
                            <ul>
                                {detalleServidor.map((linea) => (
                                    <li key={linea}>{linea}</li>
                                ))}
                            </ul>
                        )}
                    </div>
                )}
                {!edicion && (
                <fieldset>
                    <legend>Resumen profesional</legend>
                    <CampoTexto
                        id="resumen"
                        etiqueta="Cuéntanos sobre tu trayectoria"
                        ayuda="Opcional. Dos o tres líneas bastan."
                        maxLength={2000}
                        value={resumen}
                        onChange={(e) => {
                            setResumen(e.target.value)
                            limpiar('resumen')
                        }}
                        error={errores.resumen}
                    />
                </fieldset>
                )}
                <fieldset>
                    <legend>Especialidades</legend>
                    <div className="filas">
                        {especialidades.map((esp, i) => {
                            const id = `esp-${esp.uid}-nombre`
                            return (
                                <FilaRepetible
                                    key={esp.uid}
                                    titulo={`Especialidad ${i + 1}`}
                                    quitarEtiqueta={`Quitar especialidad ${i + 1}`}
                                    onQuitar={especialidades.length > 1 ? () => quitarEspecialidad(esp.uid) : undefined}
                                >
                                    {catalogoEspecialidades.length > 0 && (
                                        <div className="campo">
                                            <label htmlFor={`${id}-lista`}>Especialidad</label>
                                            <select
                                                id={`${id}-lista`}
                                                value={esOtraEspecialidad(esp) ? OTRA : esp.nombre}
                                                aria-invalid={!esOtraEspecialidad(esp) && errores[id] ? true : undefined}
                                                onChange={(e) => elegirEspecialidad(esp.uid, e.target.value, id)}
                                            >
                                                <option value="">Selecciona una especialidad</option>
                                                {catalogoEspecialidades.map((c) => (
                                                    <option
                                                        key={c.id}
                                                        value={c.nombre}
                                                        disabled={especialidades.some((o) => o.uid !== esp.uid && o.nombre === c.nombre)}
                                                    >
                                                        {c.nombre}
                                                    </option>
                                                ))}
                                                <option value={OTRA}>Otra (escribe el nombre)</option>
                                            </select>
                                            <p className="ayuda">Elige de la lista; usa «Otra» solo si no aparece.</p>
                                            {!esOtraEspecialidad(esp) && errores[id] && (
                                                <p className="field-error">{errores[id]}</p>
                                            )}
                                        </div>
                                    )}
                                    {esOtraEspecialidad(esp) && (
                                        <Campo
                                            id={id}
                                            etiqueta={catalogoEspecialidades.length > 0 ? 'Nombre de tu especialidad' : 'Especialidad'}
                                            ayuda="Por ejemplo: geología de minas, mantenimiento mecánico, seguridad minera."
                                            maxLength={120}
                                            value={esp.nombre}
                                            onChange={(e) => editar(setEspecialidades, esp.uid, { nombre: e.target.value, otra: true }, id)}
                                            error={errores[id]}
                                        />
                                    )}
                                    <label className="check">
                                        <input
                                            type="radio"
                                            name="especialidad-principal"
                                            checked={principalUid === esp.uid}
                                            onChange={() => setPrincipalUid(esp.uid)}
                                        />
                                        Es mi especialidad principal
                                    </label>
                                </FilaRepetible>
                            )
                        })}
                    </div>
                    <button
                        type="button"
                        className="btn"
                        onClick={() => setEspecialidades((actuales) => [...actuales, { uid: nuevoUid(), nombre: '' }])}
                    >
                        Agregar especialidad
                    </button>
                </fieldset>
                <fieldset>
                    <legend>Habilidades</legend>
                    <div className="agregar-linea">
                        <Campo
                            id="habilidad-nueva"
                            etiqueta="Habilidad"
                            ayuda="Por ejemplo: topografía, AutoCAD, operación de perforadoras."
                            maxLength={100}
                            list="catalogo-habilidades"
                            autoComplete="off"
                            value={habilidadNueva}
                            onChange={(e) => {
                                setHabilidadNueva(e.target.value)
                                limpiar('habilidad-nueva')
                            }}
                            onKeyDown={(e) => {
                                if (e.key === 'Enter') {
                                    e.preventDefault()
                                    agregarHabilidad()
                                }
                            }}
                            error={errores['habilidad-nueva']}
                        />
                        <button type="button" className="btn" onClick={() => agregarHabilidad()}>
                            Agregar
                        </button>
                    </div>
                    <datalist id="catalogo-habilidades">
                        {habilidadesSugeridas.map((h) => (
                            <option key={h.id} value={h.nombre} />
                        ))}
                    </datalist>
                    {habilidadesSugeridas.length > 0 && (
                        <div className="sugeridas">
                            <p className="ayuda">Sugeridas (toca para agregar):</p>
                            <ul className="sugeridas-lista">
                                {habilidadesSugeridas.map((h) => (
                                    <li key={h.id}>
                                        <button type="button" onClick={() => agregarHabilidad(h.nombre)}>
                                            + {h.nombre}
                                        </button>
                                    </li>
                                ))}
                            </ul>
                        </div>
                    )}
                    {habilidades.length > 0 && (
                        <ul className="chips" aria-label="Habilidades agregadas">
                            {habilidades.map((h) => (
                                <li key={h.uid}>
                                    <span>{h.nombre}</span>
                                    <button type="button" aria-label={`Quitar ${h.nombre}`} onClick={() => quitar(setHabilidades, h.uid)}>
                                        ×
                                    </button>
                                </li>
                            ))}
                        </ul>
                    )}
                </fieldset>
                <fieldset>
                    <legend>Experiencia laboral</legend>
                    {experiencias.length === 0 && <p className="muted">Aún no agregaste experiencia.</p>}
                    <div className="filas">
                        {experiencias.map((x, i) => {
                            const p = `exp-${x.uid}`
                            return (
                                <FilaRepetible
                                    key={x.uid}
                                    titulo={`Experiencia ${i + 1}`}
                                    quitarEtiqueta={`Quitar experiencia ${i + 1}`}
                                    onQuitar={() => quitar(setExperiencias, x.uid)}
                                >
                                    <div className="dos-columnas">
                                        <Campo
                                            id={`${p}-puesto`}
                                            etiqueta="Puesto"
                                            maxLength={150}
                                            value={x.puesto}
                                            onChange={(e) => editar(setExperiencias, x.uid, { puesto: e.target.value }, `${p}-puesto`)}
                                            error={errores[`${p}-puesto`]}
                                        />
                                        <Campo
                                            id={`${p}-empresa`}
                                            etiqueta="Empresa"
                                            maxLength={150}
                                            value={x.empresa}
                                            onChange={(e) => editar(setExperiencias, x.uid, { empresa: e.target.value }, `${p}-empresa`)}
                                            error={errores[`${p}-empresa`]}
                                        />
                                    </div>
                                    <div className="dos-columnas">
                                        <Campo
                                            id={`${p}-inicio`}
                                            etiqueta="Fecha de inicio"
                                            type="date"
                                            max={hoy}
                                            value={x.fechaInicio}
                                            onChange={(e) => editar(setExperiencias, x.uid, { fechaInicio: e.target.value }, `${p}-inicio`, `${p}-fin`)}
                                            error={errores[`${p}-inicio`]}
                                        />
                                        {!x.actual && (
                                            <Campo
                                                id={`${p}-fin`}
                                                etiqueta="Fecha de fin"
                                                type="date"
                                                max={hoy}
                                                value={x.fechaFin}
                                                onChange={(e) => editar(setExperiencias, x.uid, { fechaFin: e.target.value }, `${p}-fin`)}
                                                error={errores[`${p}-fin`]}
                                            />
                                        )}
                                    </div>
                                    <label className="check">
                                        <input
                                            type="checkbox"
                                            checked={x.actual}
                                            onChange={(e) =>
                                                editar(setExperiencias, x.uid, { actual: e.target.checked, fechaFin: '' }, `${p}-fin`)
                                            }
                                        />
                                        Trabajo aquí actualmente
                                    </label>
                                    <CampoTexto
                                        id={`${p}-descripcion`}
                                        etiqueta="Funciones y logros"
                                        ayuda="Opcional. Máximo 2000 caracteres."
                                        maxLength={2000}
                                        value={x.descripcion}
                                        onChange={(e) => editar(setExperiencias, x.uid, { descripcion: e.target.value }, `${p}-descripcion`)}
                                        error={errores[`${p}-descripcion`]}
                                    />
                                </FilaRepetible>
                            )
                        })}
                    </div>
                    <button
                        type="button"
                        className="btn"
                        onClick={() =>
                            setExperiencias((actuales) => [
                                ...actuales,
                                { uid: nuevoUid(), puesto: '', empresa: '', fechaInicio: '', fechaFin: '', actual: false, descripcion: '' },
                            ])
                        }
                    >
                        Agregar experiencia
                    </button>
                </fieldset>
                <fieldset>
                    <legend>Formación académica</legend>
                    {formaciones.length === 0 && <p className="muted">Aún no agregaste estudios.</p>}
                    <div className="filas">
                        {formaciones.map((x, i) => {
                            const p = `for-${x.uid}`
                            return (
                                <FilaRepetible
                                    key={x.uid}
                                    titulo={`Formación ${i + 1}`}
                                    quitarEtiqueta={`Quitar formación ${i + 1}`}
                                    onQuitar={() => quitar(setFormaciones, x.uid)}
                                >
                                    <div className="dos-columnas">
                                        <Campo
                                            id={`${p}-institucion`}
                                            etiqueta="Institución"
                                            maxLength={200}
                                            value={x.institucion}
                                            onChange={(e) => editar(setFormaciones, x.uid, { institucion: e.target.value }, `${p}-institucion`)}
                                            error={errores[`${p}-institucion`]}
                                        />
                                        <Campo
                                            id={`${p}-carrera`}
                                            etiqueta="Carrera o grado"
                                            maxLength={150}
                                            value={x.carreraGrado}
                                            onChange={(e) => editar(setFormaciones, x.uid, { carreraGrado: e.target.value }, `${p}-carrera`)}
                                            error={errores[`${p}-carrera`]}
                                        />
                                    </div>
                                    <div className="dos-columnas">
                                        <Campo
                                            id={`${p}-inicio`}
                                            etiqueta="Fecha de inicio"
                                            type="date"
                                            max={hoy}
                                            value={x.fechaInicio}
                                            onChange={(e) => editar(setFormaciones, x.uid, { fechaInicio: e.target.value }, `${p}-inicio`, `${p}-fin`)}
                                            error={errores[`${p}-inicio`]}
                                        />
                                        {!x.enCurso && (
                                            <Campo
                                                id={`${p}-fin`}
                                                etiqueta="Fecha de fin"
                                                type="date"
                                                max={hoy}
                                                value={x.fechaFin}
                                                onChange={(e) => editar(setFormaciones, x.uid, { fechaFin: e.target.value }, `${p}-fin`)}
                                                error={errores[`${p}-fin`]}
                                            />
                                        )}
                                    </div>
                                    <label className="check">
                                        <input
                                            type="checkbox"
                                            checked={x.enCurso}
                                            onChange={(e) =>
                                                editar(setFormaciones, x.uid, { enCurso: e.target.checked, fechaFin: '' }, `${p}-fin`)
                                            }
                                        />
                                        Aún estoy estudiando
                                    </label>
                                </FilaRepetible>
                            )
                        })}
                    </div>
                    <button
                        type="button"
                        className="btn"
                        onClick={() =>
                            setFormaciones((actuales) => [
                                ...actuales,
                                { uid: nuevoUid(), institucion: '', carreraGrado: '', fechaInicio: '', fechaFin: '', enCurso: false },
                            ])
                        }
                    >
                        Agregar formación
                    </button>
                </fieldset>
                {!edicion && (
                <fieldset>
                    <legend>Certificaciones</legend>
                    <p className="muted">
                        Agrega tus certificaciones, cursos o diplomados.
                        Esta sección es opcional.
                    </p>
                    {certificaciones.length === 0 && (
                        <p className="muted">
                            Aún no agregaste certificaciones.
                        </p>
                    )}
                    <div className="filas">
                        {certificaciones.map((x, i) => {
                            const p = `cer-${x.uid}`

                            return (
                                <FilaRepetible
                                    key={x.uid}
                                    titulo={`Certificación ${i + 1}`}
                                    quitarEtiqueta={`Quitar certificación ${i + 1}`}
                                    onQuitar={() =>
                                        quitar(setCertificaciones, x.uid)
                                    }
                                >
                                    <div className="dos-columnas">
                                        <div className="campo">
                                            <label htmlFor={`${p}-tipo`}>Certificación</label>
                                            <select
                                                id={`${p}-tipo`}
                                                value={x.idCatalogoCertificacion ?? ''}
                                                onChange={(e) =>
                                                    editar(
                                                        setCertificaciones,
                                                        x.uid,
                                                        { idCatalogoCertificacion: e.target.value === '' ? null : Number(e.target.value) },
                                                        `${p}-nombre`
                                                    )
                                                }
                                            >
                                                {tiposCertificacion.map((t) => (
                                                    <option key={t.id} value={t.id}>{t.nombre}</option>
                                                ))}
                                                <option value="">Otra (escribe el nombre)</option>
                                            </select>
                                            <p className="ayuda">Elige de la lista; usa «Otra» solo si no aparece.</p>
                                        </div>
                                        {x.idCatalogoCertificacion === null && (
                                            <Campo
                                                id={`${p}-nombre`}
                                                etiqueta="Nombre de la certificación"
                                                maxLength={200}
                                                value={x.nombre}
                                                onChange={(e) =>
                                                    editar(
                                                        setCertificaciones,
                                                        x.uid,
                                                        { nombre: e.target.value },
                                                        `${p}-nombre`
                                                    )
                                                }
                                                error={errores[`${p}-nombre`]}
                                            />
                                        )}
                                        <Campo
                                            id={`${p}-entidad`}
                                            etiqueta="Entidad emisora"
                                            maxLength={150}
                                            value={x.entidadEmisora}
                                            onChange={(e) =>
                                                editar(
                                                    setCertificaciones,
                                                    x.uid,
                                                    { entidadEmisora: e.target.value },
                                                    `${p}-entidad`
                                                )
                                            }
                                            error={errores[`${p}-entidad`]}
                                        />
                                    </div>
                                    <div className="dos-columnas">
                                        <Campo
                                            id={`${p}-fecha`}
                                            etiqueta="Fecha de emisión"
                                            ayuda="Opcional."
                                            type="date"
                                            max={hoy}
                                            value={x.fechaEmision}
                                            onChange={(e) =>
                                                editar(
                                                    setCertificaciones,
                                                    x.uid,
                                                    { fechaEmision: e.target.value },
                                                    `${p}-fecha`
                                                )
                                            }
                                            error={errores[`${p}-fecha`]}
                                        />
                                    </div>
                                </FilaRepetible>
                            )
                        })}
                    </div>
                    <button
                        type="button"
                        className="btn"
                        disabled={
                            certificaciones.length >= MAX_ELEMENTOS_PERFIL
                        }
                        onClick={() =>
                            setCertificaciones((actuales) => [
                                ...actuales,
                                {
                                    uid: nuevoUid(),
                                    idCatalogoCertificacion: null,
                                    nombre: '',
                                    entidadEmisora: '',
                                    fechaEmision: ''
                                }
                            ])
                        }
                    >
                        + Agregar certificación
                    </button>
                </fieldset>
                )}
                {!edicion && (
                <fieldset>
                    <legend>Documentación adjunta (Opcional)</legend>
                    <p className="muted">
                        Después de guardar, podrás subir tu CV, tu DNI, el CUL, el examen médico ocupacional
                        (EMO) y el PDF de cada certificación desde <strong>Mi perfil</strong>.
                    </p>
                </fieldset>
                )}
                <div className="actions" style={{ marginTop: '2rem' }}>
                    {edicion && (
                        <button type="button" className="btn" onClick={edicion.onCancelar} disabled={enviando}>
                            Cancelar
                        </button>
                    )}
                    <button type="submit" className="btn btn-primary" disabled={enviando}>
                        {enviando ? 'Guardando perfil...' : edicion ? 'Guardar cambios' : 'Guardar perfil profesional'}
                    </button>
                </div>
            </form>
        </section>
    )
}