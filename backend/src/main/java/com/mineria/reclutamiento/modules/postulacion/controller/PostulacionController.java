package com.mineria.reclutamiento.modules.postulacion.controller;

import com.mineria.reclutamiento.core.response.ApiResponse;
import com.mineria.reclutamiento.modules.postulacion.dto.request.PostularRequest;
import com.mineria.reclutamiento.modules.postulacion.dto.response.MiPostulacionResponse;
import com.mineria.reclutamiento.modules.postulacion.dto.response.RequisitosPostulacionResponse;
import com.mineria.reclutamiento.modules.postulacion.service.PostulacionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/postulaciones")
@PreAuthorize("hasRole('POSTULANTE')")
public class PostulacionController {

    private final PostulacionService service;

    public PostulacionController(PostulacionService service) {
        this.service = service;
    }

    @GetMapping("/requisitos")
    public ApiResponse<RequisitosPostulacionResponse> requisitos(Authentication authentication,
                                                                 @RequestParam(required = false) Long idConvocatoria) {
        return ApiResponse.ok(service.requisitos(authentication.getName(), idConvocatoria));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MiPostulacionResponse>> postular(Authentication authentication,
                                                                       @Valid @RequestBody PostularRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Postulación enviada correctamente", service.postular(authentication.getName(), request)));
    }

    @GetMapping("/mias")
    public ApiResponse<List<MiPostulacionResponse>> misPostulaciones(Authentication authentication) {
        return ApiResponse.ok(service.misPostulaciones(authentication.getName()));
    }

    @PatchMapping("/{id}/confirmar")
    public ApiResponse<MiPostulacionResponse> confirmar(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.ok("Oferta confirmada", service.confirmar(authentication.getName(), id));
    }

    @PatchMapping("/{id}/retirar")
    public ApiResponse<MiPostulacionResponse> retirar(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.ok("Te retiraste del proceso", service.retirar(authentication.getName(), id));
    }

    @PatchMapping("/vistas")
    public ApiResponse<Void> marcarVistas(Authentication authentication) {
        service.marcarVistas(authentication.getName());
        return ApiResponse.ok(null);
    }
}
