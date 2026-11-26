package com.mineria.reclutamiento.modules.convocatoria.controller;

import com.mineria.reclutamiento.core.response.ApiResponse;
import com.mineria.reclutamiento.modules.convocatoria.dto.ConvocatoriaRequest;
import com.mineria.reclutamiento.modules.convocatoria.dto.ConvocatoriaResponse;
import com.mineria.reclutamiento.modules.convocatoria.dto.FechaCierreRequest;
import com.mineria.reclutamiento.modules.convocatoria.dto.MoverConvocatoriaRequest;
import com.mineria.reclutamiento.modules.convocatoria.entity.EstadoConvocatoria;
import com.mineria.reclutamiento.modules.convocatoria.service.ConvocatoriaEmpresaService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/empresa/convocatorias")
@PreAuthorize("hasRole('EMPRESA')")
public class ConvocatoriaEmpresaController {

    private final ConvocatoriaEmpresaService service;

    public ConvocatoriaEmpresaController(ConvocatoriaEmpresaService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<ConvocatoriaResponse>> listar(Authentication auth,
                                                          @RequestParam(required = false) EstadoConvocatoria estado) {
        return ApiResponse.ok(service.listar(auth.getName(), estado));
    }

    @GetMapping("/{id}")
    public ApiResponse<ConvocatoriaResponse> detalle(Authentication auth, @PathVariable Long id) {
        return ApiResponse.ok(service.detalle(auth.getName(), id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ConvocatoriaResponse>> crear(Authentication auth,
                                                                   @Valid @RequestBody ConvocatoriaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Convocatoria guardada como borrador", service.crear(auth.getName(), request)));
    }

    @PutMapping("/{id}")
    public ApiResponse<ConvocatoriaResponse> actualizar(Authentication auth, @PathVariable Long id,
                                                        @Valid @RequestBody ConvocatoriaRequest request) {
        return ApiResponse.ok("Convocatoria actualizada", service.actualizar(auth.getName(), id, request));
    }

    @PostMapping("/{id}/publicar")
    public ApiResponse<ConvocatoriaResponse> publicar(Authentication auth, @PathVariable Long id) {
        return ApiResponse.ok("Convocatoria publicada", service.publicar(auth.getName(), id));
    }

    @PostMapping("/{id}/cerrar")
    public ApiResponse<ConvocatoriaResponse> cerrar(Authentication auth, @PathVariable Long id) {
        return ApiResponse.ok("Convocatoria cerrada", service.cerrar(auth.getName(), id));
    }

    @PatchMapping("/{id}/fecha-cierre")
    public ApiResponse<ConvocatoriaResponse> cambiarFechaCierre(Authentication auth, @PathVariable Long id,
                                                                @RequestBody FechaCierreRequest request) {
        ConvocatoriaResponse r = service.cambiarFechaCierre(auth.getName(), id, request.fechaCierre());
        return ApiResponse.ok("Fecha de cierre actualizada", r);
    }

    @PutMapping("/{id}/busqueda")
    public ApiResponse<ConvocatoriaResponse> mover(Authentication auth, @PathVariable Long id,
                                                   @RequestBody MoverConvocatoriaRequest request) {
        return ApiResponse.ok("Convocatoria movida", service.mover(auth.getName(), id, request.idBusqueda()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> eliminar(Authentication auth, @PathVariable Long id) {
        service.eliminar(auth.getName(), id);
        return ApiResponse.ok("Convocatoria eliminada", null);
    }
}
