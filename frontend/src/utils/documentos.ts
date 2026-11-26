export type TipoDocumento = 'DNI' | 'PASAPORTE' | 'CARNE_EXTRANJERIA' | 'CEDULA' | 'OTRO'

export const PAISES: { codigo: string; nombre: string }[] = [
  { codigo: 'PE', nombre: 'Perú' },
  { codigo: 'AR', nombre: 'Argentina' },
  { codigo: 'AU', nombre: 'Australia' },
  { codigo: 'BO', nombre: 'Bolivia' },
  { codigo: 'BR', nombre: 'Brasil' },
  { codigo: 'CA', nombre: 'Canadá' },
  { codigo: 'CL', nombre: 'Chile' },
  { codigo: 'CN', nombre: 'China' },
  { codigo: 'CO', nombre: 'Colombia' },
  { codigo: 'EC', nombre: 'Ecuador' },
  { codigo: 'ES', nombre: 'España' },
  { codigo: 'US', nombre: 'Estados Unidos' },
  { codigo: 'MX', nombre: 'México' },
  { codigo: 'PA', nombre: 'Panamá' },
  { codigo: 'PY', nombre: 'Paraguay' },
  { codigo: 'GB', nombre: 'Reino Unido' },
  { codigo: 'ZA', nombre: 'Sudáfrica' },
  { codigo: 'UY', nombre: 'Uruguay' },
  { codigo: 'VE', nombre: 'Venezuela' },
]

export const TIPOS_DOCUMENTO: { valor: TipoDocumento; nombre: string }[] = [
  { valor: 'DNI', nombre: 'DNI' },
  { valor: 'PASAPORTE', nombre: 'Pasaporte' },
  { valor: 'CARNE_EXTRANJERIA', nombre: 'Carné de extranjería' },
  { valor: 'CEDULA', nombre: 'Cédula de identidad' },
  { valor: 'OTRO', nombre: 'Otro documento' },
]

const IDENTIFICADOR_FISCAL: Record<string, string> = {
  PE: 'RUC',
  CL: 'RUT',
  CO: 'NIT',
  BO: 'NIT',
  AR: 'CUIT',
  EC: 'RUC',
  MX: 'RFC',
  BR: 'CNPJ',
  US: 'EIN',
  AU: 'ABN',
  CA: 'Business Number',
  ES: 'NIF',
}

export function nombrePais(codigo: string | null | undefined): string {
  return PAISES.find((p) => p.codigo === codigo)?.nombre ?? codigo ?? ''
}

export function nombreTipoDocumento(tipo: string | null | undefined): string {
  return TIPOS_DOCUMENTO.find((t) => t.valor === tipo)?.nombre ?? 'Documento'
}

export function nombreIdentificadorFiscal(pais: string | null | undefined): string {
  return (pais && IDENTIFICADOR_FISCAL[pais]) || 'Identificador fiscal'
}

export function normalizarDocumento(valor: string): string {
  return valor.toUpperCase().replace(/\s/g, '')
}

export function problemaDocumento(pais: string, tipo: string, numero: string): string | undefined {
  const limpio = normalizarDocumento(numero)
  if (!tipo) return 'Selecciona el tipo de documento.'
  if (pais === 'PE' && tipo === 'DNI') return /^\d{8}$/.test(limpio) ? undefined : 'El DNI peruano debe tener 8 dígitos.'
  return /^[A-Z0-9-]{5,20}$/.test(limpio) ? undefined : 'El número debe tener entre 5 y 20 letras o números, sin puntos.'
}

export function problemaIdentificadorFiscal(pais: string, numero: string): string | undefined {
  const limpio = normalizarDocumento(numero)
  if (pais === 'PE') return /^20\d{9}$/.test(limpio) ? undefined : 'El RUC de la empresa debe empezar con 20 y tener 11 dígitos.'
  return /^[A-Z0-9-]{5,20}$/.test(limpio) ? undefined : 'Debe tener entre 5 y 20 letras o números, sin puntos.'
}

export function documentoLegible(pais: string | null | undefined, tipo: string | null | undefined, numero: string): string {
  return `${nombreTipoDocumento(tipo)} ${numero}${pais && pais !== 'PE' ? ` (${nombrePais(pais)})` : ''}`
}
