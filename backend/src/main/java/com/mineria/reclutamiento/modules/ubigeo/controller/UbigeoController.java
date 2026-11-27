package com.mineria.reclutamiento.modules.ubigeo.controller;

import com.mineria.reclutamiento.core.exception.BadRequestException;
import com.mineria.reclutamiento.core.response.ApiResponse;
import com.mineria.reclutamiento.modules.ubigeo.dto.LugarResponse;
import com.mineria.reclutamiento.modules.ubigeo.repository.UbigeoRepository;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/ubigeo")
public class UbigeoController {

    private final UbigeoRepository repository;

    public UbigeoController(UbigeoRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/departamentos")
    @Transactional(readOnly = true)
    public ApiResponse<List<LugarResponse>> departamentos() {
        return ApiResponse.ok(repository.departamentos());
    }

    @GetMapping("/departamentos/{codigo}/provincias")
    @Transactional(readOnly = true)
    public ApiResponse<List<LugarResponse>> provincias(@PathVariable String codigo) {
        exigirDigitos(codigo, 2);
        return ApiResponse.ok(repository.provincias(codigo));
    }

    @GetMapping("/provincias/{codigo}/distritos")
    @Transactional(readOnly = true)
    public ApiResponse<List<LugarResponse>> distritos(@PathVariable String codigo) {
        exigirDigitos(codigo, 4);
        return ApiResponse.ok(repository.distritos(codigo));
    }

    private static void exigirDigitos(String codigo, int cantidad) {
        if (codigo == null || !codigo.matches("\\d{" + cantidad + "}")) {
            throw new BadRequestException("Código de ubicación no válido (debe tener " + cantidad + " dígitos)");
        }
    }
}
