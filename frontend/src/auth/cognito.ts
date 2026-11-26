import {
  AuthenticationDetails,
  CognitoUser,
  CognitoUserAttribute,
  CognitoUserPool,
  type CognitoUserSession,
} from 'amazon-cognito-identity-js'
import axios from 'axios'

interface Configuracion {
  region: string
  userPoolId: string
  clientId: string
}

const API_URL: string = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

let pool: Promise<CognitoUserPool> | null = null

export class CognitoNoConfigurado extends Error {
  constructor() {
    super('El inicio de sesión no está disponible todavía. Intenta en unos minutos.')
  }
}

function obtenerPool(): Promise<CognitoUserPool> {
  if (!pool) {
    pool = axios
        .get<{ data: Configuracion }>(`${API_URL}/api/public/configuracion`, { timeout: 15000 })
        .then((res) => {
          const cfg = res.data.data
          if (!cfg.userPoolId || !cfg.clientId) throw new CognitoNoConfigurado()
          return new CognitoUserPool({ UserPoolId: cfg.userPoolId, ClientId: cfg.clientId })
        })
        .catch((error: unknown) => {
          pool = null
          throw error
        })
  }
  return pool
}

function usuario(p: CognitoUserPool, correo: string): CognitoUser {
  return new CognitoUser({ Username: correo.trim().toLowerCase(), Pool: p })
}

export async function iniciarSesionCognito(correo: string, password: string): Promise<void> {
  const p = await obtenerPool()
  const limpio = correo.trim().toLowerCase()
  return new Promise((ok, error) => {
    usuario(p, limpio).authenticateUser(
        new AuthenticationDetails({ Username: limpio, Password: password }),
        { onSuccess: () => ok(), onFailure: (e: unknown) => error(e) },
    )
  })
}

export async function registrarCuenta(correo: string, password: string): Promise<void> {
  const p = await obtenerPool()
  const limpio = correo.trim().toLowerCase()
  return new Promise((ok, error) => {
    p.signUp(limpio, password, [new CognitoUserAttribute({ Name: 'email', Value: limpio })], [], (e) =>
        e ? error(e) : ok(),
    )
  })
}

export async function confirmarCuenta(correo: string, codigo: string): Promise<void> {
  const p = await obtenerPool()
  return new Promise((ok, error) => {
    usuario(p, correo).confirmRegistration(codigo.trim(), true, (e) => (e ? error(e) : ok()))
  })
}

export async function reenviarCodigo(correo: string): Promise<void> {
  const p = await obtenerPool()
  return new Promise((ok, error) => {
    usuario(p, correo).resendConfirmationCode((e) => (e ? error(e) : ok()))
  })
}

export async function olvidePassword(correo: string): Promise<void> {
  const p = await obtenerPool()
  return new Promise((ok, error) => {
    usuario(p, correo).forgotPassword({ onSuccess: () => ok(), onFailure: (e) => error(e), inputVerificationCode: () => ok() })
  })
}

export async function nuevaPassword(correo: string, codigo: string, password: string): Promise<void> {
  const p = await obtenerPool()
  return new Promise((ok, error) => {
    usuario(p, correo).confirmPassword(codigo.trim(), password, { onSuccess: () => ok(), onFailure: (e) => error(e) })
  })
}

function sesionActual(p: CognitoUserPool): Promise<CognitoUserSession | null> {
  const u = p.getCurrentUser()
  if (!u) return Promise.resolve(null)
  return new Promise((ok) => {
    u.getSession((e: Error | null, sesion: CognitoUserSession | null) => ok(e || !sesion?.isValid() ? null : sesion))
  })
}

export async function tokenVigente(): Promise<string | null> {
  try {
    const p = await obtenerPool()
    const sesion = await sesionActual(p)
    return sesion ? sesion.getIdToken().getJwtToken() : null
  } catch {
    return null
  }
}

export function haySesionGuardada(): boolean {
  return Object.keys(localStorage).some((k) => k.startsWith('CognitoIdentityServiceProvider.') && k.endsWith('.idToken'))
}

export async function cerrarSesionCognito(): Promise<void> {
  try {
    const p = await obtenerPool()
    p.getCurrentUser()?.signOut()
  } catch {
    Object.keys(localStorage)
        .filter((k) => k.startsWith('CognitoIdentityServiceProvider.'))
        .forEach((k) => localStorage.removeItem(k))
  }
}

export function mensajeCognito(error: unknown): string {
  const e = error as { code?: string; name?: string; message?: string }
  switch (e?.code ?? e?.name) {
    case 'NotAuthorizedException':
      return 'El correo o la contraseña no son válidos.'
    case 'UserNotConfirmedException':
      return 'Tu correo aún no está verificado. Ingresa el código que te enviamos.'
    case 'UsernameExistsException':
      return 'Ya existe una cuenta con ese correo.'
    case 'CodeMismatchException':
      return 'El código no es correcto. Revisa e intenta de nuevo.'
    case 'ExpiredCodeException':
      return 'El código venció. Pide uno nuevo.'
    case 'InvalidPasswordException':
      return 'La contraseña no cumple la política: mínimo 12 caracteres con minúscula, mayúscula, número y símbolo.'
    case 'LimitExceededException':
    case 'TooManyRequestsException':
      return 'Demasiados intentos. Espera unos minutos.'
    case 'UserNotFoundException':
      return 'El correo o la contraseña no son válidos.'
    default:
      return e instanceof CognitoNoConfigurado ? e.message : e?.message || 'No se pudo completar la operación.'
  }
}
