package com.mineria.reclutamiento.web;

import com.mineria.reclutamiento.modelo.Documento;
import com.mineria.reclutamiento.modelo.TipoDocumento;
import com.mineria.reclutamiento.servicio.DocumentoServicio;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** Subir (solo TRABAJADOR, ver SeguridadConfig) y descargar (dueno, empresa receptora o admin). */
@RestController
@RequestMapping("/v1/documentos")
public class DocumentoControlador {
    private final DocumentoServicio documentos;

    public DocumentoControlador(DocumentoServicio documentos) { this.documentos = documentos; }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Documento subir(@AuthenticationPrincipal Jwt jwt, @RequestParam TipoDocumento tipo,
            @RequestParam("archivo") MultipartFile archivo) throws IOException {
        return documentos.subir(Actual.id(jwt), tipo, archivo.getOriginalFilename(), archivo.getBytes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> descargar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        var d = documentos.descargar(Actual.id(jwt), Actual.rol(jwt), id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + d.nombreArchivo() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(d.contenido());
    }
}
