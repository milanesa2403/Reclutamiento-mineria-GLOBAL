import { api } from './client.ts'
import type { ApiResponse } from './types.ts'

export interface HealthData {
  baseDeDatos: 'UP' | 'DOWN'
  tablas?: string[]
  error?: string
  hora: string
}

export async function obtenerSalud(): Promise<HealthData> {
  const res = await api.get<ApiResponse<HealthData>>('/api/health')
  return res.data.data as HealthData
}
