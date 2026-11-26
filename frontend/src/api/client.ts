import axios from 'axios'
import { tokenVigente } from '../auth/cognito'
import type { ApiResponse } from './types'

export const API_URL: string = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

export const EVENTO_SESION_EXPIRADA = 'mineria:sesion-expirada'

export const api = axios.create({
  baseURL: API_URL,
  timeout: 15000,
})

api.interceptors.request.use(async (config) => {
  const token = await tokenVigente()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
    (response) => response,
    (error: unknown) => {
      if (
          axios.isAxiosError(error) &&
          error.response?.status === 401 &&
          error.config?.headers?.Authorization &&
          !error.config?.url?.includes('/api/auth/')
      ) {
        window.dispatchEvent(new Event(EVENTO_SESION_EXPIRADA))
      }
      return Promise.reject(error)
    },
)

type Respuesta<T> = Promise<{ data: ApiResponse<T> }>

export const dato = async <T>(peticion: Respuesta<T>): Promise<T> => (await peticion).data.data!

export const lista = async <T>(peticion: Respuesta<T[]>): Promise<T[]> => (await peticion).data.data ?? []

export function getStatus(error: unknown): number | undefined {
  return axios.isAxiosError(error) ? error.response?.status : undefined
}

export const TIMEOUT_LARGO_MS = 60000

export function textoError(error: unknown): string {
  if (axios.isAxiosError<ApiResponse<unknown>>(error)) {
    if (!error.response && (error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT')) {
      return 'El servidor está tardando en responder. Intenta nuevamente en unos segundos.'
    }
    if (!error.response) return `No se pudo conectar con el servidor (${API_URL}).`
      const data = error.response.data as { message?: string; mensaje?: string } | undefined
    return data?.message ?? data?.mensaje ?? `Error ${error.response.status}`
  }
  return 'Ocurrió un error inesperado.'
}

export function erroresPorCampo(error: unknown): Record<string, string> {
  if (axios.isAxiosError<ApiResponse<Record<string, string>>>(error)) {
    const data = error.response?.data?.data
    if (data && typeof data === 'object') return data
  }
  return {}
}

export function errorFormulario(error: unknown): string {
  const fields = Object.values(erroresPorCampo(error))
  return fields.length > 0 ? fields.join(' · ') : textoError(error)
}
