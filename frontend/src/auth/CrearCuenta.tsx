import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { mensajeCognito, registrarCuenta } from './cognito.ts'
import { problemaPassword, REGLA_PASSWORD } from './password.ts'

export type TipoCuenta = 'postulante' | 'empresa'

export const CLAVE_TIPO_CUENTA = 'mintra.tipoCuenta'

const TEXTOS: Record<TipoCuenta, { titulo: string; detalle: string; correo: string }> = {
  postulante: {
    titulo: 'Crea tu cuenta de postulante',
    detalle: 'Primero creamos tu acceso. Después de verificar tu correo e iniciar sesión, completas tus datos personales.',
    correo: 'Correo electrónico',
  },
  empresa: {
    titulo: 'Crea tu cuenta de empresa',
    detalle: 'Primero creamos el acceso de la empresa. Después de verificar el correo e iniciar sesión, registras los datos de la empresa y del representante.',
    correo: 'Correo electrónico corporativo',
  },
}

export default function CrearCuenta({ tipo }: { tipo: TipoCuenta }) {
  const navigate = useNavigate()
  const [correo, setCorreo] = useState('')
  const [password, setPassword] = useState('')
  const [confirmacion, setConfirmacion] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)
  const textos = TEXTOS[tipo]

  async function enviar(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    setError(null)
    const limpio = correo.trim().toLowerCase()
    if (limpio.length > 150 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(limpio)) {
      setError('Ingresa un correo válido, por ejemplo nombre@dominio.com.')
      return
    }
    const problema = problemaPassword(password)
    if (problema) {
      setError(problema)
      return
    }
    if (password !== confirmacion) {
      setError('Las contraseñas no coinciden.')
      return
    }
    setEnviando(true)
    try {
      await registrarCuenta(limpio, password)
      localStorage.setItem(CLAVE_TIPO_CUENTA, tipo)
      navigate(`/registro/verificar?correo=${encodeURIComponent(limpio)}`)
    } catch (err) {
      setError(mensajeCognito(err))
    } finally {
      setEnviando(false)
    }
  }

  return (
      <div className="container" style={{ padding: '3rem 1rem' }}>
        <section className="form-page-container">
          <div className="form-header-box">
            <h1>{textos.titulo}</h1>
            <p>{textos.detalle}</p>
          </div>
          {error && (
              <p className="form-error" role="alert">
                {error}
              </p>
          )}
          <form onSubmit={enviar} className="custom-registration-form" noValidate>
            <div className="campo">
              <label htmlFor="correo">{textos.correo}</label>
              <input id="correo" type="email" autoComplete="email" maxLength={150} value={correo}
                     onChange={(e) => setCorreo(e.target.value)} required />
            </div>
            <div className="campo">
              <label htmlFor="password">Contraseña</label>
              <input id="password" type="password" autoComplete="new-password" maxLength={256} value={password}
                     onChange={(e) => setPassword(e.target.value)} aria-describedby="password-ayuda" required />
              <p id="password-ayuda" className="ayuda">{REGLA_PASSWORD}</p>
            </div>
            <div className="campo">
              <label htmlFor="confirmacion">Repite la contraseña</label>
              <input id="confirmacion" type="password" autoComplete="new-password" maxLength={256} value={confirmacion}
                     onChange={(e) => setConfirmacion(e.target.value)} required />
            </div>
            <button type="submit" className="btn btn-primary" disabled={enviando}>
              {enviando ? 'Creando cuenta…' : 'Crear cuenta'}
            </button>
          </form>
          <p className="redirect-note text-center" style={{ marginTop: '1.25rem' }}>
            {tipo === 'postulante'
                ? <>¿Eres empresa? <Link to="/registro/empresa">Crea una cuenta de empresa</Link></>
                : <>¿Buscas empleo? <Link to="/registro/postulante">Crea una cuenta de postulante</Link></>}
          </p>
          <p className="redirect-note text-center">
            ¿Ya tienes cuenta? <Link to="/login">Inicia sesión</Link>
          </p>
        </section>
      </div>
  )
}
