package com.mineria.reclutamiento.modules.convocatoria.controller;

import com.mineria.reclutamiento.core.response.ApiResponse;
import com.mineria.reclutamiento.modules.convocatoria.dto.BusquedaRequest;
import com.mineria.reclutamiento.modules.convocatoria.dto.BusquedaResponse;
import com.mineria.reclutamiento.modules.convocatoria.service.BusquedaService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/empresa/busquedas")
@PreAuthorize("hasRole('EMPRESA')")
public class BusquedaController {

    private final BusquedaService service;

    public BusquedaController(BusquedaService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<BusquedaResponse>> listar(Authentication auth) {
        return ApiResponse.ok(service.listar(auth.getName()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BusquedaResponse>> crear(Authentication auth,
                                                               @Valid @RequestBody BusquedaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Búsqueda creada", service.crear(auth.getName(), request.nombre())));
    }

    @PutMapping("/{id}")
    public ApiResponse<BusquedaResponse> renombrar(Authentication auth, @PathVariable Long id,
                                                   @Valid @RequestBody BusquedaRequest request) {
        return ApiResponse.ok("Búsqueda actualizada", service.renombrar(auth.getName(), id, request.nombre()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> eliminar(Authentication auth, @PathVariable Long id) {
        service.eliminar(auth.getName(), id);
        return ApiResponse.ok("Búsqueda eliminada; sus convocatorias quedaron sueltas", null);
    }
}
