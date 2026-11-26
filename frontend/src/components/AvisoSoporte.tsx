import { Mail } from 'lucide-react'
import { CORREO_SOPORTE } from '../soporte'
import './AvisoSoporte.css'

export default function AvisoSoporte() {
    if (!CORREO_SOPORTE) return null
    return (
        <p className="aviso-soporte">
            <Mail size={16} />
            <span>
                ¿Otro problema con una empresa? Escríbenos a{' '}
                <a href={`mailto:${CORREO_SOPORTE}`}>{CORREO_SOPORTE}</a>{' '}
                y adjunta tus pruebas (capturas, mensajes o documentos).
            </span>
        </p>
    )
}
