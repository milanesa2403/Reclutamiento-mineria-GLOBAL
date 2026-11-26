import { api } from './client.ts'
import type { ApiResponse } from './types.ts'


export interface Lugar {
    codigo: string
    nombre: string
}

export interface Ubicacion {
    codigo: string
    departamento: string
    provincia: string
    distrito: string
}

export async function listarDepartamentos(): Promise<Lugar[]> {
    const res = await api.get<ApiResponse<Lugar[]>>('/api/public/ubigeo/departamentos')
    return res.data.data ?? []
}

export async function listarProvincias(codDepartamento: string): Promise<Lugar[]> {
    const res = await api.get<ApiResponse<Lugar[]>>(`/api/public/ubigeo/departamentos/${codDepartamento}/provincias`)
    return res.data.data ?? []
}

export async function listarDistritos(codProvincia: string): Promise<Lugar[]> {
    const res = await api.get<ApiResponse<Lugar[]>>(`/api/public/ubigeo/provincias/${codProvincia}/distritos`)
    return res.data.data ?? []
}

export function formatearUbicacion(u: Ubicacion | null | undefined): string {
    if (!u) return ''
    const t = (s: string) => s.toLowerCase().replace(/(^|\s|-)\S/g, (c) => c.toUpperCase())
    return `${t(u.distrito)}, ${t(u.provincia)} (${t(u.departamento)})`
}
