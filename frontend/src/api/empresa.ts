import { api } from './client.ts'
import type { ApiResponse } from './types.ts'


export type EstadoEmpresa = | 'PENDIENTE_DOCUMENTO' | 'EN_REVISION' | 'OBSERVADA' | 'APROBADA' | 'SUSPENDIDA' | 'BLOQUEADA' | 'RECHAZADA'


export const ETIQUETA_ESTADO: Record<EstadoEmpresa, string> = {
    PENDIENTE_DOCUMENTO: 'Falta documento',
    EN_REVISION: 'En revisión',
    OBSERVADA: 'Observada',
    APROBADA: 'Aprobada',
    SUSPENDIDA: 'Suspendida',
    BLOQUEADA: 'Bloqueada',
    RECHAZADA: 'Rechazada',
}

export type TipoEvento =
    | 'REGISTRADA' | 'DOCUMENTO_ENVIADO' | 'APROBADA' | 'OBSERVADA' | 'RECHAZADA'
    | 'ADVERTENCIA' | 'SUSPENDIDA' | 'REACTIVADA' | 'BLOQUEADA'

export const ETIQUETA_EVENTO: Record<TipoEvento, string> = {
    REGISTRADA: 'Solicitó su registro',
    DOCUMENTO_ENVIADO: 'Envió su documento',
    APROBADA: 'Aprobada',
    OBSERVADA: 'Observada',
    RECHAZADA: 'Rechazada',
    ADVERTENCIA: 'Advertencia',
    SUSPENDIDA: 'Suspendida',
    REACTIVADA: 'Reactivada',
    BLOQUEADA: 'Bloqueada',
}

export interface DocumentoInfo {
    nombreOriginal: string
    tipoMime: string
    tamanoBytes: number
    fechaSubida: string
}

export interface UrlTemporal {
    url: string
    expiraEnSegundos: number
}

export const FORMATOS_ACREDITACION = '.pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png'
export const MAX_BYTES_DOCUMENTO = 5 * 1024 * 1024


export interface SolicitudEmpresa {
    pais: string
    ruc: string
    razonSocial: string
    estado: EstadoEmpresa
    motivoRevision: string | null
    fechaRevision: string | null
    documentoAcreditacion: DocumentoInfo | null
    puedeSubirDocumento: boolean
    disponibleDesde: string | null
}

export async function obtenerMiSolicitud(): Promise<SolicitudEmpresa> {
    const res = await api.get<ApiResponse<SolicitudEmpresa>>('/api/empresa/solicitud')
    if (!res.data.data) throw new Error('No se recibió la solicitud de la empresa.')
    return res.data.data
}

export async function subirDocumentoAcreditacion(archivo: File): Promise<SolicitudEmpresa> {
    const datos = new FormData()
    datos.append('archivo', archivo)
    const res = await api.post<ApiResponse<SolicitudEmpresa>>('/api/empresa/solicitud/documento', datos, {
        timeout: 60000,
    })
    if (!res.data.data) throw new Error('No se recibió la solicitud actualizada.')
    return res.data.data
}

export async function urlMiDocumento(): Promise<UrlTemporal> {
    const res = await api.get<ApiResponse<UrlTemporal>>('/api/empresa/solicitud/documento/url')
    if (!res.data.data) throw new Error('No se recibió la URL del documento.')
    return res.data.data
}


export interface PerfilEmpresa {
    idEmpresa: number
    pais: string
    ruc: string
    razonSocial: string
    direccion: string | null
    telefono: string | null
    correoCorporativo: string
    sectorMinero: string | null
    idSectorMinero: number | null
    descripcion: string | null
    imagenUrl: string | null
    bannerUrl: string | null
    tipoDocumentoRepresentante: string
    dniRepresentante: string
    nombresRepresentante: string
    apellidosRepresentante: string
    telefonoRepresentante: string
    direccionRepresentante: string | null
    cargoRepresentante: string | null
    estado: EstadoEmpresa | null
}

export interface ActualizarPerfilEmpresa {
    telefono?: string
    idSectorMinero?: number
    descripcion?: string
    telefonoRepresentante?: string
    cargoRepresentante?: string
}

export async function obtenerPerfilEmpresa(): Promise<PerfilEmpresa> {
    return (await api.get<PerfilEmpresa>('/api/empresa/perfil')).data
}

export async function actualizarPerfilEmpresa(datos: ActualizarPerfilEmpresa): Promise<PerfilEmpresa> {
    return (await api.put<PerfilEmpresa>('/api/empresa/perfil', datos)).data
}


export interface EmpresaAdminItem {
    idEmpresa: number
    pais: string
    ruc: string
    razonSocial: string
    sectorMinero: string | null
    correoCorporativo: string
    fechaRegistro: string | null
    estado: EstadoEmpresa
    tieneDocumento: boolean
}

export interface SolicitanteEmpresa {
    pais: string
    tipoDocumento: string
    dni: string
    nombres: string
    apellidos: string
    cargo: string | null
    correo: string
    telefono: string
}

export interface EmpresaAdminDetalle {
    idEmpresa: number
    pais: string
    ruc: string
    razonSocial: string
    direccion: string | null
    telefono: string | null
    correoCorporativo: string
    sectorMinero: string | null
    descripcion: string | null
    fechaRegistro: string | null
    estado: EstadoEmpresa
    motivoRevision: string | null
    fechaRevision: string | null
    revisadoPor: string | null
    documentoAcreditacion: DocumentoInfo | null
    solicitantes: SolicitanteEmpresa[]
    rechazosAnteriores: { motivo: string; fechaRechazo: string }[]
    advertencias: number
    historial: { tipo: TipoEvento; motivo: string | null; realizadoPor: string | null; fecha: string }[]

}

function datos<T>(res: { data: ApiResponse<T> }): T {
    if (res.data.data === null || res.data.data === undefined) throw new Error('Respuesta vacía del servidor.')
    return res.data.data
}

export async function listarEmpresasAdmin(estado?: EstadoEmpresa): Promise<EmpresaAdminItem[]> {
    return datos(await api.get<ApiResponse<EmpresaAdminItem[]>>('/api/admin/empresas', {
        params: estado ? { estado } : undefined,
    }))
}

export async function detalleEmpresaAdmin(id: number): Promise<EmpresaAdminDetalle> {
    return datos(await api.get<ApiResponse<EmpresaAdminDetalle>>(`/api/admin/empresas/${id}`))
}

export async function urlDocumentoEmpresaAdmin(id: number): Promise<UrlTemporal> {
    return datos(await api.get<ApiResponse<UrlTemporal>>(`/api/admin/empresas/${id}/documento/url`))
}

export async function aprobarEmpresa(id: number): Promise<EmpresaAdminDetalle> {
    return datos(await api.post<ApiResponse<EmpresaAdminDetalle>>(`/api/admin/empresas/${id}/aprobar`))
}

export async function observarEmpresa(id: number, motivo: string): Promise<EmpresaAdminDetalle> {
    return datos(await api.post<ApiResponse<EmpresaAdminDetalle>>(`/api/admin/empresas/${id}/observar`, { motivo }))
}

export async function rechazarEmpresa(id: number, motivo: string): Promise<EmpresaAdminDetalle> {
    return datos(await api.post<ApiResponse<EmpresaAdminDetalle>>(`/api/admin/empresas/${id}/rechazar`, { motivo }))
}

export function fechaCorta(isoFecha: string): string {
    const [anio, mes, dia] = isoFecha.slice(0, 10).split('-')
    return `${dia}/${mes}/${anio}`
}


