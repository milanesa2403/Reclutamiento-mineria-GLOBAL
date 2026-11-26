import { api, dato, lista } from './client.ts'
import type { UrlTemporal } from './empresa.ts'
import type { DocumentosPostulante, TipoDocumentoPostulante } from './postulante.ts'
import type { EstadoConvocatoria, TipoConvocatoria } from './convocatorias.ts'
import type { Ubicacion } from './ubigeo.ts'


export type EstadoPostulacion = 'POSTULADO' | 'EN_REVISION' | 'ENTREVISTA' | 'ACEPTADO' | 'DESCARTADO' | 'CONFIRMADO' | 'RETIRADO'


export interface OpcionCertificado {
    idCertificacion: number
    nombre: string
    tienePdf: boolean
    coincide: boolean
}

export interface RequisitoDeConvocatoria {
    idRequisito: number
    nombre: string
    requierePdf: boolean
    cumplible: boolean
    opciones: OpcionCertificado[]
}

export interface RequisitosPostulacion {
    perfilCompleto: boolean
    tieneCv: boolean
    tieneCertificaciones: boolean
    puedePostular: boolean
    faltantes: string[]
    certificadosRequeridos: RequisitoDeConvocatoria[]
}

export const listarRequisitos = (idConvocatoria?: number) =>
    dato<RequisitosPostulacion>(api.get('/api/postulaciones/requisitos', { params: { idConvocatoria }, }))

export interface PostularRequest {
    idConvocatoria: number
    idsCertificaciones?: number[]
    requisitos?: Array<{ idRequisito: number; idCertificacion: number }>
}

export interface MiPostulacion {
    idPostulacion: number
    idConvocatoria: number
    puesto: string
    empresa: string
    tipo: TipoConvocatoria
    campamento: string
    estadoConvocatoria: EstadoConvocatoria
    estado: EstadoPostulacion
    fechaPostulacion: string
    fechaCambioEstado: string | null
    certificacionesAdjuntas: string[]
    ubicacion: Ubicacion | null
    fechaCierre: string | null
    fechaLimiteConfirmacion: string | null
    motivoRetiro: string | null
    visto: boolean
    entrevista: Entrevista | null
    logoUrl: string | null
    turno: string | null
    fechaInicioLabores: string | null
    fechaFinLabores: string | null
    direccion: string | null
    referencia: string | null
    descripcion: string | null
}

export interface Entrevista {
    fecha: string
    modalidad: 'PRESENCIAL' | 'VIRTUAL'
    lugar: string
    mensaje: string | null
}

export const postular = (datos: PostularRequest) =>
    dato<MiPostulacion>(api.post('/api/postulaciones', datos))

export const listarMisPostulaciones = () =>
    lista<MiPostulacion>(api.get('/api/postulaciones/mias'))

export async function confirmarPostulacion(id: number): Promise<void> {
    await api.patch(`/api/postulaciones/${id}/confirmar`)
}

export async function retirarPostulacion(id: number): Promise<void> {
    await api.patch(`/api/postulaciones/${id}/retirar`)
}

export async function marcarPostulacionesVistas(): Promise<void> {
    await api.patch('/api/postulaciones/vistas')
}

export async function citarEntrevista(postulaciones: number[], entrevista: Entrevista): Promise<void> {
    await api.post('/api/empresa/entrevistas', { postulaciones, entrevista })
}


export interface Candidato {
    idPostulacion: number
    nombres: string
    apellidos: string
    correo: string
    telefono: string
    especialidadPrincipal: string | null
    aniosExperiencia: number
    certificaciones: Array<{ idCertificacion: number; nombre: string; entidadEmisora: string; tienePdf: boolean }>
    documentos: DocumentosPostulante
    estado: EstadoPostulacion
    fechaPostulacion: string
    fechaCambioEstado: string | null
    requisitosCubiertos: Array<{
        requisito: string
        requierePdf: boolean
        idCertificacion: number
        certificacion: string
        tienePdf: boolean
        coincide: boolean
    }>
    resumenProfesional: string | null
    especialidades: Array<{ nombre: string; principal: boolean }>
    habilidades: string[]
    experiencias: Array<{ puesto: string; empresa: string; fechaInicio: string | null; fechaFin: string | null; descripcion: string | null }>
    formaciones: Array<{ institucion: string; carreraGrado: string; fechaInicio: string | null; fechaFin: string | null }>
    motivoRetiro: string | null
    entrevista: Entrevista | null
}

export const listarCandidatos = (idConvocatoria: number) =>
    lista<Candidato>(api.get(`/api/empresa/convocatorias/${idConvocatoria}/postulaciones`))

export const cambiarEstadoPostulacion = (idPostulacion: number, estado: EstadoPostulacion) =>
    dato<Candidato>(api.patch(`/api/empresa/postulaciones/${idPostulacion}/estado`, { estado }))

export const urlDocumentoCandidato = (idPostulacion: number, tipo: TipoDocumentoPostulante) =>
    dato<UrlTemporal>(api.get(`/api/empresa/postulaciones/${idPostulacion}/documentos/${tipo}/url`))

export const urlCertificadoCandidato = (idPostulacion: number, idCertificacion: number) =>
    dato<UrlTemporal>(api.get(`/api/empresa/postulaciones/${idPostulacion}/certificaciones/${idCertificacion}/url`))
