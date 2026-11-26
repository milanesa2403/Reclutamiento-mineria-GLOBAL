import Cargando from '../../components/Cargando';
import { useEffect, useState } from 'react'
import { Link, useOutletContext } from 'react-router-dom'
import type { AdminContext } from '../../components/AdminLayout'
import { textoError } from '../../api/client.ts'
import { listarEmpresasAdmin, type EmpresaAdminItem } from '../../api/empresa.ts'
import { Briefcase, Building2, ChevronRight } from 'lucide-react'
import './AdminHome.css'

function saludoSegunHora(fecha: Date = new Date()): string {
    const hora = fecha.getHours()
    if (hora >= 5 && hora < 12) return 'Buenos días'
    if (hora >= 12 && hora < 18) return 'Buenas tardes'
    return 'Buenas noches'
}

export default function AdminHome() {
    const { perfil } = useOutletContext<AdminContext>()
    const [empresas, setEmpresas] = useState<EmpresaAdminItem[] | null>(null)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        listarEmpresasAdmin()
            .then(setEmpresas)
            .catch((err) => setError(textoError(err)))
    }, [])

    if (!perfil) return <Cargando tipo="montana" mensaje="Cargando el panel" />

    const porRevisar = empresas?.filter((e) => e.estado === 'EN_REVISION').length ?? 0
    const aprobadas = empresas?.filter((e) => e.estado === 'APROBADA').length ?? 0

    return (
        <div className="admin-home">
            <span className="admin-home__mini">PANEL DE ADMINISTRACIÓN</span>
            <h1>{saludoSegunHora()}</h1>
            <p className="admin-home__subtitulo">Este es el resumen de MINTRA.</p>
            {error && <p className="admin-home__error">{error}</p>}
            {empresas && (
                <>
                    <h2 className="admin-home__seccion">Resumen</h2>
                    <section className="admin-home__resumen">
                        <Link to="/admin/empresas" className="admin-home__tarjeta">
                            <span className="admin-home__icono"><Building2 size={21} /></span>
                            <span className="admin-home__etiqueta">Empresas</span>
                            <strong>{empresas.length}</strong>
                            <small>{aprobadas} {aprobadas === 1 ? 'aprobada' : 'aprobadas'}</small>
                        </Link>
                        <Link to="/admin/ofertas" className="admin-home__tarjeta">
                            <span className="admin-home__icono"><Briefcase size={21} /></span>
                            <span className="admin-home__etiqueta">Convocatorias</span>
                            <strong>Ver</strong>
                            <small>Ofertas publicadas</small>
                        </Link>
                    </section>
                    <h2 className="admin-home__seccion">Pendientes de atención</h2>
                    <section className="admin-home__pendientes">
                        {porRevisar > 0 ? (
                            <Link to="/admin/empresas?estado=EN_REVISION" className="admin-home__pendiente">
                                <div>
                                    <strong>
                                        {porRevisar} {porRevisar === 1 ? 'empresa espera validación' : 'empresas esperan validación'}
                                    </strong>
                                    <p>Requieren revisión antes de poder usar las funciones de empresa.</p>
                                </div>
                                <ChevronRight size={20} aria-hidden="true" />
                            </Link>
                        ) : (
                            <p className="admin-home__vacio">Todo al día. No hay nada pendiente.</p>
                        )}
                    </section>
                </>
            )}
        </div>
    )
}
