package com.mineria.reclutamiento.modules.convocatoria.controller;

import com.mineria.reclutamiento.core.response.ApiResponse;
import com.mineria.reclutamiento.modules.convocatoria.dto.ConvocatoriaResponse;
import com.mineria.reclutamiento.modules.convocatoria.entity.TipoConvocatoria;
import com.mineria.reclutamiento.modules.convocatoria.service.ConvocatoriaPublicaService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/convocatorias")
public class ConvocatoriaPublicaController {

    private final ConvocatoriaPublicaService service;

    public ConvocatoriaPublicaController(ConvocatoriaPublicaService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<ConvocatoriaResponse>> buscar(@RequestParam(required = false) String texto,
                                                          @RequestParam(required = false) TipoConvocatoria tipo,
                                                          @RequestParam(required = false) String departamento,
                                                          @RequestParam(required = false) String provincia,
                                                          @RequestParam(required = false) String distrito) {
        return ApiResponse.ok(service.buscar(
                new ConvocatoriaPublicaService.Filtros(texto, tipo, departamento, provincia, distrito)));
    }

    @GetMapping("/{id}")
    public ApiResponse<ConvocatoriaResponse> detalle(@PathVariable Long id) {
        return ApiResponse.ok(service.detalle(id));
    }
}
