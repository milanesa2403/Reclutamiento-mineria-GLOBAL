package com.mineria.reclutamiento.modules.empresa.controller;

import com.mineria.reclutamiento.core.response.ApiResponse;
import com.mineria.reclutamiento.modules.empresa.dto.response.SectorMineroResponse;
import com.mineria.reclutamiento.modules.empresa.service.SectorMineroService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/sectores-mineros")
public class SectorMineroController {

    private final SectorMineroService service;

    public SectorMineroController(SectorMineroService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<SectorMineroResponse>> listar() {
        return ApiResponse.ok(service.listarActivos());
    }
}
