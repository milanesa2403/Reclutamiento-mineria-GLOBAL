package com.mineria.reclutamiento.web;

import com.mineria.reclutamiento.modelo.RegistroAuditoria;
import com.mineria.reclutamiento.servicio.*;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/admin")
public class AdminControlador {
    private final AuditoriaServicio auditoria;
    private final ColaPostulaciones cola;
    private final EstadoSistema estado;
    private final OfertaServicio ofertas;
    private final PrivacidadServicio privacidad;
    private final ImportacionServicio importacion;

    public AdminControlador(AuditoriaServicio auditoria, ColaPostulaciones cola, EstadoSistema estado,
            OfertaServicio ofertas, PrivacidadServicio privacidad, ImportacionServicio importacion) {
        this.auditoria = auditoria;
        this.cola = cola;
        this.estado = estado;
        this.ofertas = ofertas;
        this.privacidad = privacidad;
        this.importacion = importacion;
    }

    @GetMapping("/auditoria")
    public List<RegistroAuditoria> auditoria() { return auditoria.recientes(); }

    @GetMapping("/cola")
    public Map<String, Object> cola() { return cola.resumen(); }

    @PostMapping("/cola/reprocesar-dlq")
    public Map<String, Object> reprocesar() { return Map.of("reprocesados", cola.reprocesarDlq()); }

    @PostMapping("/modo-solo-lectura")
    public Map<String, Object> modo(@RequestParam boolean activo) {
        estado.setSoloLectura(activo);
        return Map.of("soloLectura", estado.isSoloLectura());
    }

    @PostMapping("/mantenimiento/ejecutar")
    public Map<String, Object> mantenimiento() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ofertasArchivadas", ofertas.archivarAntiguas());
        m.put("cuentasBorradas", privacidad.purgar());
        return m;
    }

    @PostMapping("/perfiles/importar")
    public ImportacionServicio.Reporte importar(@AuthenticationPrincipal Jwt jwt, @RequestParam("archivo") MultipartFile archivo,
            @RequestParam String formato) throws IOException {
        return importacion.importarPerfiles(Actual.id(jwt), archivo.getBytes(), formato);
    }

    @GetMapping("/perfiles/exportar")
    public ResponseEntity<byte[]> exportar(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "json") String formato) {
        return Descargas.de(importacion.exportarPerfiles(Actual.id(jwt), formato), formato, "perfiles");
    }
}
