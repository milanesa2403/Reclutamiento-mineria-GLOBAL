import { api } from './client.ts'
import type { ApiResponse } from './types.ts'

export interface TipoCertificacion {
    id: number
    nombre: string
}

export async function listarTiposCertificacion(): Promise<TipoCertificacion[]> {
    const res = await api.get<ApiResponse<TipoCertificacion[]>>('/api/public/catalogo-certificaciones')
    return res.data.data ?? []
}
