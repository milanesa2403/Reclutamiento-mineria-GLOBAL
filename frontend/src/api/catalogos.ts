import { api } from './client.ts'
import type { ApiResponse } from './types.ts'

export interface SectorMinero {
  id: number
  nombre: string
}

export async function listarSectoresMineros(): Promise<SectorMinero[]> {
  const res = await api.get<ApiResponse<SectorMinero[]>>('/api/public/sectores-mineros')
  return res.data.data ?? []
}

export interface EspecialidadCatalogo {
  id: number
  nombre: string
  descripcion: string | null
}

export interface HabilidadCatalogo {
  id: number
  nombre: string
  categoria: string | null
}

export async function listarEspecialidadesCatalogo(): Promise<EspecialidadCatalogo[]> {
  const res = await api.get<ApiResponse<EspecialidadCatalogo[]>>('/api/public/catalogo-especialidades')
  return res.data.data ?? []
}

export async function listarHabilidadesCatalogo(): Promise<HabilidadCatalogo[]> {
  const res = await api.get<ApiResponse<HabilidadCatalogo[]>>('/api/public/catalogo-habilidades')
  return res.data.data ?? []
}
