import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { MailCheck } from 'lucide-react'
import { confirmarCuenta, mensajeCognito, reenviarCodigo } from './cognito.ts'
import './VerificarEmail.css'

export default function VerificarEmail() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [correo, setCorreo] = useState(searchParams.get('correo') ?? '')
  const [codigo, setCodigo] = useState('')
  const [mensaje, setMensaje] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [cargando, setCargando] = useState(false)

  async function verificar(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    setError(null)
    setMensaje(null)
    if (!correo.trim() || !/^\d{6}$/.test(codigo.trim())) {
      setError('Ingresa tu correo y el código de 6 dígitos que te enviamos.')
      return
    }
    setCargando(true)
    try {
      await confirmarCuenta(correo, codigo)
      navigate('/login?verificada=true', { replace: true })
    } catch (err) {
      setError(mensajeCognito(err))
    } finally {
      setCargando(false)
    }
  }

  async function reenviar() {
    setError(null)
    setMensaje(null)
    if (!correo.trim()) {
      setError('Ingresa tu correo para enviarte un nuevo código.')
      return
    }
    try {
      await reenviarCodigo(correo)
      setMensaje('Te enviamos un nuevo código. Revisa también la carpeta de spam.')
    } catch (err) {
      setError(mensajeCognito(err))
    }
  }

  return (
      <div className="container verificar-email">
        <section className="form-page-container verificar-email-tarjeta">
          <div className="verificar-email-icono">
            <MailCheck size={30} />
          </div>
          <div className="form-header-box">
            <h1>Verificación de correo</h1>
            <p>Te enviamos un código de 6 dígitos. Ingrésalo para activar tu cuenta.</p>
          </div>
          {error && <div className="alert-message error" role="alert">{error}</div>}
          {mensaje && <div className="alert-message success" role="status">{mensaje}</div>}
          <form onSubmit={verificar} className="custom-registration-form" noValidate>
            <div className="campo">
              <label htmlFor="correo">Correo electrónico</label>
              <input id="correo" type="email" autoComplete="email" value={correo} onChange={(e) => setCorreo(e.target.value)} />
            </div>
            <div className="campo">
              <label htmlFor="codigo">Código de verificación</label>
              <input id="codigo" inputMode="numeric" autoComplete="one-time-code" maxLength={6} value={codigo}
                     onChange={(e) => setCodigo(e.target.value.replace(/\D/g, '').slice(0, 6))} />
            </div>
            <button type="submit" className="btn btn-primary" disabled={cargando}>
              {cargando ? 'Verificando…' : 'Verificar correo'}
            </button>
          </form>
          <div className="verificar-email-pie">
            <button type="button" className="verificar-email-enlace" onClick={() => void reenviar()}>
              Reenviar código
            </button>
            <Link to="/login" className="verificar-email-enlace">Volver a iniciar sesión</Link>
          </div>
        </section>
      </div>
  )
}
