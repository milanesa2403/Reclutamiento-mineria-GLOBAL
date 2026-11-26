import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { api, EVENTO_SESION_EXPIRADA, textoError, getStatus } from '../api/client.ts'
import { obtenerMiPerfil } from '../api/postulante.ts'
import { obtenerMiSolicitud, type EstadoEmpresa } from '../api/empresa.ts'
import { cerrarSesionCognito, haySesionGuardada, tokenVigente } from './cognito.ts'
import {
    homeFor,
    prefijoDe,
    RUTA_COMPLETAR_PERFIL,
    RUTA_COMPLETAR_REGISTRO,
    RUTA_SOLICITUD_EMPRESA,
    type Rol,
} from './session.ts'

export interface Sesion {
    rol: Rol
    correo: string
    nombre: string
    perfilCompleto: boolean
    estadoEmpresa: EstadoEmpresa | null
}

export type EstadoAuth =
    | { tipo: 'cargando' }
    | { tipo: 'anonimo'; expirada?: boolean; destino?: string; desde?: string }
    | { tipo: 'sinRegistro'; correo: string }
    | { tipo: 'autenticado'; sesion: Sesion }
    | { tipo: 'error'; rol: Rol | null; mensaje: string }

interface AuthApi {
    estado: EstadoAuth
    iniciarSesion: (rutaPedida?: string) => Promise<string>
    cerrarSesion: (destino?: string) => void
    refrescar: () => Promise<void>
}

interface Yo {
    correo: string
    rol: Rol | null
}

const AuthContext = createContext<AuthApi | null>(null)

class SesionInvalida extends Error {}

async function quienSoy(): Promise<Yo> {
    try {
        const res = await api.get<{ data: Yo }>('/api/auth/yo')
        return res.data.data
    } catch (error) {
        if (getStatus(error) === 401) throw new SesionInvalida()
        throw error
    }
}

async function cargarSesion(rol: Rol, correo: string): Promise<Sesion> {
    try {
        switch (rol) {
            case 'POSTULANTE': {
                const perfil = await obtenerMiPerfil()
                return {
                    rol,
                    correo,
                    nombre: `${perfil.nombres} ${perfil.apellidos}`.trim(),
                    perfilCompleto: perfil.perfilCompleto,
                    estadoEmpresa: null,
                }
            }
            case 'EMPRESA': {
                const solicitud = await obtenerMiSolicitud()
                return { rol, correo, nombre: solicitud.razonSocial, perfilCompleto: true, estadoEmpresa: solicitud.estado }
            }
            case 'ADMINISTRADOR':
                return { rol, correo, nombre: 'Administrador', perfilCompleto: true, estadoEmpresa: null }
        }
    } catch (error) {
        if (getStatus(error) === 401) throw new SesionInvalida()
        throw error
    }
}

export function destinoPara(sesion: Sesion, rutaPedida?: string): string {
    if (sesion.rol === 'POSTULANTE' && !sesion.perfilCompleto) return RUTA_COMPLETAR_PERFIL
    if (sesion.rol === 'EMPRESA' && sesion.estadoEmpresa !== 'APROBADA') return RUTA_SOLICITUD_EMPRESA
    if (rutaPedida && rutaPedida.startsWith(prefijoDe(sesion.rol))) return rutaPedida
    return homeFor(sesion.rol)
}

export function AuthProvider({ children }: { children: ReactNode }) {
    const [estado, setEstado] = useState<EstadoAuth>(() => (haySesionGuardada() ? { tipo: 'cargando' } : { tipo: 'anonimo' }))
    const intento = useRef(0)

    const validar = useCallback(async (): Promise<Sesion | string | null> => {
        const id = ++intento.current
        if (!(await tokenVigente())) {
            if (id === intento.current) setEstado({ tipo: 'anonimo' })
            return null
        }
        let rol: Rol | null = null
        try {
            const yo = await quienSoy()
            rol = yo.rol
            if (!yo.rol) {
                if (id === intento.current) setEstado({ tipo: 'sinRegistro', correo: yo.correo })
                return RUTA_COMPLETAR_REGISTRO
            }
            const sesion = await cargarSesion(yo.rol, yo.correo)
            if (id === intento.current) setEstado({ tipo: 'autenticado', sesion })
            return sesion
        } catch (error) {
            if (id !== intento.current) return null
            if (error instanceof SesionInvalida) {
                await cerrarSesionCognito()
                setEstado({ tipo: 'anonimo' })
                return null
            }
            setEstado({ tipo: 'error', rol, mensaje: textoError(error) })
            return null
        }
    }, [])

    useEffect(() => {
        if (haySesionGuardada()) void validar()
    }, [validar])

    useEffect(() => {
        const alExpirar = () => {
            intento.current++
            void cerrarSesionCognito()
            setEstado({ tipo: 'anonimo', expirada: true })
        }
        window.addEventListener(EVENTO_SESION_EXPIRADA, alExpirar)
        return () => window.removeEventListener(EVENTO_SESION_EXPIRADA, alExpirar)
    }, [])

    const iniciarSesion = useCallback(async (rutaPedida?: string): Promise<string> => {
        setEstado({ tipo: 'cargando' })
        const resultado = await validar()
        if (typeof resultado === 'string') return resultado
        if (!resultado) throw new Error('No se pudo iniciar la sesión. Inténtalo nuevamente.')
        return destinoPara(resultado, rutaPedida)
    }, [validar])

    const cerrarSesion = useCallback((destino = '/') => {
        intento.current++
        void cerrarSesionCognito()
        setEstado({ tipo: 'anonimo', destino, desde: window.location.pathname })
    }, [])

    const refrescar = useCallback(async () => {
        await validar()
    }, [validar])

    const valor = useMemo<AuthApi>(
        () => ({ estado, iniciarSesion, cerrarSesion, refrescar }),
        [estado, iniciarSesion, cerrarSesion, refrescar],
    )
    return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthApi {
    const ctx = useContext(AuthContext)
    if (!ctx) throw new Error('useAuth debe usarse dentro de <AuthProvider>')
    return ctx
}
