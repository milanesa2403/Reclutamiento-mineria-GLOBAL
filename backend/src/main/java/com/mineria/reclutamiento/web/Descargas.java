package com.mineria.reclutamiento.web;

import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

final class Descargas {
    private Descargas() {}

    static ResponseEntity<byte[]> de(byte[] datos, String formato, String nombre) {
        boolean csv = "csv".equalsIgnoreCase(formato);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + (csv ? ".csv" : ".json") + "\"")
                .contentType(csv ? new MediaType("text", "csv", StandardCharsets.UTF_8) : MediaType.APPLICATION_JSON)
                .body(datos);
    }
}
