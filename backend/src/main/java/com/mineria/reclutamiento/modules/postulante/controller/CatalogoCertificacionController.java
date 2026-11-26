package com.mineria.reclutamiento.modules.postulante.controller;

import com.mineria.reclutamiento.core.response.ApiResponse;
import com.mineria.reclutamiento.modules.postulante.repository.CatalogoCertificacionRepository;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/catalogo-certificaciones")
public class CatalogoCertificacionController {

    public record TipoCertificacion(Long id, String nombre) {
    }

    private final CatalogoCertificacionRepository repository;

    public CatalogoCertificacionController(CatalogoCertificacionRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ApiResponse<List<TipoCertificacion>> listar() {
        return ApiResponse.ok(repository.findAllByOrderByNombreAsc().stream()
                .map(t -> new TipoCertificacion(t.getId(), t.getNombre())).toList());
    }
}
