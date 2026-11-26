import React, { useState } from 'react';
import { Link, Navigate, useNavigate, useLocation } from 'react-router-dom';
import { Mail, Lock, Eye, EyeOff, ArrowRight, ShieldCheck, Building2, TrendingUp, AlertCircle, CheckCircle2 } from 'lucide-react';
import { destinoPara, useAuth } from './AuthContext';
import { RUTA_COMPLETAR_REGISTRO } from './session';
import { iniciarSesionCognito, mensajeCognito } from './cognito';

export const Login: React.FC = () => {
  const [correo, setCorreo] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const navigate = useNavigate();
  const location = useLocation();
  const { estado, iniciarSesion } = useAuth();

  const searchParams = new URLSearchParams(location.search);
  const isExpired = searchParams.get('expired') === 'true';
  const cuentaVerificada = searchParams.get('verificada') === 'true';
  const rutaPedida = (location.state as { from?: { pathname?: string } } | null)?.from?.pathname;

  if (estado.tipo === 'autenticado' && !loading) {
    return <Navigate to={destinoPara(estado.sesion, rutaPedida)} replace />;
  }
  if (estado.tipo === 'sinRegistro' && !loading) {
    return <Navigate to={RUTA_COMPLETAR_REGISTRO} replace />;
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    if (!correo.trim() || !password) {
      setErrorMessage('Por favor ingresa tu correo electrónico y contraseña.');
      return;
    }

    setLoading(true);
    try {
      await iniciarSesionCognito(correo, password);
      navigate(await iniciarSesion(rutaPedida), { replace: true });
    } catch (err) {
      const codigoError = (err as { code?: string })?.code;
      if (codigoError === 'UserNotConfirmedException') {
        navigate(`/registro/verificar?correo=${encodeURIComponent(correo.trim())}`);
        return;
      }
      setErrorMessage(codigoError ? mensajeCognito(err) : err instanceof Error ? err.message : mensajeCognito(err));
    } finally {
      setLoading(false);
    }
  };

  return (
      <div className="login-page-layout">
        <div className="login-split-card">
          <div className="login-visual-panel">
            <img
                src="/images/hero-miners.jpg"
                alt="Especialistas y trabajadores mineros observando el tajo abierto"
                className="login-photo-bg"
            />
            <div className="visual-photo-overlay"></div>
            <div className="visual-content">
              <div className="visual-header">
                <span className="visual-kicker">TALENTO QUE IMPULSA UN MUNDO MÁS GRANDE</span>
                <h1 className="visual-title">Inicia sesión</h1>
                <p className="visual-subtitle">
                  Accede a tu cuenta como empresa, contratista o postulante y continúa en la plataforma del sector minero.
                </p>
                <div className="visual-handwritten-note">
                  Un mejor futuro para la minería
                </div>
              </div>
              <div className="visual-badges">
                <div className="visual-badge">
                  <div>
                    <strong>MÁS OPORTUNIDADES</strong>
                    <span className="badge-sub">Paradas de planta, proyectos y tiempo completo</span>
                  </div>
                </div>
                <div className="visual-badge">
                  <div>
                    <strong>PERSONAS QUE HACEN LA DIFERENCIA</strong>
                    <span className="badge-sub">Mecánicos, electricistas, operadores e ingenieros</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div className="login-form-panel">
            <div className="form-card-inner">
              <div className="form-title-group">
                <h2>Bienvenido de nuevo</h2>
                <p className="form-helper-text">
                  Ingresa con tu correo y contraseña.
                </p>
              </div>
              {cuentaVerificada && (
                  <div className="alert-message success" role="status">
                    <CheckCircle2 size={18} />
                    <span>Tu correo fue verificado. Ya puedes iniciar sesión.</span>
                  </div>
              )}
              {isExpired && (
                  <div className="alert-message warning">
                    <AlertCircle size={18} />
                    <span>Tu sesión ha expirado por seguridad. Por favor ingresa nuevamente.</span>
                  </div>
              )}
              {errorMessage && (
                  <div className="alert-message error">
                    <AlertCircle size={18} />
                    <span>{errorMessage}</span>
                  </div>
              )}
              <form onSubmit={handleSubmit} className="auth-form">
                <div className="form-field">
                  <label htmlFor="correo">Correo electrónico</label>
                  <div className="input-with-icon">
                    <Mail size={18} className="field-icon" />
                    <input
                        id="correo"
                        type="email"
                        value={correo}
                        onChange={(e) => setCorreo(e.target.value)}
                        placeholder="tu@correo.com"
                        required
                        autoComplete="email"
                    />
                  </div>
                </div>
                <div className="form-field">
                  <label htmlFor="password">Contraseña</label>
                  <div className="input-with-icon">
                    <Lock size={18} className="field-icon" />
                    <input
                        id="password"
                        type={showPassword ? 'text' : 'password'}
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        placeholder="Ingresa tu contraseña"
                        required
                        autoComplete="current-password"
                    />
                    <button
                        type="button"
                        className="password-toggle-btn"
                        onClick={() => setShowPassword(!showPassword)}
                        aria-label="Mostrar u ocultar contraseña"
                    >
                      {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                    </button>
                  </div>
                </div>
                <div className="form-aux-row">
                  <Link to="/recuperar-password" className="forgot-password-link">
                    ¿Olvidaste tu contraseña?
                  </Link>
                </div>
                <button type="submit" className="btn btn-primary btn-submit w-full" disabled={loading}>
                  {loading ? 'Verificando credenciales...' : 'Iniciar sesión'}
                  <ArrowRight size={18} />
                </button>
              </form>
              <div className="form-register-redirect">
                <span>¿No tienes cuenta?</span>{' '}
                <span className="register-options">
                <Link to="/registro/postulante" className="text-orange-bold">Regístrate</Link>
              </span>
              </div>
              <div className="login-footer-trust-grid">
                <div className="trust-mini-card">
                  <div className="mini-icon">
                    <ShieldCheck size={18} />
                  </div>
                  <strong>Acceso seguro</strong>
                  <p>Tus datos están protegidos</p>
                </div>
                <div className="trust-mini-card">
                  <div className="mini-icon">
                    <Building2 size={18} />
                  </div>
                  <strong>Empresas verificadas</strong>
                  <p>Conectamos talento con oportunidades</p>
                </div>
                <div className="trust-mini-card">
                  <div className="mini-icon">
                    <TrendingUp size={18} />
                  </div>
                  <strong>Oportunidades reales</strong>
                  <p>Impulsa tu desarrollo profesional</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
  );
};

export default Login;
