import { type SyntheticEvent, type ClipboardEvent, type KeyboardEvent, useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { mensajeCognito, nuevaPassword as cambiarPassword, olvidePassword } from './cognito.ts'
import { problemaPassword } from './password.ts'
import './RecuperarPassword.css'

type Paso = 'correo' | 'codigo' | 'nuevaPassword' | 'exito'

const DURACION_CODIGO_SEGUNDOS = 60 * 60

function enmascararCorreo(correo: string): string {
  const [usuario, dominio] = correo.split('@')
  if (!dominio) return correo
  if (usuario.length <= 2) return `${usuario[0]}***@${dominio}`
  return `${usuario[0]}***${usuario[usuario.length - 1]}@${dominio}`
}

function formatearTiempo(segundos: number): string {
  const m = Math.floor(segundos / 60)
  const s = segundos % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

const REQUISITOS_PASSWORD = [
  { etiqueta: 'Mínimo 12 caracteres', cumple: (p: string) => p.length >= 12 },
  { etiqueta: 'Al menos una minúscula', cumple: (p: string) => /[a-z]/.test(p) },
  { etiqueta: 'Al menos una mayúscula', cumple: (p: string) => /[A-Z]/.test(p) },
  { etiqueta: 'Al menos un número', cumple: (p: string) => /[0-9]/.test(p) },
  { etiqueta: 'Al menos un carácter especial', cumple: (p: string) => /[^A-Za-z0-9]/.test(p) },
]

function IconoOjo() {
  return (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
        <path d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7-11-7-11-7z" />
        <circle cx="12" cy="12" r="3" />
      </svg>
  )
}

function IconoOjoTachado() {
  return (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
        <path d="M17.94 17.94A10.94 10.94 0 0 1 12 19c-7 0-11-7-11-7a18.6 18.6 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 7 11 7a18.5 18.5 0 0 1-2.16 3.19" />
        <path d="M14.12 14.12a3 3 0 1 1-4.24-4.24" />
        <line x1="1" y1="1" x2="23" y2="23" />
      </svg>
  )
}

export default function RecuperarPassword() {
  const navigate = useNavigate()

  const [paso, setPaso] = useState<Paso>('correo')
  const [correo, setCorreo] = useState('')
  const [digitos, setDigitos] = useState<string[]>(Array(6).fill(''))
  const [nuevaPassword, setNuevaPassword] = useState('')
  const [confirmarPassword, setConfirmarPassword] = useState('')
  const [verNuevaPassword, setVerNuevaPassword] = useState(false)
  const [verConfirmarPassword, setVerConfirmarPassword] = useState(false)

  const [segundosRestantes, setSegundosRestantes] = useState(DURACION_CODIGO_SEGUNDOS)
  const [cargando, setCargando] = useState(false)
  const [reenviando, setReenviando] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const inputsRef = useRef<(HTMLInputElement | null)[]>([])

  useEffect(() => {
    if (paso !== 'codigo') return
    if (segundosRestantes <= 0) return
    const id = setInterval(() => setSegundosRestantes((s) => Math.max(0, s - 1)), 1000)
    return () => clearInterval(id)
  }, [paso, segundosRestantes])

  const codigo = digitos.join('')
  const codigoExpirado = segundosRestantes <= 0
  const requisitosCumplidos = REQUISITOS_PASSWORD.every((r) => r.cumple(nuevaPassword))
  const passwordsCoinciden = nuevaPassword.length > 0 && nuevaPassword === confirmarPassword
  const puedeEnviarNuevaPassword = requisitosCumplidos && passwordsCoinciden && !cargando

  async function handleEnviarCorreo(e: SyntheticEvent<HTMLFormElement>): Promise<void> {
    e.preventDefault()
    setError(null)
    setCargando(true)
    try {
      await olvidePassword(correo)
      setSegundosRestantes(DURACION_CODIGO_SEGUNDOS)
      setDigitos(Array(6).fill(''))
      setPaso('codigo')
    } catch (err) {
      setError(mensajeCognito(err))
    } finally {
      setCargando(false)
    }
  }

  async function handleReenviarCodigo(): Promise<void> {
    setError(null)
    setReenviando(true)
    try {
      await olvidePassword(correo)
      setSegundosRestantes(DURACION_CODIGO_SEGUNDOS)
      setDigitos(Array(6).fill(''))
      inputsRef.current[0]?.focus()
    } catch (err) {
      setError(mensajeCognito(err))
    } finally {
      setReenviando(false)
    }
  }

  function handleCambiarDigito(index: number, valor: string): void {
    const limpio = valor.replace(/\D/g, '')
    if (!limpio) {
      setDigitos((prev) => prev.map((d, i) => (i === index ? '' : d)))
      return
    }
    setDigitos((prev) => prev.map((d, i) => (i === index ? limpio[limpio.length - 1] : d)))
    if (index < 5) inputsRef.current[index + 1]?.focus()
  }

  function handleTeclaDigito(index: number, e: KeyboardEvent<HTMLInputElement>): void {
    if (e.key === 'Backspace' && !digitos[index] && index > 0) {
      inputsRef.current[index - 1]?.focus()
    }
  }

  function handlePegarDigitos(e: ClipboardEvent<HTMLInputElement>): void {
    const texto = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6)
    if (!texto) return
    e.preventDefault()
    const nuevos = Array(6).fill('')
    texto.split('').forEach((c, i) => (nuevos[i] = c))
    setDigitos(nuevos)
    inputsRef.current[Math.min(texto.length, 5)]?.focus()
  }

  function handleContinuarConCodigo(e: SyntheticEvent<HTMLFormElement>): void {
    e.preventDefault()
    setError(null)
    if (codigoExpirado) {
      setError('El código venció. Solicita uno nuevo.')
      return
    }
    if (codigo.length !== 6) {
      setError('Ingresa los 6 dígitos del código.')
      return
    }
    setPaso('nuevaPassword')
  }

  async function handleRestablecerPassword(e: SyntheticEvent<HTMLFormElement>): Promise<void> {
    e.preventDefault()
    setError(null)
    setCargando(true)
    try {
      const problema = problemaPassword(nuevaPassword)
      if (problema) throw new Error(problema)
      await cambiarPassword(correo, codigo, nuevaPassword)
      setPaso('exito')
    } catch (err) {
      const e = err as { code?: string; name?: string }
      if ((e?.code ?? e?.name) === 'CodeMismatchException' || (e?.code ?? e?.name) === 'ExpiredCodeException') {
        setDigitos(Array(6).fill(''))
        setPaso('codigo')
      }
      setError(mensajeCognito(err))
    } finally {
      setCargando(false)
    }
  }

  useEffect(() => {
    if (paso !== 'exito') return
    const id = setTimeout(() => navigate('/login'), 2500)
    return () => clearTimeout(id)
  }, [paso, navigate])

  return (
      <section className="recuperar-password">
        {paso === 'correo' && (
            <div className="recuperar-password__card">
              <h1>Recuperación segura</h1>
              <hr className="recuperar-password__divisor" />
              {error && <p className="recuperar-password__error">{error}</p>}
              <form onSubmit={handleEnviarCorreo}>
                <label htmlFor="correo">Correo electrónico</label>
                <input
                    id="correo"
                    type="email"
                    required
                    value={correo}
                    onChange={(e) => setCorreo(e.target.value)}
                    placeholder="tu@correo.com"
                />
                <button type="submit" disabled={cargando}>
                  {cargando ? 'Enviando...' : 'Enviar código'}
                </button>
              </form>
              <a className="recuperar-password__volver" href="/login">
                ← Volver al inicio de sesión
              </a>
            </div>
        )}
        {paso === 'codigo' && (
            <div className="recuperar-password__card">
              <h1>Código de verificación</h1>
              <hr className="recuperar-password__divisor" />
              {error && <p className="recuperar-password__error">{error}</p>}
              <form onSubmit={handleContinuarConCodigo}>
                <label>Código enviado a</label>
                <div className="recuperar-password__correo-fijo">
                  <strong>{enmascararCorreo(correo)}</strong>
                </div>
                <label>Ingresa el código que enviamos a tu correo.</label>
                <div className="recuperar-password__digitos">
                  {digitos.map((d, i) => (
                      <input
                          key={i}
                          ref={(el) => {
                            inputsRef.current[i] = el
                          }}
                          type="text"
                          inputMode="numeric"
                          maxLength={1}
                          value={d}
                          disabled={codigoExpirado}
                          onChange={(e) => handleCambiarDigito(i, e.target.value)}
                          onKeyDown={(e) => handleTeclaDigito(i, e)}
                          onPaste={handlePegarDigitos}
                      />
                  ))}
                </div>
                <p className={`recuperar-password__nota ${codigoExpirado ? 'vencido' : ''}`}>
                  {codigoExpirado ? 'El código expiró' : <>El código expira en <strong>{formatearTiempo(segundosRestantes)}</strong></>}
                </p>
                <button type="submit" disabled={codigoExpirado || codigo.length !== 6}>
                  Verificar código
                </button>
                <button
                    type="button"
                    className="recuperar-password__secundario"
                    onClick={handleReenviarCodigo}
                    disabled={reenviando}
                >
                  {reenviando ? 'Reenviando...' : 'Reenviar código'}
                </button>
              </form>
              <a
                  className="recuperar-password__volver"
                  href="#"
                  onClick={(e) => {
                    e.preventDefault()
                    setPaso('correo')
                  }}
              >
                ← Volver a recuperación
              </a>
            </div>
        )}
        {paso === 'nuevaPassword' && (
            <div className="recuperar-password__card">
              <h1>Crea tu nueva contraseña</h1>
              <hr className="recuperar-password__divisor" />
              {error && <p className="recuperar-password__error">{error}</p>}
              <form onSubmit={handleRestablecerPassword}>
                <label htmlFor="nuevaPassword">Nueva contraseña</label>
                <div className="recuperar-password__campo-password">
                  <input
                      id="nuevaPassword"
                      type={verNuevaPassword ? 'text' : 'password'}
                      required
                      value={nuevaPassword}
                      onChange={(e) => setNuevaPassword(e.target.value)}
                  />
                  <button
                      type="button"
                      className="recuperar-password__toggle-ojo"
                      onClick={() => setVerNuevaPassword((v) => !v)}
                      aria-label={verNuevaPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                  >
                    {verNuevaPassword ? <IconoOjoTachado /> : <IconoOjo />}
                  </button>
                </div>
                <label htmlFor="confirmarPassword">Confirmar nueva contraseña</label>
                <div className="recuperar-password__campo-password">
                  <input
                      id="confirmarPassword"
                      type={verConfirmarPassword ? 'text' : 'password'}
                      required
                      value={confirmarPassword}
                      onChange={(e) => setConfirmarPassword(e.target.value)}
                  />
                  <button
                      type="button"
                      className="recuperar-password__toggle-ojo"
                      onClick={() => setVerConfirmarPassword((v) => !v)}
                      aria-label={verConfirmarPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                  >
                    {verConfirmarPassword ? <IconoOjoTachado /> : <IconoOjo />}
                  </button>
                </div>
                <ul className="recuperar-password__requisitos">
                  {REQUISITOS_PASSWORD.map((r) => (
                      <li key={r.etiqueta} className={r.cumple(nuevaPassword) ? 'cumple' : ''}>
                        {r.etiqueta}
                      </li>
                  ))}
                  <li className={passwordsCoinciden ? 'cumple' : ''}>Las contraseñas coinciden</li>
                </ul>
                <button type="submit" disabled={!puedeEnviarNuevaPassword}>
                  {cargando ? 'Restableciendo...' : 'Restablecer contraseña'}
                </button>
              </form>
            </div>
        )}
        {paso === 'exito' && (
            <div className="recuperar-password__card recuperar-password__card--exito">
              <div className="recuperar-password__icono-exito">✓</div>
              <h1>Contraseña actualizada</h1>
            </div>
        )}
      </section>
  )
}
