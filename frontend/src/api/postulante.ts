import { api, TIMEOUT_LARGO_MS } from './client.ts'
import type { ApiResponse } from './types.ts'
import type { DocumentoInfo, UrlTemporal } from './empresa.ts'
import type { TipoDocumento } from '../utils/documentos.ts'

export interface RegistroPostulanteRequest {
    pais: string
    tipoDocumento: TipoDocumento
    numeroDocumento: string
    nombres: string
    apellidos: string
    telefono: string
}

export async function registrarPostulante(datos: RegistroPostulanteRequest): Promise<void> {
    await api.post('/api/auth/register/postulante', datos, { timeout: TIMEOUT_LARGO_MS })
}

export interface EspecialidadDto {
    nombre: string
    principal: boolean
}

export interface ExperienciaDto {
    puesto: string
    empresa: string
    fechaInicio: string
    fechaFin: string | null
    descripcion: string | null
}

export interface FormacionDto {
    institucion: string
    carreraGrado: string
    fechaInicio: string
    fechaFin: string | null
}

export interface CertificacionDto {
    id?: number | null
    idCatalogoCertificacion: number | null
    nombre: string
    entidadEmisora: string
    fechaEmision: string | null
}

export const MAX_ELEMENTOS_PERFIL = 20

export interface PerfilProfesionalRequest {
    resumenProfesional: string | null;
    especialidades: Array<{ nombre: string; principal: boolean }>;
    habilidades: string[];
    experiencias: Array<{
        puesto: string;
        empresa: string;
        fechaInicio: string;
        fechaFin: string | null;
        descripcion: string | null;
    }>;
    formaciones: Array<{
        institucion: string;
        carreraGrado: string;
        fechaInicio: string;
        fechaFin: string | null;
    }>;
    certificaciones: CertificacionDto[];
}

export interface CertificacionGuardada {
    id: number
    idCatalogoCertificacion: number | null
    nombre: string
    entidadEmisora: string
    fechaEmision: string | null
    archivo: DocumentoInfo | null
}

export interface PerfilProfesional extends Omit<PerfilProfesionalRequest, 'resumenProfesional' | 'certificaciones'> {
    resumenProfesional: string | null
    certificaciones: CertificacionGuardada[]
    perfilCompleto: boolean
    pendientes: string[]
}

export async function obtenerPerfilProfesional(): Promise<PerfilProfesional> {
    const res = await api.get<ApiResponse<PerfilProfesional>>('/api/postulantes/me/perfil')
    if (!res.data.data) throw new Error('No se recibió el perfil profesional.')
    return res.data.data
}

export async function guardarPerfilProfesional(datos: PerfilProfesionalRequest): Promise<PerfilProfesional> {
    const res = await api.put<ApiResponse<PerfilProfesional>>('/api/postulantes/me/perfil', datos)
    if (!res.data.data) throw new Error('No se recibió el perfil guardado.')
    return res.data.data
}


export interface PerfilPostulante {
    pais: string
    tipoDocumento: string
    dni: string
    nombres: string
    apellidos: string
    correo: string
    telefono: string
    direccion: string | null
    resumenProfesional: string | null
    documentos: DocumentosPostulante
    perfilCompleto: boolean
    pendientes: string[]
}

export interface ActualizarContactoRequest {
    telefono: string
    direccion: string | null
    resumenProfesional: string | null
}

export async function obtenerMiPerfil(): Promise<PerfilPostulante> {
    const res = await api.get<ApiResponse<PerfilPostulante>>('/api/postulantes/me')
    if (!res.data.data) throw new Error('No se recibió el perfil del postulante.')
    return res.data.data
}

export async function actualizarMiContacto(datos: ActualizarContactoRequest): Promise<PerfilPostulante> {
    const res = await api.put<ApiResponse<PerfilPostulante>>('/api/postulantes/me', datos)
    if (!res.data.data) throw new Error('No se recibió el perfil actualizado.')
    return res.data.data
}


export type TipoDocumentoPostulante = 'cv' | 'foto-dni' | 'cul' | 'emo'

export interface DocumentosPostulante {
    cv: { formato: string } | null
    fotoDni: { formato: string } | null
    cul: { formato: string } | null
    emo: { formato: string } | null
}

export const DOCUMENTOS_POSTULANTE: Array<{
    tipo: TipoDocumentoPostulante
    clave: keyof DocumentosPostulante
    titulo: string
    formatos: string[]
    accept: string
}> = [
    { tipo: 'cv', clave: 'cv', titulo: 'Currículum Vitae (CV)', formatos: ['pdf'], accept: '.pdf,application/pdf' },
    { tipo: 'foto-dni', clave: 'fotoDni', titulo: 'DNI (ambas caras)', formatos: ['pdf', 'jpg', 'jpeg', 'png'],
        accept: '.pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png' },
    { tipo: 'cul', clave: 'cul', titulo: 'Certificado Único Laboral (CUL)', formatos: ['pdf'], accept: '.pdf,application/pdf' },
    { tipo: 'emo', clave: 'emo', titulo: 'Examen Médico Ocupacional (EMO)', formatos: ['pdf', 'jpg', 'jpeg', 'png'],
        accept: '.pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png' },
]

export async function subirMiDocumento(tipo: TipoDocumentoPostulante, archivo: File): Promise<DocumentosPostulante> {
    const datos = new FormData()
    datos.append('archivo', archivo)
    const res = await api.post<ApiResponse<DocumentosPostulante>>(`/api/postulantes/me/documentos/${tipo}`, datos, { timeout: 60000 })
    if (!res.data.data) throw new Error('No se recibió el documento guardado.')
    return res.data.data
}

export async function urlMiDocumento(tipo: TipoDocumentoPostulante): Promise<UrlTemporal> {
    const res = await api.get<ApiResponse<UrlTemporal>>(`/api/postulantes/me/documentos/${tipo}/url`)
    if (!res.data.data) throw new Error('No se recibió la URL del documento.')
    return res.data.data
}


export async function subirPdfCertificacion(id: number, archivo: File): Promise<DocumentoInfo> {
    const datos = new FormData()
    datos.append('archivo', archivo)
    const res = await api.post<ApiResponse<DocumentoInfo>>(`/api/postulantes/me/certificaciones/${id}/archivo`, datos, { timeout: 60000 })
    if (!res.data.data) throw new Error('No se recibió el PDF guardado.')
    return res.data.data
}

export async function urlPdfCertificacion(id: number): Promise<UrlTemporal> {
    const res = await api.get<ApiResponse<UrlTemporal>>(`/api/postulantes/me/certificaciones/${id}/archivo/url`)
    if (!res.data.data) throw new Error('No se recibió la URL del PDF.')
    return res.data.data
}
