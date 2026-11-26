package com.mineria.reclutamiento.modules.postulacion.controller;

import com.mineria.reclutamiento.core.exception.ResourceNotFoundException;
import com.mineria.reclutamiento.core.response.ApiResponse;
import com.mineria.reclutamiento.modules.documento.dto.UrlTemporalResponse;
import com.mineria.reclutamiento.modules.postulacion.dto.request.EstadoRequest;
import com.mineria.reclutamiento.modules.postulacion.dto.response.CandidatoResponse;
import com.mineria.reclutamiento.modules.postulacion.dto.request.EntrevistaRequest;
import com.mineria.reclutamiento.modules.postulacion.service.EntrevistaService;
import com.mineria.reclutamiento.modules.postulacion.service.CandidatoService;
import com.mineria.reclutamiento.modules.postulante.entity.DocumentoPostulante;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/empresa")
@PreAuthorize("hasRole('EMPRESA')")
public class CandidatoController {

    private final CandidatoService service;
    private final EntrevistaService entrevistaService;

    public CandidatoController(CandidatoService service, EntrevistaService entrevistaService) {
        this.service = service;
        this.entrevistaService = entrevistaService;
    }

    @PostMapping("/entrevistas")
    public ApiResponse<Void> citar(Authentication authentication, @Valid @RequestBody EntrevistaRequest request) {
        entrevistaService.citar(authentication.getName(), request.postulaciones(), request.entrevista());
        return ApiResponse.ok("Citación enviada", null);
    }

    @GetMapping("/convocatorias/{idConvocatoria}/postulaciones")
    public ApiResponse<List<CandidatoResponse>> candidatos(Authentication authentication, @PathVariable Long idConvocatoria) {
        return ApiResponse.ok(service.candidatos(authentication.getName(), idConvocatoria));
    }

    @PatchMapping("/postulaciones/{id}/estado")
    public ApiResponse<CandidatoResponse> cambiarEstado(Authentication authentication, @PathVariable Long id,
                                                        @Valid @RequestBody EstadoRequest request) {
        return ApiResponse.ok("Estado actualizado", service.cambiarEstado(authentication.getName(), id, request.estado()));
    }

    @GetMapping("/postulaciones/{id}/documentos/{tipo}/url")
    public ApiResponse<UrlTemporalResponse> urlDocumento(Authentication authentication, @PathVariable Long id,
                                                         @PathVariable String tipo) {
        DocumentoPostulante documento = DocumentoPostulante.desdeRuta(tipo)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de documento no válido: usa cv, foto-dni, cul o emo"));
        return ApiResponse.ok(service.urlDocumento(authentication.getName(), id, documento));
    }

    @GetMapping("/postulaciones/{id}/certificaciones/{idCertificacion}/url")
    public ApiResponse<UrlTemporalResponse> urlCertificado(Authentication authentication, @PathVariable Long id,
                                                           @PathVariable Long idCertificacion) {
        return ApiResponse.ok(service.urlCertificado(authentication.getName(), id, idCertificacion));
    }
}
