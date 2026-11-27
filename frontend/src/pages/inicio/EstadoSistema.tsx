import axios from 'axios'
import { useState } from 'react'
import { API_URL } from '../../api/client.ts'
import { obtenerSalud, type HealthData } from '../../api/health.ts'
import type { ApiResponse } from '../../api/types.ts'

type Estado =
  | { fase: 'inicial' }
  | { fase: 'cargando' }
  | { fase: 'listo'; backend: boolean; data?: HealthData; mensaje?: string }

export default function EstadoSistema() {
  const [estado, setEstado] = useState<Estado>({ fase: 'inicial' })

  async function probar() {
    setEstado({ fase: 'cargando' })
    try {
      const data = await obtenerSalud()
      setEstado({ fase: 'listo', backend: true, data })
    } catch (error) {
      if (axios.isAxiosError<ApiResponse<HealthData>>(error) && error.response) {
        setEstado({
          fase: 'listo',
          backend: true,
          data: error.response.data?.data ?? undefined,
          mensaje: error.response.data?.message,
        })
      } else {
        setEstado({
          fase: 'listo',
          backend: false,
          mensaje: `No hay respuesta de ${API_URL}. Revisa que el backend esté corriendo y que VITE_API_URL sea correcta.`,
        })
      }
    }
  }

  const listo = estado.fase === 'listo' ? estado : null
  const dbOk = listo?.data?.baseDeDatos === 'UP'

  return (
    <section>
      <h1>Estado del sistema</h1>
      <p className="muted">
        Comprueba que <strong>React → Spring Boot → Supabase</strong> están conectados.
      </p>
      <button type="button" className="btn btn-primary" onClick={probar} disabled={estado.fase === 'cargando'}>
        {estado.fase === 'cargando' ? 'Probando…' : 'Probar conexión'}
      </button>
      <ul className="checks">
        <li className="ok">
          <strong>Frontend (React)</strong>
          <span>Funcionando. API configurada en {API_URL}</span>
        </li>
        <li className={!listo ? 'pending' : listo.backend ? 'ok' : 'fail'}>
          <strong>Backend (Spring Boot)</strong>
          <span>{!listo ? 'Sin probar' : listo.backend ? 'Responde' : 'No responde'}</span>
        </li>
        <li className={!listo || !listo.backend ? 'pending' : dbOk ? 'ok' : 'fail'}>
          <strong>Base de datos (Supabase)</strong>
          <span>
            {!listo || !listo.backend
              ? 'Sin probar'
              : dbOk
                ? `Conectada · ${listo.data?.tablas?.length ?? 0} tablas en el esquema público`
                : 'Sin conexión'}
          </span>
        </li>
      </ul>
      {listo?.mensaje && !dbOk && <p className="error">{listo.mensaje}</p>}
      {listo?.data?.error && <pre className="error">{listo.data.error}</pre>}
      {dbOk && listo?.data?.tablas && <pre>{listo.data.tablas.join('\n')}</pre>}
    </section>
  )
}
