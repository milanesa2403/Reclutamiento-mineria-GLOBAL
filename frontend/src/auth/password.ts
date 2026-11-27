export const REGLA_PASSWORD =
    'Mínimo 12 caracteres, con al menos una minúscula, una mayúscula, un número y un símbolo.'

export function problemaPassword(password: string): string | null {
  const cumple = password.length >= 12 && password.length <= 256
      && /[a-z]/.test(password) && /[A-Z]/.test(password) && /[0-9]/.test(password) && /[^A-Za-z0-9]/.test(password)
  return cumple ? null : `La contraseña no es válida. ${REGLA_PASSWORD}`
}
