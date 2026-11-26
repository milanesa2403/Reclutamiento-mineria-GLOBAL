import { useEffect, useState, type ChangeEvent, type FormEvent, type InputHTMLAttributes } from 'react'
import { api, textoError, erroresPorCampo, TIMEOUT_LARGO_MS } from '../../api/client.ts'
import { nombreIdentificadorFiscal, normalizarDocumento, PAISES, problemaDocumento, problemaIdentificadorFiscal } from '../../utils/documentos.ts'
import SelectorDocumento from './SelectorDocumento.tsx'
import './RegistroEmpresa.css'

interface SectorMinero {
  id: number
  nombre: string
}

type NombreCampo =
    | 'pais'
    | 'identificadorFiscal'
    | 'razonSocial'
    | 'direccion'
    | 'telefono'
    | 'idSectorMinero'
    | 'paisRepresentante'
    | 'tipoDocumentoRepresentante'
    | 'numeroDocumentoRepresentante'
    | 'nombresRepresentante'
    | 'apellidosRepresentante'
    | 'telefonoRepresentante'
    | 'direccionRepresentante'
    | 'cargoRepresentante'

type Valores = Record<NombreCampo, string>
type Errores = Partial<Record<NombreCampo, string>>

const ORDEN: NombreCampo[] = [
  'pais',
  'identificadorFiscal',
  'razonSocial',
  'direccion',
  'telefono',
  'idSectorMinero',
  'paisRepresentante',
  'tipoDocumentoRepresentante',
  'numeroDocumentoRepresentante',
  'nombresRepresentante',
  'apellidosRepresentante',
  'telefonoRepresentante',
  'direccionRepresentante',
  'cargoRepresentante',
]

const VALORES_INICIALES = {
  ...Object.fromEntries(ORDEN.map((c) => [c, ''])),
  pais: 'PE',
  paisRepresentante: 'PE',
  tipoDocumentoRepresentante: 'DNI',
} as Valores

const ID_CAMPO: Partial<Record<NombreCampo, string>> = {
  paisRepresentante: 'representantePais',
  tipoDocumentoRepresentante: 'representanteTipo',
  numeroDocumentoRepresentante: 'representanteNumero',
}

const TITULO = { fontSize: '1.1rem', fontWeight: 600, marginTop: '1rem', marginBottom: '1rem', color: '#1f2937', borderBottom: '1px solid #e5e7eb', paddingBottom: '0.4rem' }
const DOS_COLUMNAS = { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }

function validar(v: Valores): Errores {
  const errores: Errores = {}
  const fiscal = problemaIdentificadorFiscal(v.pais, v.identificadorFiscal)
  if (fiscal) errores.identificadorFiscal = fiscal
  if (!v.razonSocial) errores.razonSocial = 'La razón social es obligatoria.'
  if (!v.idSectorMinero) errores.idSectorMinero = 'Debe seleccionar un sector minero.'
  const documento = problemaDocumento(v.paisRepresentante, v.tipoDocumentoRepresentante, v.numeroDocumentoRepresentante)
  if (documento) errores.numeroDocumentoRepresentante = documento
  if (!v.nombresRepresentante) errores.nombresRepresentante = 'Los nombres del representante son obligatorios.'
  if (!v.apellidosRepresentante) errores.apellidosRepresentante = 'Los apellidos del representante son obligatorios.'
  if (!v.telefonoRepresentante) errores.telefonoRepresentante = 'El teléfono del representante es obligatorio.'
  if (!v.cargoRepresentante) errores.cargoRepresentante = 'El cargo del representante es obligatorio.'
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

export default function RegistroEmpresa({ alRegistrar }: { alRegistrar: () => Promise<void> }) {
  const [valores, setValores] = useState<Valores>(VALORES_INICIALES)
  const [errores, setErrores] = useState<Errores>({})
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null)
  const [sectores, setSectores] = useState<SectorMinero[]>([])
  const [cargandoSectores, setCargandoSectores] = useState(true)
  const [enviando, setEnviando] = useState(false)

  useEffect(() => {
    api.get<{ data?: SectorMinero[] } | SectorMinero[]>('/api/public/sectores-mineros')
        .then((res) => {
          const lista = Array.isArray(res.data) ? res.data : res.data?.data
          setSectores(Array.isArray(lista) ? lista : [])
        })
        .catch(() => setSectores([]))
        .finally(() => setCargandoSectores(false))
  }, [])

  const cambiar = (campo: NombreCampo) => (e: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    let texto = e.target.value
    setValores((actuales) => ({ ...actuales, [campo]: texto }))
    if (errores[campo]) setErrores((actuales) => ({ ...actuales, [campo]: undefined }))
  }

  async function enviar(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    setErrorGeneral(null)
    const datos = Object.fromEntries(ORDEN.map((c) => [c, valores[c].trim()])) as Valores
    datos.identificadorFiscal = normalizarDocumento(datos.identificadorFiscal)
    datos.numeroDocumentoRepresentante = normalizarDocumento(datos.numeroDocumentoRepresentante)
    const locales = validar(datos)
    setErrores(locales)
    const primero = ORDEN.find((c) => locales[c])
    if (primero) {
      document.getElementById(ID_CAMPO[primero] ?? primero)?.focus()
      return
    }
    setEnviando(true)
    try {
      await api.post('/api/auth/register/empresa', { ...datos, idSectorMinero: Number(datos.idSectorMinero) },
          { timeout: TIMEOUT_LARGO_MS })
      await alRegistrar()
    } catch (error) {
      const respuesta = (error as { response?: { data?: Record<string, unknown> } }).response?.data ?? {}
      const porCampo: Record<string, unknown> = { ...respuesta, ...erroresPorCampo(error) }
      const mapeados: Errores = {}
      for (const campo of ORDEN) if (typeof porCampo[campo] === 'string') mapeados[campo] = porCampo[campo] as string
      if (Object.keys(mapeados).length > 0) {
        setErrores(mapeados)
        const campo = ORDEN.find((c) => mapeados[c])
        if (campo) document.getElementById(ID_CAMPO[campo] ?? campo)?.focus()
      } else {
        setErrorGeneral(typeof respuesta.correo === 'string' ? respuesta.correo : textoError(error))
      }
    } finally {
      setEnviando(false)
    }
  }

  return (
      <>
        {errorGeneral && <p className="form-error" role="alert">{errorGeneral}</p>}
        <form onSubmit={enviar} className="custom-registration-form" noValidate>
          <h3 style={TITULO}>1. Datos de la empresa</h3>
          <div className="form-grid-2col" style={DOS_COLUMNAS}>
            <div className="campo">
              <label htmlFor="pais">País de la empresa</label>
              <select id="pais" value={valores.pais} onChange={cambiar('pais')}
                      style={{ width: '100%', padding: '0.65rem', borderRadius: '0.375rem', border: '1px solid #d1d5db' }}>
                {PAISES.map((p) => <option key={p.codigo} value={p.codigo}>{p.nombre}</option>)}
              </select>
              {errores.pais && <p className="field-error">{errores.pais}</p>}
            </div>
            <Campo id="identificadorFiscal" etiqueta={nombreIdentificadorFiscal(valores.pais)} maxLength={20}
                   placeholder={valores.pais === 'PE' ? '20XXXXXXXXX' : 'Sin puntos'}
                   value={valores.identificadorFiscal} onChange={cambiar('identificadorFiscal')}
                   error={errores.identificadorFiscal} />
          </div>
          <Campo id="razonSocial" etiqueta="Razón social" maxLength={200}
                 value={valores.razonSocial} onChange={cambiar('razonSocial')} error={errores.razonSocial} />
          <Campo id="direccion" etiqueta="Dirección fiscal" maxLength={255}
                 value={valores.direccion} onChange={cambiar('direccion')} error={errores.direccion} />
          <div className="form-grid-2col" style={DOS_COLUMNAS}>
            <Campo id="telefono" etiqueta="Teléfono empresa" type="tel" maxLength={20}
                   value={valores.telefono} onChange={cambiar('telefono')} error={errores.telefono} />
            <div className="campo">
              <label htmlFor="idSectorMinero">Sector minero</label>
              <select id="idSectorMinero" name="idSectorMinero" value={valores.idSectorMinero}
                      onChange={cambiar('idSectorMinero')} aria-invalid={errores.idSectorMinero ? true : undefined}
                      style={{ width: '100%', padding: '0.65rem', borderRadius: '0.375rem', border: '1px solid #d1d5db' }}>
                <option value="">{cargandoSectores ? 'Cargando…' : 'Seleccione un sector'}</option>
                {sectores.map((sec) => <option key={sec.id} value={sec.id}>{sec.nombre}</option>)}
              </select>
              {errores.idSectorMinero && <p className="field-error">{errores.idSectorMinero}</p>}
            </div>
          </div>
          <h3 style={TITULO}>2. Representante legal</h3>
          <SelectorDocumento prefijo="representante" pais={valores.paisRepresentante}
                             tipo={valores.tipoDocumentoRepresentante} numero={valores.numeroDocumentoRepresentante}
                             errores={{
                               pais: errores.paisRepresentante,
                               tipo: errores.tipoDocumentoRepresentante,
                               numero: errores.numeroDocumentoRepresentante,
                             }}
                             alCambiar={(campo, valor) => {
                               const nombre: NombreCampo = campo === 'pais' ? 'paisRepresentante'
                                   : campo === 'tipo' ? 'tipoDocumentoRepresentante' : 'numeroDocumentoRepresentante'
                               setValores((actuales) => ({ ...actuales, [nombre]: valor }))
                               setErrores((actuales) => ({ ...actuales, [nombre]: undefined }))
                             }} />
          <div className="form-grid-2col" style={DOS_COLUMNAS}>
            <Campo id="nombresRepresentante" etiqueta="Nombres" maxLength={100}
                   value={valores.nombresRepresentante} onChange={cambiar('nombresRepresentante')}
                   error={errores.nombresRepresentante} />
            <Campo id="apellidosRepresentante" etiqueta="Apellidos" maxLength={100}
                   value={valores.apellidosRepresentante} onChange={cambiar('apellidosRepresentante')}
                   error={errores.apellidosRepresentante} />
          </div>
          <div className="form-grid-2col" style={DOS_COLUMNAS}>
            <Campo id="telefonoRepresentante" etiqueta="Teléfono" type="tel" maxLength={20}
                   value={valores.telefonoRepresentante} onChange={cambiar('telefonoRepresentante')}
                   error={errores.telefonoRepresentante} />
            <Campo id="cargoRepresentante" etiqueta="Cargo" maxLength={100} placeholder="Ej. Gerente General"
                   value={valores.cargoRepresentante} onChange={cambiar('cargoRepresentante')}
                   error={errores.cargoRepresentante} />
          </div>
          <Campo id="direccionRepresentante" etiqueta="Dirección personal (opcional)" maxLength={255}
                 value={valores.direccionRepresentante} onChange={cambiar('direccionRepresentante')}
                 error={errores.direccionRepresentante} />
          <button type="submit" className="btn btn-primary" disabled={enviando} style={{ marginTop: '1.5rem', width: '100%' }}>
            {enviando ? 'Guardando…' : 'Registrar empresa'}
          </button>
        </form>
      </>
  )
}
