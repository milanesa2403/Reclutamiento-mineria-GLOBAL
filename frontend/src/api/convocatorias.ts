import { api, dato, lista } from './client.ts'
import type { Ubicacion } from './ubigeo.ts'


export type TipoConvocatoria = 'PLANILLA_COMPLETA' | 'PARADA_PLANTA'
export type EstadoConvocatoria = 'BORRADOR' | 'PUBLICADA' | 'CERRADA'

export interface CertificadoRequeridoRequest {
    idCatalogoCertificacion: number
    requierePdf: boolean
}

export interface ConvocatoriaRequest {
    tipo: TipoConvocatoria
    puesto: string
    campamento: string
    turno: string
    descripcion: string
    requisitosObligatorios: string
    requisitosDeseables?: string | null
    experienciaMinimaAnios?: number | null
    fechaInicioLabores?: string | null
    fechaFinLabores?: string | null
    fechaCierre?: string | null
    codigoUbigeo: string
    direccion?: string | null
    referencia?: string | null
    certificadosRequeridos?: CertificadoRequeridoRequest[]
    vacantes?: number | null
    idBusqueda?: number | null
}

export interface CertificadoRequerido {
    idRequisito: number
    idCatalogoCertificacion: number
    nombre: string
    requierePdf: boolean
}

export interface Convocatoria {
    id: number
    tipo: TipoConvocatoria
    puesto: string
    campamento: string
    turno: string
    descripcion: string
    requisitosObligatorios: string
    requisitosDeseables: string | null
    experienciaMinimaAnios: number | null
    fechaInicioLabores: string | null
    fechaFinLabores: string | null
    fechaCierre: string | null
    estado: EstadoConvocatoria
    fechaCreacion: string
    fechaPublicacion: string | null
    ubicacion: Ubicacion | null
    direccion: string | null
    referencia: string | null
    certificadosRequeridos: CertificadoRequerido[]
    empresa: {
        id: number
        razonSocial: string
        sectorMinero: string | null
        descripcion: string | null
        direccion: string | null
        telefono: string | null
        correoCorporativo: string
        logoUrl?: string | null
    }
    totalPostulaciones: number | null
    postulacionesPendientes: number | null
    postulacionesAceptadas: number | null
    guardada: boolean | null
    vacantes: number
    busqueda: { id: number; nombre: string } | null
}


const EMPRESA = '/api/empresa/convocatorias'

export const listarMisConvocatorias = (estado?: EstadoConvocatoria) =>
    lista<Convocatoria>(api.get(EMPRESA, { params: { estado } }))

export const crearConvocatoria = (datos: ConvocatoriaRequest) =>
    dato<Convocatoria>(api.post(EMPRESA, datos))

export const actualizarConvocatoria = (id: number, datos: ConvocatoriaRequest) =>
    dato<Convocatoria>(api.put(`${EMPRESA}/${id}`, datos))

export const publicarConvocatoria = (id: number) =>
    dato<Convocatoria>(api.post(`${EMPRESA}/${id}/publicar`))

export const cerrarConvocatoria = (id: number) =>
    dato<Convocatoria>(api.post(`${EMPRESA}/${id}/cerrar`))

export const cambiarFechaCierre = (id: number, fechaCierre: string) =>
    dato<Convocatoria>(api.patch(`${EMPRESA}/${id}/fecha-cierre`, { fechaCierre }))

export async function eliminarConvocatoria(id: number): Promise<void> {
    await api.delete(`${EMPRESA}/${id}`)
}

export const moverConvocatoria = (id: number, idBusqueda: number | null) =>
    dato<Convocatoria>(api.put(`${EMPRESA}/${id}/busqueda`, { idBusqueda }))


export interface Busqueda {
    id: number
    nombre: string
    fechaCreacion: string
}

const BUSQUEDAS = '/api/empresa/busquedas'

export const listarBusquedas = () =>
    lista<Busqueda>(api.get(BUSQUEDAS))

export const crearBusqueda = (nombre: string) =>
    dato<Busqueda>(api.post(BUSQUEDAS, { nombre }))

export const renombrarBusqueda = (id: number, nombre: string) =>
    dato<Busqueda>(api.put(`${BUSQUEDAS}/${id}`, { nombre }))

export async function eliminarBusqueda(id: number): Promise<void> {
    await api.delete(`${BUSQUEDAS}/${id}`)
}


export interface FiltrosFeed {
    texto?: string
    tipo?: TipoConvocatoria
    departamento?: string
    provincia?: string
    distrito?: string
}

export async function buscarConvocatorias(filtros: FiltrosFeed = {}): Promise<Convocatoria[]> {
    const params = Object.fromEntries(Object.entries(filtros).filter(([, v]) => v !== undefined && v !== ''))
    return lista<Convocatoria>(api.get('/api/public/convocatorias', { params }))
}


const GUARDADAS = '/api/postulantes/me/convocatorias-guardadas'

export const listarGuardadas = () =>
    lista<Convocatoria>(api.get(GUARDADAS))

export async function guardarConvocatoria(id: number): Promise<void> {
    await api.put(`${GUARDADAS}/${id}`)
}

export async function quitarGuardada(id: number): Promise<void> {
    await api.delete(`${GUARDADAS}/${id}`)
}

export function estaDisponible(c: Pick<Convocatoria, 'estado' | 'fechaCierre'>, hoy = new Date()): boolean {
    const hoyIso = new Date(hoy.getTime() - hoy.getTimezoneOffset() * 60000).toISOString().slice(0, 10)
    return c.estado === 'PUBLICADA' && (!c.fechaCierre || c.fechaCierre >= hoyIso)
}
