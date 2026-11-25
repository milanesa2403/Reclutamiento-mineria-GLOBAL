# Backend - Reclutamiento minero (Spring Boot 3, Java 17)

API REST `/v1` en JSON (UTF-8), documentada con OpenAPI 3.0. Base H2 en memoria por defecto; PostgreSQL con el perfil `prod`.

## Ejecutar
```bash
cd backend
mvn spring-boot:run                 # http://localhost:8080
mvn verify                          # pruebas + reporte de cobertura (target/site/jacoco/index.html)
./pruebas/prueba-rapida.sh          # prueba de humo end-to-end (con el backend levantado)
```
Admin de prueba: `admin@mineria.com` / `AdminMineria123!` (solo perfiles distintos de `prod`).
Documentacion interactiva: `http://localhost:8080/swagger-ui.html` · JSON OpenAPI: `/v3/api-docs`.

## Endpoints principales
| Metodo y ruta | Rol | Descripcion |
|---|---|---|
| POST `/v1/auth/registro`, `/v1/auth/login` | publico | Registro (con `consentimiento`) y login (JWT 30 min) |
| GET `/v1/ofertas?especialidad=&pais=`, `/v1/ofertas/{id}` | autenticado | Busqueda de convocatorias abiertas |
| GET/PUT `/v1/trabajador/perfil` | TRABAJADOR | Perfil (pais ISO 3166-1, idioma ISO 639-1) |
| POST `/v1/documentos` (multipart `tipo`,`archivo`) | TRABAJADOR | Sube CV/certificacion (max 10 MB, SHA-256, 1 original por contenido) |
| GET `/v1/documentos/{id}` | dueno, empresa receptora, admin | Descarga (403 + auditoria si no corresponde) |
| POST `/v1/trabajador/postulaciones` + `Idempotency-Key` | TRABAJADOR | Postula; repetir no duplica. En solo lectura responde 202 y encola |
| GET `/v1/trabajador/postulaciones`, `/v1/trabajador/documentos` | TRABAJADOR | Mis datos |
| POST/GET `/v1/empresa/ofertas`, POST `.../{id}/cerrar` | EMPRESA | Publicar y cerrar (solo ofertas propias) |
| GET `/v1/empresa/ofertas/{id}/postulaciones` | EMPRESA | Postulantes de una oferta propia |
| PUT `/v1/empresa/postulaciones/{id}/estado` + `Idempotency-Key` | EMPRESA | Cambia estado |
| GET `/v1/empresa/perfiles?especialidad=&minAnios=` | EMPRESA | Filtra candidatos (queda auditado) |
| POST `/v1/empresa/ofertas/importar`, GET `.../exportar?formato=csv|json` | EMPRESA | Importacion/exportacion con reporte de errores por fila |
| GET/POST `/v1/postulaciones/{id}/mensajes` + `Idempotency-Key` | trabajador y empresa de la postulacion | Mensajes de entrevista |
| POST `/v1/cuenta/solicitud-borrado` | autenticado | Derecho de supresion |
| GET `/v1/admin/auditoria`, `/v1/admin/cola` | ADMINISTRADOR | Auditoria y estado de la cola/DLQ |
| POST `/v1/admin/modo-solo-lectura?activo=`, `/v1/admin/cola/reprocesar-dlq`, `/v1/admin/mantenimiento/ejecutar` | ADMINISTRADOR | Operacion |
| POST `/v1/admin/perfiles/importar`, GET `/v1/admin/perfiles/exportar` | ADMINISTRADOR | Importacion/exportacion de perfiles |
| GET `/api/health`, `/v1/health` | publico | Health check del ALB |
| `/v0/**` | publico | 410 Gone, indica la version vigente `v1` |

## Errores (RNF 29)
Siempre `{"codigo":"...","mensaje":"..."}`: 400 datos invalidos, 401, 403, 404, 409 conflicto/duplicado, 410 version retirada, 413 archivo grande, 423 cuenta bloqueada, 429 IP bloqueada (con `Retry-After`), 503 mantenimiento.

## Mapa RNF -> codigo
| RNF | Implementacion |
|---|---|
| 2, 24 | `EstadoSistema` + `SoloLecturaFiltro` (503 "Estamos en mantenimiento") + `ColaPostulaciones` (encola postulaciones) |
| 5 | `ColaPostulaciones`: 5 reintentos y DLQ con alerta en log; reprocesable desde admin |
| 7 | `SeguridadConfig` (roles en servidor) + validacion de propiedad en servicios; 403 auditado con usuario, IP y hora |
| 8 | Clave >= 12, bloqueo 15 min tras 5 fallos (`AutenticacionServicioImpl`), JWT de 30 min |
| 10 | Archivos privados; descarga solo con permiso (`DocumentoServicioImpl`). El cifrado AES-256 lo aporta S3/RDS/KMS (Terraform) |
| 11 | `AuditoriaServicioImpl` (REQUIRES_NEW) + `LimiteIpFiltro`: >10 fallos/10 min -> 429 por 15 min |
| 12 | Consentimiento obligatorio, `PrivacidadServicio` (borrado tras periodo de gracia de 7 dias, maximo legal 30) |
| 16 | Subida <= 10 MB, firma PDF invalida -> `PENDIENTE_REVISION` |
| 20 | SHA-256 por archivo; un solo original fisico por contenido |
| 20 (archivado) | `OfertaServicio.archivarAntiguas()` (cerradas > 6 meses), tarea diaria |
| 23 | `Idempotency-Key` + restriccion unica oferta/trabajador + transaccion unica |
| 25 | Pruebas + JaCoCo; el pipeline ejecuta `mvn verify` |
| 27 | `RequestIdFiltro` (X-Request-Id / trace del ALB) + logs JSON (`logback-spring.xml`) |
| 29 | OpenAPI 3.0 (springdoc), `/v1`, 400 y 410 estandar |
| 30 | `ImportacionServicioImpl` + `EstandaresIso` (ISO 3166-1, 639-1, 4217, fechas ISO 8601 UTC) |

## Pendiente / siguientes etapas
TOTP (2FA), cola real SQS y Lambda, reintentos con espera 1-2-4 s, cache de lecturas, circuit breaker para servicios externos, envio real de correos (SNS), version de archivos en S3, migraciones Flyway, y subir el JAR a las EC2 (el user_data actual aun levanta Nginx).
