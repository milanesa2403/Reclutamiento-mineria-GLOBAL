import React from 'react';
import { Link } from 'react-router-dom';
import {
  ShieldCheck,
  Building2,
  TrendingUp,
  Users,
  Compass,
  CheckCircle2,
  ArrowRight,
  Sparkles,
  Award,
  Zap
} from 'lucide-react';

export default function Landing(): React.ReactElement {
  return (
      <div className="landing-page">
        <section id="inicio" className="hero-section hero-with-photo">
          <div className="hero-photo-backdrop">
            <img
                src="/images/hero-miners.jpg"
                alt="Especialistas y trabajadores mineros observando el tajo abierto en MINTRA"
                className="hero-bg-img"
            />
            <div className="hero-gradient-overlay"></div>
          </div>
          <div className="hero-container">
            <div className="hero-grid">
              <div className="hero-content">
                <div className="hero-pill-tag">
                  <Sparkles size={16} className="pill-icon" />
                  <span>PLATAFORMA ESPECIALIZADA EN EL SECTOR MINERO</span>
                </div>
                <h1 className="hero-title">
                  Conectamos empresas del sector minero con <span className="highlight-text">el talento que necesitan</span>
                </h1>
                <p className="hero-description">
                  La plataforma diseñada para conectar de forma directa a empresas mineras con talento calificado en Perú.
                  Gestiona desde <strong>convocatorias urgentes para paradas de planta</strong> de pocos días hasta <strong>puestos permanentes a tiempo completo</strong> en operaciones de tajo abierto, subterráneas y plantas concentradoras.
                </p>
                <div className="hero-slogan-box">
                  <span className="hero-handwritten">Talento que mueve la industria</span>
                  <span className="hero-subcaption">CONECTAMOS EL MEJOR TALENTO CON LAS EMPRESAS LÍDERES</span>
                </div>
                <div className="hero-badges-row">
                  <div className="badge-item">
                    <div className="badge-icon-box">
                      <Compass size={20} />
                    </div>
                    <div className="badge-meta">
                      <strong>MÁS OPORTUNIDADES</strong>
                      <span>Paradas de planta y contratos estables</span>
                    </div>
                  </div>
                  <div className="badge-item">
                    <div className="badge-icon-box">
                      <Users size={20} />
                    </div>
                    <div className="badge-meta">
                      <strong>PERSONAS QUE HACEN LA DIFERENCIA</strong>
                      <span>Técnicos, mecánicos e ingenieros</span>
                    </div>
                  </div>
                  <div className="badge-item">
                    <div className="badge-icon-box">
                      <Award size={20} />
                    </div>
                    <div className="badge-meta">
                      <strong>MINERÍA MÁS SOSTENIBLE</strong>
                      <span>Seguridad, homologación y formalidad</span>
                    </div>
                  </div>
                </div>
              </div>
              <div className="hero-card-col">
                <div className="commercial-access-card">
                  <div className="card-header-accent">
                    <span className="accent-tag">ACCESO CENTRALIZADO</span>
                    <h3>Bienvenido a MINTRA</h3>
                    <p>
                      Un solo punto de acceso seguro para empresas mineras y postulantes del sector.
                    </p>
                  </div>
                  <div className="unified-access-banner">
                    <Users size={20} className="banner-icon" />
                    <div>
                      <strong>Un solo acceso seguro</strong>
                      <p>Empresas y postulantes ingresan directamente con sus credenciales.</p>
                    </div>
                  </div>
                  <div className="card-quick-actions">
                    <Link to="/login" className="btn btn-primary w-full btn-block">
                      <span>Acceder a mi cuenta</span>
                      <ArrowRight size={18} />
                    </Link>
                  </div>
                  <div className="trust-indicators-list" id="beneficios">
                    <div className="trust-indicator-item">
                      <div className="trust-icon-wrapper shield">
                        <ShieldCheck size={20} />
                      </div>
                      <div className="trust-text">
                        <h4>Acceso seguro</h4>
                        <p>Tus datos personales y de empresa protegidos bajo cifrado.</p>
                      </div>
                    </div>
                    <div className="trust-indicator-item">
                      <div className="trust-icon-wrapper building">
                        <Building2 size={20} />
                      </div>
                      <div className="trust-text">
                        <h4>Empresas verificadas</h4>
                        <p>Conectamos talento calificado con operaciones formales y fiscalizadas.</p>
                      </div>
                    </div>
                    <div className="trust-indicator-item">
                      <div className="trust-icon-wrapper trend">
                        <TrendingUp size={20} />
                      </div>
                      <div className="trust-text">
                        <h4>Oportunidades reales</h4>
                        <p>Convocatorias vigentes con seguimiento directo de postulaciones.</p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>
        <section id="nosotros" className="about-section">
          <div className="container">
            <div className="section-header text-center">
              <span className="section-eyebrow">ACERCA DE LA PLATAFORMA</span>
              <h2 className="section-title">¿Qué es MINTRA?</h2>
              <p className="section-subtitle">
                MINTRA es una plataforma digital de reclutamiento especializada en el sector minero que facilita la publicación de convocatorias, la búsqueda de candidatos y la gestión de postulaciones desde un solo lugar.
              </p>
            </div>
            <div className="about-feature-row" style={{ marginBottom: '5rem' }}>
              <div className="about-feature-img-wrapper">
                <img
                    src="/images/contractors-field.jpg"
                    alt="Profesionales y técnicos en operaciones mineras"
                    className="about-feature-img"
                />
                <div className="about-img-caption">
                  <strong>Reclutamiento Especializado</strong>
                  <span>Conectando la oferta y demanda de la industria minera.</span>
                </div>
              </div>
              <div className="about-feature-text">
                <span className="pill-tag-inline">EL DESAFÍO OPERATIVO</span>
                <h3>¿Qué problema resolvemos?</h3>
                <p style={{ marginBottom: '1.5rem' }}>
                  Tradicionalmente, la selección de personal en la minería se enfrenta a cuellos de botella que ralentizan la cobertura de puestos clave:
                </p>
                <div className="challenge-mini-list">
                  <div className="mini-challenge" style={{ background: '#f8fafc', padding: '1rem 1.25rem', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)' }}>
                    <div className="mini-icon-circle" style={{ background: 'var(--primary-orange-light)', color: 'var(--primary-orange)' }}>01</div>
                    <div>
                      <strong>Dispersión de canales y postulaciones</strong>
                      <p>Recepción caótica de CVs por correos masivos y mensajes, dificultando el seguimiento.</p>
                    </div>
                  </div>
                  <div className="mini-challenge" style={{ background: '#f8fafc', padding: '1rem 1.25rem', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)' }}>
                    <div className="mini-icon-circle" style={{ background: 'var(--primary-orange-light)', color: 'var(--primary-orange)' }}>02</div>
                    <div>
                      <strong>Tiempos prolongados de convocatoria</strong>
                      <p>Falta de visibilidad directa para que los profesionales técnicos conozcan las vacantes.</p>
                    </div>
                  </div>
                  <div className="mini-challenge" style={{ background: '#f8fafc', padding: '1rem 1.25rem', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)' }}>
                    <div className="mini-icon-circle" style={{ background: 'var(--primary-orange-light)', color: 'var(--primary-orange)' }}>03</div>
                    <div>
                      <strong>Gestión fragmentada del talento</strong>
                      <p>Ausencia de una plataforma digital que centralice los requerimientos del sector minero.</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div style={{
              background: 'linear-gradient(135deg, #0f172a 0%, #1e293b 100%)',
              borderRadius: 'var(--radius-xl)',
              padding: '4rem 2.5rem',
              color: '#ffffff',
              marginBottom: '5rem',
              boxShadow: 'var(--shadow-xl)'
            }}>
              <div className="text-center" style={{ maxWidth: '700px', margin: '0 auto 3rem' }}>
                <span style={{ color: '#fb923c', fontSize: '0.82rem', fontWeight: '800', letterSpacing: '0.08em', textTransform: 'uppercase', display: 'block', marginBottom: '0.5rem' }}>
                  NUESTRO VALOR DIFERENCIAL
                </span>
                <h3 style={{ fontSize: '2.2rem', fontWeight: '800', letterSpacing: '-0.02em', marginBottom: '1rem', color: '#ffffff' }}>
                  Transformamos el reclutamiento en la minería
                </h3>
                <p style={{ color: '#cbd5e1', fontSize: '1.05rem', lineHeight: '1.6' }}>
                  Diseñamos una experiencia tecnológica ágil que simplifica la conexión laboral mediante tres pilares fundamentales:
                </p>
              </div>
              <div className="pillars-grid" style={{ gap: '1.5rem' }}>
                <div style={{
                  background: 'rgba(255, 255, 255, 0.05)',
                  border: '1px solid rgba(255, 255, 255, 0.1)',
                  borderRadius: 'var(--radius-lg)',
                  padding: '2rem',
                  backdropFilter: 'blur(8px)',
                  transition: 'transform 0.2s'
                }}>
                  <div style={{ fontSize: '2.2rem', fontWeight: '900', color: '#fb923c', marginBottom: '0.5rem', opacity: '0.9' }}>01</div>
                  <h4 style={{ fontSize: '1.2rem', fontWeight: '700', color: '#ffffff', marginBottom: '0.75rem' }}>Gestión Centralizada</h4>
                  <p style={{ fontSize: '0.92rem', color: '#94a3b8', lineHeight: '1.6' }}>
                    Adiós a los correos dispersos y hojas de cálculo desordenadas. Toda la información de ofertas y candidatos se administra desde un panel único.
                  </p>
                </div>
                <div style={{
                  background: 'rgba(255, 255, 255, 0.08)',
                  border: '1px solid rgba(251, 146, 60, 0.4)',
                  borderRadius: 'var(--radius-lg)',
                  padding: '2rem',
                  backdropFilter: 'blur(8px)',
                  boxShadow: '0 10px 25px -5px rgba(217, 93, 24, 0.2)'
                }}>
                  <div style={{ fontSize: '2.2rem', fontWeight: '900', color: '#fb923c', marginBottom: '0.5rem' }}>02</div>
                  <h4 style={{ fontSize: '1.2rem', fontWeight: '700', color: '#ffffff', marginBottom: '0.75rem' }}>Conexión Directa</h4>
                  <p style={{ fontSize: '0.92rem', color: '#e2e8f0', lineHeight: '1.6' }}>
                    Facilitamos que las empresas del sector encuentren perfiles calificados y que los postulantes accedan a vacantes de forma inmediata y transparente.
                  </p>
                </div>
                <div style={{
                  background: 'rgba(255, 255, 255, 0.05)',
                  border: '1px solid rgba(255, 255, 255, 0.1)',
                  borderRadius: 'var(--radius-lg)',
                  padding: '2rem',
                  backdropFilter: 'blur(8px)'
                }}>
                  <div style={{ fontSize: '2.2rem', fontWeight: '900', color: '#fb923c', marginBottom: '0.5rem', opacity: '0.9' }}>03</div>
                  <h4 style={{ fontSize: '1.2rem', fontWeight: '700', color: '#ffffff', marginBottom: '0.75rem' }}>Enfoque Sectorial</h4>
                  <p style={{ fontSize: '0.92rem', color: '#94a3b8', lineHeight: '1.6' }}>
                    Una herramienta diseñada exclusivamente para responder a las dinámicas y exigencias específicas del rubro minero en el país.
                  </p>
                </div>
              </div>
            </div>
          </div>
        </section>
        <section id="trabajos" className="modalities-section">
          <div className="container">
            <div className="section-header text-center">
              <span className="section-eyebrow">FLEXIBILIDAD PARA EL SECTOR MINERO</span>
              <h2 className="section-title">
                Modalidades de convocatoria en MINTRA
              </h2>
              <p className="section-subtitle">
                Ya sea que tu empresa necesite cubrir una parada de planta urgente de pocos días o contratar personal a tiempo completo por tiempo indefinido.
              </p>
            </div>
            <div className="modalities-grid-2col">
              <Link to="/convocatorias?tipo=PARADA_PLANTA" className="modality-card modality-card-enlace">
                <div className="modality-image-box">
                  <img
                      src="/images/plant-maintenance.jpg"
                      alt="Técnicos realizando mantenimiento en molino industrial en parada de planta"
                      className="modality-photo"
                  />
                  <span className="modality-badge urgent">
                  <Zap size={14} /> Convocatorias de 3 a 20 días
                </span>
                </div>
                <div className="modality-body">
                  <div className="modality-meta-header">
                    <span className="modality-type">MANTENIMIENTO INTENSIVO</span>
                    <span className="modality-tag">Corta Duración</span>
                  </div>
                  <h3>Paradas de Planta y Servicios Temporales</h3>
                  <p>
                    Campañas de alta demanda y rápida cobertura para mantenimiento mayor de molinos SAG, chancado, fajas transportadoras, hornos y montaje electromecánico.
                  </p>
                  <ul className="modality-perks">
                    <li>
                      <CheckCircle2 size={16} className="text-orange" />
                      <span>Pago por jornada especializada con bonificación de parada.</span>
                    </li>
                    <li>
                      <CheckCircle2 size={16} className="text-orange" />
                      <span>Perfiles clave: Soldadores 6G, mecánicos de molinos, electricistas e instrumentistas.</span>
                    </li>
                    <li>
                      <CheckCircle2 size={16} className="text-orange" />
                      <span>Convocatorias urgentes con contratación en menos de 48 horas.</span>
                    </li>
                  </ul>
                  <span className="modality-ver-ofertas">Ver convocatorias <ArrowRight size={16} /></span>
                </div>
              </Link>
              <Link to="/convocatorias?tipo=PLANILLA_COMPLETA" className="modality-card featured-modality modality-card-enlace">
                <div className="modality-image-box">
                  <img
                      src="/images/fulltime-career.jpg"
                      alt="Profesionales de la minería junto a camión gigante de acarreo"
                      className="modality-photo"
                  />
                  <span className="modality-badge stable">
                  <Award size={14} /> Planilla Directa & Estabilidad
                </span>
                </div>
                <div className="modality-body">
                  <div className="modality-meta-header">
                    <span className="modality-type">STAFF PERMANENTE</span>
                    <span className="modality-tag orange">Tiempo Completo</span>
                  </div>
                  <h3>Puestos Fijos y Desarrollo de Carrera</h3>
                  <p>
                    Plazas laborales indefinidas contratadas directamente por empresas mineras operadoras para la gestión continua en tajo abierto, interior mina o concentradora.
                  </p>
                  <ul className="modality-perks">
                    <li>
                      <CheckCircle2 size={16} className="text-orange" />
                      <span>Régimen atípico regular (14x7, 10x10, 4x3) con habitabilidad y alimentación.</span>
                    </li>
                    <li>
                      <CheckCircle2 size={16} className="text-orange" />
                      <span>Perfiles: Operadores de camión gigante (CAT/Komatsu), ingenieros de mina y geología.</span>
                    </li>
                    <li>
                      <CheckCircle2 size={16} className="text-orange" />
                      <span>Línea de ascenso profesional, EPS corporativa y utilidades mineras.</span>
                    </li>
                  </ul>
                  <span className="modality-ver-ofertas">Ver convocatorias <ArrowRight size={16} /></span>
                </div>
              </Link>
            </div>
          </div>
        </section>
        <section className="about-section">
          <div className="container">
            <div className="split-solution-container">
              <div className="solution-card company-card">
                <div className="solution-tag">PARA EMPRESAS MINERAS</div>
                <h3 className="solution-title">Gestiona tus convocatorias</h3>
                <p className="solution-desc">
                  Publica requerimientos de personal para el sector minero y administra tu proceso de selección de manera centralizada.
                </p>
                <ul className="solution-features">
                  <li>
                    <CheckCircle2 size={18} className="check-icon" />
                    <span>Publicación ágil de ofertas laborales y convocatorias vigentes.</span>
                  </li>
                  <li>
                    <CheckCircle2 size={18} className="check-icon" />
                    <span>Visualización y revisión de los perfiles de los postulantes inscritos.</span>
                  </li>
                  <li>
                    <CheckCircle2 size={18} className="check-icon" />
                    <span>Recepción y gestión ordenada de candidatos en tu panel de empresa.</span>
                  </li>
                  <li>
                    <CheckCircle2 size={18} className="check-icon" />
                    <span>Optimización de los tiempos de respuesta en el proceso de reclutamiento.</span>
                  </li>
                </ul>
              </div>
              <div className="solution-card candidate-card">
                <div className="solution-tag">PARA TÉCNICOS Y PROFESIONALES</div>
                <h3 className="solution-title">Encuentra nuevas oportunidades</h3>
                <p className="solution-desc">
                  Presenta tu perfil profesional y postula de forma sencilla a las diferentes oportunidades laborales disponibles en el sector.
                </p>
                <ul className="solution-features">
                  <li>
                    <CheckCircle2 size={18} className="check-icon" />
                    <span>Acceso a diversas convocatorias activas de la industria minera.</span>
                  </li>
                  <li>
                    <CheckCircle2 size={18} className="check-icon" />
                    <span>Creación y actualización de tu perfil profesional en la plataforma.</span>
                  </li>
                  <li>
                    <CheckCircle2 size={18} className="check-icon" />
                    <span>Seguimiento directo al estado de tus postulaciones enviadas.</span>
                  </li>
                  <li>
                    <CheckCircle2 size={18} className="check-icon" />
                    <span>Plataforma de postulación 100% accesible y transparente.</span>
                  </li>
                </ul>
              </div>
            </div>
            <div className="about-cta-banner" style={{ marginTop: '4rem' }}>
              <div className="cta-content">
                <h3>¿Listo para conectarte con las oportunidades del sector minero?</h3>
                <p>
                  Regístrate hoy en MINTRA. Encuentra al talento ideal para tu empresa o postula a las convocatorias que impulsarán tu carrera.
                </p>
                <div className="cta-buttons">
                  <Link to="/registro/empresa" className="btn btn-white-accent">
                    Comenzar como Empresa
                  </Link>
                  <Link to="/registro/postulante" className="btn btn-white-outline">
                    Comenzar como Postulante
                  </Link>
                  <Link to="/login" className="btn btn-translucent">
                    Ya tengo una cuenta
                  </Link>
                </div>
              </div>
            </div>
          </div>
        </section>
      </div>
  );
}