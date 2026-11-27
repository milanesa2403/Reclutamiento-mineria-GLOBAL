import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext.tsx'
import { CLAVE_TIPO_CUENTA, type TipoCuenta } from '../../auth/CrearCuenta.tsx'
import Cargando from '../../components/Cargando.tsx'
import RegistroEmpresa from './RegistroEmpresa.tsx'
import RegistroPostulante from './RegistroPostulante.tsx'

function tipoGuardado(): TipoCuenta {
  return localStorage.getItem(CLAVE_TIPO_CUENTA) === 'empresa' ? 'empresa' : 'postulante'
}

export default function CompletarRegistro() {
  const { estado, iniciarSesion, cerrarSesion } = useAuth()
  const navigate = useNavigate()
  const [tipo, setTipo] = useState<TipoCuenta>(tipoGuardado)

  if (estado.tipo === 'cargando') return <Cargando tipo="montana" variante="pantalla" mensaje="Validando sesión" />
  if (estado.tipo === 'anonimo') return <Navigate to="/login" replace />
  if (estado.tipo === 'autenticado') return <Navigate to="/" replace />
  if (estado.tipo === 'error') {
    return (
        <div className="container" style={{ padding: '3rem 1rem', maxWidth: 560 }}>
          <div className="alert-message error" role="alert">{estado.mensaje}</div>
        </div>
    )
  }

  const alRegistrar = async () => {
    localStorage.removeItem(CLAVE_TIPO_CUENTA)
    navigate(await iniciarSesion(), { replace: true })
  }

  return (
      <div className="container" style={{ padding: '3rem 1rem' }}>
        <section className="form-page-container">
          <div className="form-header-box">
            <h1>{tipo === 'empresa' ? 'Registra tu empresa' : 'Completa tus datos'}</h1>
            <p>
              Sesión iniciada como <strong>{estado.correo}</strong>.{' '}
              {tipo === 'empresa'
                  ? 'Luego subirás el documento de acreditación para que el administrador revise la solicitud.'
                  : 'Luego armarás tu perfil profesional y subirás tu CV.'}
            </p>
          </div>
          <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1.5rem' }}>
            <button type="button" className={`btn ${tipo === 'postulante' ? 'btn-primary' : 'btn-outline'}`}
                    onClick={() => setTipo('postulante')}>Soy postulante</button>
            <button type="button" className={`btn ${tipo === 'empresa' ? 'btn-primary' : 'btn-outline'}`}
                    onClick={() => setTipo('empresa')}>Soy empresa</button>
          </div>
          {tipo === 'empresa'
              ? <RegistroEmpresa alRegistrar={alRegistrar} />
              : <RegistroPostulante alRegistrar={alRegistrar} />}
          <p className="redirect-note text-center" style={{ marginTop: '1.25rem' }}>
            <button type="button" className="btn btn-outline" onClick={() => cerrarSesion('/')}>Cerrar sesión</button>
          </p>
        </section>
      </div>
  )
}
