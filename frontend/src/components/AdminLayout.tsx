import { type ReactNode, useEffect, useRef, useState } from 'react'
import { Link, Outlet, useLocation, useNavigate  } from 'react-router-dom'
import { Bell, Building2, ChevronDown, House, LogOut, Search } from 'lucide-react'
import { useAuth } from '../auth/AuthContext'
import { listarEmpresasAdmin, fechaCorta, type EmpresaAdminItem, type EstadoEmpresa } from '../api/empresa.ts'
import './AdminLayout.css'
import { nombreIdentificadorFiscal } from '../utils/documentos.ts'

export interface PerfilAdmin {
    nombre: string
    correo: string
    rol: string
}

export interface AdminContext {
    perfil: PerfilAdmin | null
}

const ITEMS_MENU = [
    { path: '/admin', etiqueta: 'Inicio', icono: House },
    { path: '/admin/empresas', etiqueta: 'Empresas', icono: Building2 },
]

export default function AdminLayout(): ReactNode {
    const location = useLocation()
    const [menuAbierto, setMenuAbierto] = useState(false)
    const menuRef = useRef<HTMLDivElement>(null)
    const navigate = useNavigate()
    const [pendientes, setPendientes] = useState<EmpresaAdminItem[]>([])
    const [campanaAbierta, setCampanaAbierta] = useState(false)
    const campanaRef = useRef<HTMLDivElement>(null)
    const ESTADOS_NOTIFICABLES: EstadoEmpresa[] = ['PENDIENTE_DOCUMENTO', 'EN_REVISION']

    useEffect(() => {
        function cargar() {
            listarEmpresasAdmin()
                .then((lista) => setPendientes(lista.filter((e) => ESTADOS_NOTIFICABLES.includes(e.estado))))
                .catch(() => {})
        }
        cargar()
        const intervalo = setInterval(cargar, 30000)
        window.addEventListener('empresas-actualizadas', cargar)
        return () => {
            clearInterval(intervalo)
            window.removeEventListener('empresas-actualizadas', cargar)
        }
    }, [location.pathname])

    useEffect(() => {
        function manejarClicFuera(e: MouseEvent) {
            if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
                setMenuAbierto(false)
            }

            if (campanaRef.current && !campanaRef.current.contains(e.target as Node)) {
                setCampanaAbierta(false)
            }
        }
        document.addEventListener('mousedown', manejarClicFuera)
        return () => document.removeEventListener('mousedown', manejarClicFuera)
    }, [])

    const { cerrarSesion, estado } = useAuth()
    const perfil: PerfilAdmin | null = estado.tipo === 'autenticado'
        ? { nombre: estado.sesion.nombre, correo: estado.sesion.correo, rol: estado.sesion.rol }
        : null

    function handleLogout(): void {
        cerrarSesion('/login')
    }

    const contexto: AdminContext = { perfil }

    return (
        <div className="admin-layout">
            <aside className="admin-layout__sidebar">
                <div className="admin-layout__marca">
                    <span className="admin-layout__marca-mini">PANEL DE ADMINISTRACIÓN</span>
                    <h2>MINTRA</h2>
                    <p>Gestiona las empresas</p>
                </div>
                <nav className="admin-layout__nav">
                    {ITEMS_MENU.map(({ path, etiqueta, icono: Icono }) => (
                        <Link key={path} to={path} className={location.pathname === path ? 'activo' : ''}>
                            <Icono size={17} />
                            <span>{etiqueta}</span>
                        </Link>
                    ))}

                    <div className="admin-layout__divider" />
                    <Link to="/admin/ofertas" className={location.pathname === '/admin/ofertas' ? 'activo' : ''}>
                        <Search size={17} />
                        <span>Ofertas laborales</span>
                    </Link>
                    <div className="admin-layout__divider" />
                    <button type="button" className="admin-layout__logout" onClick={handleLogout}>
                        <LogOut size={17} />
                        <span>Cerrar sesión</span>
                    </button>
                </nav>
            </aside>
            <div className="admin-layout__contenido">
                <header className="admin-layout__header">
                    <div className="admin-layout__campana-wrapper" ref={campanaRef}>
                        <button
                            className="admin-layout__campana"
                            aria-label="Notificaciones"
                            onClick={() => setCampanaAbierta((abierta) => !abierta)}
                        >
                            <Bell size={20} />
                            {pendientes.length > 0 && (
                                <span className="admin-layout__campana-badge">
                                    {pendientes.length > 9 ? '9+' : pendientes.length}
                                </span>
                            )}
                        </button>
                        {campanaAbierta && (
                            <div className="admin-layout__notificaciones">
                                <div className="admin-layout__notificaciones-titulo">Notificaciones</div>
                                {pendientes.length === 0 ? (
                                    <p className="admin-layout__notificaciones-vacio">No hay solicitudes pendientes.</p>
                                ) : (
                                    pendientes.map((e) => (
                                        <button
                                            key={e.idEmpresa}
                                            className="admin-layout__notificacion"
                                            onClick={() => {
                                                setCampanaAbierta(false)
                                                navigate(`/admin/empresas?estado=${e.estado}`)
                                            }}
                                        >
                                            <strong>{e.razonSocial}</strong>
                                            <span>
                                                {e.estado === 'EN_REVISION'
                                                    ? 'Solicitud en revisión'
                                                    : 'Nueva empresa registrada · falta su documento'}
                                                {' · '}{nombreIdentificadorFiscal(e.pais)} {e.ruc}
                                                {e.fechaRegistro ? ` · ${fechaCorta(e.fechaRegistro)}` : ''}
                                                </span>
                                        </button>
                                    ))
                                )}
                            </div>
                        )}
                    </div>
                    {perfil && (
                        <div className="admin-layout__usuario-wrapper" ref={menuRef}>
                            <button
                                className="admin-layout__usuario"
                                onClick={() => setMenuAbierto((abierto) => !abierto)}
                            >
                                <span className="admin-layout__avatar">
                                    {perfil.nombre.charAt(0).toUpperCase()}
                                </span>
                                <span className="admin-layout__usuario-texto">
                                    {perfil.rol === 'ADMINISTRADOR' ? 'Administrador del sistema' : perfil.nombre}
                                </span>
                                <ChevronDown size={16} />
                            </button>
                            {menuAbierto && (
                                <div className="admin-layout__dropdown">
                                    <Link
                                        to="/recuperar-password"
                                        className="admin-layout__dropdown-item"
                                        onClick={() => setMenuAbierto(false)}
                                    >
                                        Cambiar contraseña
                                    </Link>
                                    <button
                                        className="admin-layout__dropdown-item admin-layout__dropdown-item--logout"
                                        onClick={handleLogout}
                                    >
                                        Cerrar sesión
                                    </button>
                                </div>
                            )}
                        </div>
                    )}
                </header>
                <main>
                    <Outlet context={contexto} />
                </main>
            </div>
        </div>
    )
}