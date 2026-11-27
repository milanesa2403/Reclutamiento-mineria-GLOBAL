export type Rol = 'ADMINISTRADOR' | 'EMPRESA' | 'POSTULANTE'

export const RUTA_COMPLETAR_PERFIL = '/postulante/completar-perfil'

export const RUTA_SOLICITUD_EMPRESA = '/empresa/solicitud'

export const RUTA_COMPLETAR_REGISTRO = '/registro/completar'

export function prefijoDe(rol: Rol): string {
  return rol === 'ADMINISTRADOR' ? '/admin' : rol === 'EMPRESA' ? '/empresa' : '/postulante'
}

export function homeFor(rol: Rol): string {
  switch (rol) {
    case 'ADMINISTRADOR':
      return '/admin'
    case 'EMPRESA':
      return '/empresa'
    case 'POSTULANTE':
      return '/postulante'
    default:
      return '/login'
  }
}
