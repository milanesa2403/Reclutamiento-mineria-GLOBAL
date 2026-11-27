package com.mineria.reclutamiento.core.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class CamposInvalidosException extends RuntimeException {

    private final Map<String, String> errores;

    public CamposInvalidosException(Map<String, String> errores) {
        super("Hay campos con errores de validación");
        this.errores = Collections.unmodifiableMap(new LinkedHashMap<>(errores));
    }

    public Map<String, String> getErrores() { return errores; }
}
