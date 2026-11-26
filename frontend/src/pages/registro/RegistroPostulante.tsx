import { useState, type ChangeEvent, type FormEvent, type InputHTMLAttributes } from 'react'
import { textoError, erroresPorCampo } from '../../api/client.ts'
import { registrarPostulante } from '../../api/postulante.ts'
import { normalizarDocumento, problemaDocumento, type TipoDocumento } from '../../utils/documentos.ts'
import SelectorDocumento from './SelectorDocumento.tsx'
import './RegistroPostulante.css'

type NombreCampo = 'pais' | 'tipoDocumento' | 'numeroDocumento' | 'nombres' | 'apellidos' | 'telefono'
type Valores = Record<NombreCampo, string>
type Errores = Partial<Record<NombreCampo, string>>

const ORDEN: NombreCampo[] = ['pais', 'tipoDocumento', 'numeroDocumento', 'nombres', 'apellidos', 'telefono']

const ID_CAMPO: Partial<Record<NombreCampo, string>> = {
  pais: 'documentoPais',
  tipoDocumento: 'documentoTipo',
  numeroDocumento: 'documentoNumero',
}

function validar(v: Valores): Errores {
  const errores: Errores = {}
  const problema = problemaDocumento(v.pais, v.tipoDocumento, v.numeroDocumento)
  if (problema) errores.numeroDocumento = problema
  if (!v.nombres.trim()) errores.nombres = 'Ingresa tus nombres.'
  if (!v.apellidos.trim()) errores.apellidos = 'Ingresa tus apellidos.'
  if (!/^[+\d][\d\s-]{6,19}$/.test(v.telefono)) errores.telefono = 'Ingresa un teléfono válido, por ejemplo +51 987 654 321.'
  return errores
}

interface CampoProps extends InputHTMLAttributes<HTMLInputElement> {
  id: NombreCampo
  etiqueta: string
  error?: string
}

function Campo({ id, etiqueta, error, ...input }: CampoProps) {
  return (
      <div className="campo">
        <label htmlFor={id}>{etiqueta}</label>
        <input id={id} name={id} aria-invalid={error ? true : undefined}
               aria-describedby={error ? `${id}-error` : undefined} {...input} />
        {error && <p id={`${id}-error`} className="field-error">{error}</p>}
      </div>
  )
}

export default function RegistroPostulante({ alRegistrar }: { alRegistrar: () => Promise<void> }) {
  const [valores, setValores] = useState<Valores>({ pais: 'PE', tipoDocumento: 'DNI', numeroDocumento: '', nombres: '', apellidos: '', telefono: '' })
  const [errores, setErrores] = useState<Errores>({})
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  const cambiar = (campo: NombreCampo) => (e: ChangeEvent<HTMLInputElement>) => {
    let texto = e.target.value
    if (campo === 'telefono') texto = texto.replace(/[^+\d\s]/g, '').slice(0, 20)
    setValores((actuales) => ({ ...actuales, [campo]: texto }))
    if (errores[campo]) setErrores((actuales) => ({ ...actuales, [campo]: undefined }))
  }

  async function enviar(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    setErrorGeneral(null)
    const datos: Valores = {
      pais: valores.pais,
      tipoDocumento: valores.tipoDocumento,
      numeroDocumento: normalizarDocumento(valores.numeroDocumento),
      nombres: valores.nombres.trim(),
      apellidos: valores.apellidos.trim(),
      telefono: valores.telefono.trim(),
    }
    const locales = validar(datos)
    setErrores(locales)
    const primero = ORDEN.find((c) => locales[c])
    if (primero) {
      document.getElementById(ID_CAMPO[primero] ?? primero)?.focus()
      return
    }
    setEnviando(true)
    try {
      await registrarPostulante({ ...datos, tipoDocumento: datos.tipoDocumento as TipoDocumento })
      await alRegistrar()
    } catch (error) {
      const porCampo = erroresPorCampo(error)
      const mapeados: Errores = {}
      for (const campo of ORDEN) if (porCampo[campo]) mapeados[campo] = porCampo[campo]
      if (Object.keys(mapeados).length > 0) setErrores(mapeados)
      else setErrorGeneral(textoError(error))
    } finally {
      setEnviando(false)
    }
  }

  return (
      <>
        {errorGeneral && <p className="form-error" role="alert">{errorGeneral}</p>}
        <form onSubmit={enviar} className="custom-registration-form" noValidate>
          <SelectorDocumento prefijo="documento" pais={valores.pais} tipo={valores.tipoDocumento}
                             numero={valores.numeroDocumento}
                             errores={{ pais: errores.pais, tipo: errores.tipoDocumento, numero: errores.numeroDocumento }}
                             alCambiar={(campo, valor) => {
                               const nombre: NombreCampo = campo === 'pais' ? 'pais' : campo === 'tipo' ? 'tipoDocumento' : 'numeroDocumento'
                               setValores((actuales) => ({ ...actuales, [nombre]: valor }))
                               setErrores((actuales) => ({ ...actuales, [nombre]: undefined }))
                             }} />
          <Campo id="nombres" etiqueta="Nombres" autoComplete="given-name" maxLength={100}
                 value={valores.nombres} onChange={cambiar('nombres')} error={errores.nombres} />
          <Campo id="apellidos" etiqueta="Apellidos" autoComplete="family-name" maxLength={100}
                 value={valores.apellidos} onChange={cambiar('apellidos')} error={errores.apellidos} />
          <Campo id="telefono" etiqueta="Teléfono" type="tel" autoComplete="tel" maxLength={20}
                 value={valores.telefono} onChange={cambiar('telefono')} error={errores.telefono} />
          <button type="submit" className="btn btn-primary" disabled={enviando}>
            {enviando ? 'Guardando…' : 'Guardar mis datos'}
          </button>
        </form>
      </>
  )
}
