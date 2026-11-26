const SEPARADOR = ','

type Celda = string | number | null | undefined

function escapar(valor: Celda): string {
    let texto = valor === null || valor === undefined ? '' : String(valor)
    if (/^[=@\t\r]|^[+-](?!\d)/.test(texto)) texto = `'${texto}`
    return /[",\r\n]/.test(texto) || texto.includes(SEPARADOR) ? `"${texto.replace(/"/g, '""')}"` : texto
}

export function descargarCsv(nombreArchivo: string, columnas: string[], filas: Celda[][]): void {
    const contenido = [columnas, ...filas].map((fila) => fila.map(escapar).join(SEPARADOR)).join('\r\n')
    const blob = new Blob(['\uFEFF' + contenido], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const enlace = document.createElement('a')
    enlace.href = url
    enlace.download = nombreArchivo
    document.body.appendChild(enlace)
    enlace.click()
    document.body.removeChild(enlace)
    URL.revokeObjectURL(url)
}