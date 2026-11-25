# Frontend - Reclutamiento Minero Global (avance)

React 18 + Vite. Roles soportados: **trabajador**, **empresa** y **administrador** (mismo login, vista segun el rol del token).

## Ejecutar
```bash
cd frontend
npm install
npm run dev        # http://localhost:5173  (requiere el backend en http://localhost:8080)
npm run build      # genera dist/ (se sirve con S3 + CloudFront)
```
Vite redirige `/v1` y `/api` al backend, por lo que no hace falta configurar CORS en desarrollo.

## Pantallas
| Rol | Que permite |
|---|---|
| Todos | Registro con consentimiento (Ley 29733 / RGPD) e inicio de sesion |
| Trabajador | Subir CV, buscar ofertas por especialidad y pais, postular (con `Idempotency-Key`), ver postulaciones |
| Empresa | Publicar/cerrar ofertas, ver postulantes y cambiar su estado |
| Administrador | Ver auditoria, activar modo solo lectura, ver cola/DLQ y reprocesarla |

## Pendiente
Descarga de CV desde la empresa (requiere fetch con token y `Blob`), importacion/exportacion CSV-JSON, chat de entrevista, selector de idioma (i18n) y pruebas con Vitest.
