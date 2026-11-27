import { useEffect, useState, type ReactNode } from 'react'
import { listarDepartamentos, listarDistritos, listarProvincias, type Lugar } from '../api/ubigeo.ts'

export interface ValorUbigeo {
    departamento: string
    provincia: string
    distrito: string
}

export const UBIGEO_VACIO: ValorUbigeo = { departamento: '', provincia: '', distrito: '' }

export function ubigeoDesdeDistrito(codigo: string | null | undefined): ValorUbigeo {
    if (!codigo || codigo.length !== 6) return UBIGEO_VACIO
    return { departamento: codigo.slice(0, 2), provincia: codigo.slice(0, 4), distrito: codigo }
}

interface Props {
    valor: ValorUbigeo
    onChange: (valor: ValorUbigeo) => void
    textosVacios?: [string, string, string]
    etiquetas?: [string, string, string]
    obligatorio?: boolean
    claseCampo?: string
    claseSelect?: string
    icono?: ReactNode
    idBase?: string
    disabled?: boolean
}

const titulo = (s: string) => s.toLowerCase().replace(/(^|\s|-|\()\S/g, (c) => c.toUpperCase())

export default function SelectorUbigeo({
    valor,
    onChange,
    textosVacios = ['Departamento', 'Provincia', 'Distrito'],
    etiquetas,
    obligatorio = false,
    claseCampo,
    claseSelect,
    icono,
    idBase = 'ubigeo',
    disabled = false,
}: Props) {
    const [departamentos, setDepartamentos] = useState<Lugar[]>([])
    const [provincias, setProvincias] = useState<Lugar[]>([])
    const [distritos, setDistritos] = useState<Lugar[]>([])
    const [error, setError] = useState(false)

    useEffect(() => {
        listarDepartamentos().then(setDepartamentos).catch(() => setError(true))
    }, [])

    useEffect(() => {
        if (!valor.departamento) {
            setProvincias([])
            return
        }
        let activo = true
        listarProvincias(valor.departamento)
            .then((l) => activo && setProvincias(l))
            .catch(() => activo && setError(true))
        return () => {
            activo = false
        }
    }, [valor.departamento])

    useEffect(() => {
        if (!valor.provincia) {
            setDistritos([])
            return
        }
        let activo = true
        listarDistritos(valor.provincia)
            .then((l) => activo && setDistritos(l))
            .catch(() => activo && setError(true))
        return () => {
            activo = false
        }
    }, [valor.provincia])

    const select = (
        indice: 0 | 1 | 2,
        opciones: Lugar[],
        actual: string,
        cambiar: (codigo: string) => void,
        habilitado: boolean,
    ) => {
        const id = `${idBase}-${['dep', 'prov', 'dist'][indice]}`
        return (
            <div className={claseCampo} key={id}>
                {etiquetas && (
                    <label htmlFor={id}>
                        {etiquetas[indice]} {obligatorio && <span className="cv-obligatorio">*</span>}
                    </label>
                )}
                {icono}
                <select
                    id={id}
                    className={claseSelect}
                    value={actual}
                    disabled={disabled || !habilitado}
                    aria-label={etiquetas ? undefined : textosVacios[indice]}
                    onChange={(e) => cambiar(e.target.value)}
                >
                    <option value="">{textosVacios[indice]}</option>
                    {opciones.map((o) => (
                        <option key={o.codigo} value={o.codigo}>
                            {titulo(o.nombre)}
                        </option>
                    ))}
                </select>
            </div>
        )
    }

    return (
        <>
            {select(0, departamentos, valor.departamento, (c) => onChange({ departamento: c, provincia: '', distrito: '' }), true)}
            {select(1, provincias, valor.provincia, (c) => onChange({ ...valor, provincia: c, distrito: '' }), !!valor.departamento)}
            {select(2, distritos, valor.distrito, (c) => onChange({ ...valor, distrito: c }), !!valor.provincia)}
            {error && (
                <small role="alert" style={{ color: '#b91c1c' }}>
                    No se pudo cargar la lista de ubicaciones.
                </small>
            )}
        </>
    )
}
