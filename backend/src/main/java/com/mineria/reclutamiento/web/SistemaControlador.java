package com.mineria.reclutamiento.web;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Salud (health check del ALB) y versiones retiradas (RNF 29). */
@RestController
public class SistemaControlador {
    @GetMapping({"/api/health", "/v1/health"})
    public Map<String, String> salud() { return Map.of("estado", "UP"); }

    @RequestMapping("/v0/**")
    public ResponseEntity<Map<String, String>> versionRetirada() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("codigo", "VERSION_RETIRADA");
        m.put("mensaje", "La version v0 fue retirada. Use la version vigente.");
        m.put("versionVigente", "v1");
        return ResponseEntity.status(HttpStatus.GONE).body(m);
    }
}
