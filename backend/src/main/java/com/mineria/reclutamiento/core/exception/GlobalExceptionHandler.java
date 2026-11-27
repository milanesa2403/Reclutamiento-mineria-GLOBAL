package com.mineria.reclutamiento.core.exception;

import com.mineria.reclutamiento.core.response.ApiResponse;
import com.mineria.reclutamiento.modules.documento.storage.AlmacenamientoNoConfiguradoException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Hay campos con errores de validación", errores));
    }

    @ExceptionHandler(CamposInvalidosException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> camposInvalidos(CamposInvalidosException ex) {
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, ex.getMessage(), new LinkedHashMap<>(ex.getErrores())));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> cuerpoInvalido(HttpMessageNotReadableException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es válido (JSON mal formado o tipos incorrectos)");
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> badRequest(BadRequestException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> noEncontrado(ResourceNotFoundException ex) {
        return respuesta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> duplicado(DuplicateResourceException ex) {
        return respuesta(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> credenciales(BadCredentialsException ex) {
        return respuesta(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos");
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> cuentaInactiva(DisabledException ex) {
        return respuesta(HttpStatus.FORBIDDEN, "Debes verificar tu correo antes de iniciar sesión");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> accesoDenegado(AccessDeniedException ex) {
        return respuesta(HttpStatus.FORBIDDEN, "No tienes permiso para realizar esta acción");
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> prohibido(ForbiddenException ex) {
        return respuesta(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> parametroInvalido(MethodArgumentTypeMismatchException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, "El parámetro '" + ex.getName() + "' no tiene un valor válido");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> archivoGrande(MaxUploadSizeExceededException ex) {
        return respuesta(HttpStatus.PAYLOAD_TOO_LARGE, "El archivo supera el tamaño máximo de 5 MB.");
    }

    @ExceptionHandler(AlmacenamientoNoConfiguradoException.class)
    public ResponseEntity<ApiResponse<Void>> almacenamiento(AlmacenamientoNoConfiguradoException ex) {
        log.warn("Falta APP_S3_BUCKET: no se pueden subir ni ver archivos");
        return respuesta(HttpStatus.SERVICE_UNAVAILABLE, "El almacenamiento de archivos no está disponible.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> integridad(DataIntegrityViolationException ex) {
        String estadoSql = ex.getMostSpecificCause() instanceof SQLException sql ? sql.getSQLState() : null;
        log.warn("Conflicto de integridad en la BD (SQLState {})", estadoSql);
        String mensaje = "23505".equals(estadoSql)
                ? "Alguno de los datos ya está registrado. Revisa la información e intenta nuevamente."
                : "No se pudo guardar porque entra en conflicto con información existente. Actualiza e intenta nuevamente.";
        return respuesta(HttpStatus.CONFLICT, mensaje);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> generico(Exception ex) {
        if (ex instanceof ErrorResponse er) {
            String detalle = er.getBody().getDetail();
            return respuesta(er.getStatusCode(), detalle != null ? detalle : "Petición no válida");
        }
        log.error("Error no controlado", ex);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error interno. Intenta nuevamente.");
    }

    private ResponseEntity<ApiResponse<Void>> respuesta(HttpStatusCode status, String mensaje) {
        return ResponseEntity.status(status).body(ApiResponse.error(mensaje));
    }
}
