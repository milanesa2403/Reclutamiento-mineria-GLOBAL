package com.mineria.reclutamiento.web;

import com.mineria.reclutamiento.excepcion.AccesoDenegadoException;
import com.mineria.reclutamiento.excepcion.RecursoNoEncontradoException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/** RNF 29: todos los errores salen como {"codigo": "...", "mensaje": "..."}. */
@RestControllerAdvice
public class ManejadorErrores {
    private ResponseEntity<Map<String, String>> error(HttpStatus s, String codigo, String msg) {
        Map<String, String> cuerpo = new LinkedHashMap<>();
        cuerpo.put("codigo", codigo);
        cuerpo.put("mensaje", msg);
        return ResponseEntity.status(s).body(cuerpo);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> validacion(MethodArgumentNotValidException e) {
        return error(HttpStatus.BAD_REQUEST, "VALIDACION", e.getBindingResult().getAllErrors().get(0).getDefaultMessage());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingRequestHeaderException.class,
            MissingServletRequestParameterException.class, MissingServletRequestPartException.class,
            MethodArgumentTypeMismatchException.class})
    ResponseEntity<Map<String, String>> peticionMal(Exception e) {
        return error(HttpStatus.BAD_REQUEST, "PETICION_INVALIDA", "Peticion mal formada o con parametros faltantes");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> argumento(IllegalArgumentException e) {
        return error(HttpStatus.BAD_REQUEST, "DATOS_INVALIDOS", e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Map<String, String>> conflicto(IllegalStateException e) {
        return error(HttpStatus.CONFLICT, "CONFLICTO", e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Map<String, String>> integridad(DataIntegrityViolationException e) {
        return error(HttpStatus.CONFLICT, "DUPLICADO", "La operacion ya fue registrada");
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    ResponseEntity<Map<String, String>> noEncontrado(RecursoNoEncontradoException e) {
        return error(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", e.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<Map<String, String>> credenciales(BadCredentialsException e) {
        return error(HttpStatus.UNAUTHORIZED, "CREDENCIALES_INVALIDAS", e.getMessage());
    }

    @ExceptionHandler(LockedException.class)
    ResponseEntity<Map<String, String>> bloqueada(LockedException e) {
        return error(HttpStatus.LOCKED, "CUENTA_BLOQUEADA", e.getMessage());
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    ResponseEntity<Map<String, String>> denegado(AccesoDenegadoException e) {
        return error(HttpStatus.FORBIDDEN, "ACCESO_DENEGADO", e.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<Map<String, String>> grande(MaxUploadSizeExceededException e) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "ARCHIVO_GRANDE", "El archivo supera el tamano maximo permitido");
    }
}
