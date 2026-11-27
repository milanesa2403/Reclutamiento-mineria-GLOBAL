package com.mineria.reclutamiento.modules.convocatoria.controller;

import com.mineria.reclutamiento.core.response.ApiResponse;
import com.mineria.reclutamiento.modules.convocatoria.dto.ConvocatoriaResponse;
import com.mineria.reclutamiento.modules.convocatoria.service.ConvocatoriaGuardadaService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/postulantes/me/convocatorias-guardadas")
@PreAuthorize("hasRole('POSTULANTE')")
public class ConvocatoriaGuardadaController {

    private final ConvocatoriaGuardadaService service;

    public ConvocatoriaGuardadaController(ConvocatoriaGuardadaService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<ConvocatoriaResponse>> listar(Authentication auth) {
        return ApiResponse.ok(service.listar(auth.getName()));
    }

    @PutMapping("/{idConvocatoria}")
    public ApiResponse<Void> guardar(Authentication auth, @PathVariable Long idConvocatoria) {
        service.guardar(auth.getName(), idConvocatoria);
        return ApiResponse.ok("Convocatoria guardada", null);
    }

    @DeleteMapping("/{idConvocatoria}")
    public ApiResponse<Void> quitar(Authentication auth, @PathVariable Long idConvocatoria) {
        service.quitar(auth.getName(), idConvocatoria);
        return ApiResponse.ok("Convocatoria quitada de guardadas", null);
    }
}
