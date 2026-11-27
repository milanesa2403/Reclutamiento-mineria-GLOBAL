import type { InputHTMLAttributes, ReactNode, TextareaHTMLAttributes } from 'react'

interface Comunes {
    id: string
    etiqueta: string
    ayuda?: string
    error?: string
    soloNumeros?: boolean
}

function describedBy({ id, ayuda, error }: Comunes): string | undefined {
    const ids = [ayuda ? `${id}-ayuda` : null, error ? `${id}-error` : null].filter(Boolean).join(' ')
    return ids || undefined
}

function Envoltura({ id, etiqueta, ayuda, error, children }: Comunes & { children: ReactNode }) {
    return (
        <div className="campo">
            <label htmlFor={id}>{etiqueta}</label>
            {children}
            {ayuda && (
                <p id={`${id}-ayuda`} className="ayuda">
                    {ayuda}
                </p>
            )}
            {error && (
                <p id={`${id}-error`} className="field-error">
                    {error}
                </p>
            )}
        </div>
    )
}

export function Campo({
                          id,
                          etiqueta,
                          ayuda,
                          error,
                          soloNumeros,
                          onKeyPress,
                          ...input
                      }: Comunes & Omit<InputHTMLAttributes<HTMLInputElement>, 'id'>) {
    return (
        <Envoltura id={id} etiqueta={etiqueta} ayuda={ayuda} error={error}>
            <input
                id={id}
                name={id}
                aria-invalid={error ? true : undefined}
                aria-describedby={describedBy({ id, etiqueta, ayuda, error })}
                onKeyPress={(e) => {
                    if (soloNumeros && !/[0-9]/.test(e.key)) {
                        e.preventDefault()
                    }
                    if (onKeyPress) {
                        onKeyPress(e)
                    }
                }}
                {...input}
            />
        </Envoltura>
    )
}

export function CampoTexto({
                               id,
                               etiqueta,
                               ayuda,
                               error,
                               ...textarea
                           }: Comunes & Omit<TextareaHTMLAttributes<HTMLTextAreaElement>, 'id'>) {
    return (
        <Envoltura id={id} etiqueta={etiqueta} ayuda={ayuda} error={error}>
            <textarea
                id={id}
                name={id}
                rows={4}
                aria-invalid={error ? true : undefined}
                aria-describedby={describedBy({ id, etiqueta, ayuda, error })}
                {...textarea}
            />
        </Envoltura>
    )
}