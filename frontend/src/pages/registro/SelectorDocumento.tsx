import type { ChangeEvent } from 'react'
import { PAISES, TIPOS_DOCUMENTO } from '../../utils/documentos.ts'

interface Props {
  prefijo: string
  pais: string
  tipo: string
  numero: string
  errores: { pais?: string; tipo?: string; numero?: string }
  alCambiar: (campo: 'pais' | 'tipo' | 'numero', valor: string) => void
}

const SELECT = { width: '100%', padding: '0.65rem', borderRadius: '0.375rem', border: '1px solid #d1d5db' }

export default function SelectorDocumento({ prefijo, pais, tipo, numero, errores, alCambiar }: Props) {
  const cambio = (campo: 'pais' | 'tipo' | 'numero') =>
      (e: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => alCambiar(campo, e.target.value)
  const id = (sufijo: string) => `${prefijo}${sufijo}`
  return (
      <div className="form-grid-2col" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '1rem' }}>
        <div className="campo">
          <label htmlFor={id('Pais')}>País del documento</label>
          <select id={id('Pais')} value={pais} onChange={cambio('pais')} style={SELECT}
                  aria-invalid={errores.pais ? true : undefined}>
            {PAISES.map((p) => <option key={p.codigo} value={p.codigo}>{p.nombre}</option>)}
          </select>
          {errores.pais && <p className="field-error">{errores.pais}</p>}
        </div>
        <div className="campo">
          <label htmlFor={id('Tipo')}>Tipo de documento</label>
          <select id={id('Tipo')} value={tipo} onChange={cambio('tipo')} style={SELECT}
                  aria-invalid={errores.tipo ? true : undefined}>
            {TIPOS_DOCUMENTO.map((t) => <option key={t.valor} value={t.valor}>{t.nombre}</option>)}
          </select>
          {errores.tipo && <p className="field-error">{errores.tipo}</p>}
        </div>
        <div className="campo">
          <label htmlFor={id('Numero')}>Número de documento</label>
          <input id={id('Numero')} value={numero} onChange={cambio('numero')} maxLength={20} autoComplete="off"
                 aria-invalid={errores.numero ? true : undefined}
                 aria-describedby={errores.numero ? `${id('Numero')}-error` : undefined} />
          {errores.numero && <p id={`${id('Numero')}-error`} className="field-error">{errores.numero}</p>}
        </div>
      </div>
  )
}
