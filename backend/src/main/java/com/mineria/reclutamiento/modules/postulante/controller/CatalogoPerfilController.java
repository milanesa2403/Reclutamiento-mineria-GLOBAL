package com.mineria.reclutamiento.modules.postulante.controller;

import com.mineria.reclutamiento.core.response.ApiResponse;
import com.mineria.reclutamiento.modules.postulante.repository.CatalogoEspecialidadRepository;
import com.mineria.reclutamiento.modules.postulante.repository.CatalogoHabilidadRepository;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class CatalogoPerfilController {

    public record EspecialidadCatalogo(Integer id, String nombre, String descripcion) {
    }

    public record HabilidadCatalogo(Integer id, String nombre, String categoria) {
    }

    private final CatalogoEspecialidadRepository especialidades;
    private final CatalogoHabilidadRepository habilidades;

    public CatalogoPerfilController(CatalogoEspecialidadRepository especialidades,
                                    CatalogoHabilidadRepository habilidades) {
        this.especialidades = especialidades;
        this.habilidades = habilidades;
    }

    @GetMapping("/catalogo-especialidades")
    @Transactional(readOnly = true)
    public ApiResponse<List<EspecialidadCatalogo>> listarEspecialidades() {
        return ApiResponse.ok(especialidades.findAllByOrderByNombreAsc().stream()
                .map(e -> new EspecialidadCatalogo(e.getId(), e.getNombre(), e.getDescripcion())).toList());
    }

    @GetMapping("/catalogo-habilidades")
    @Transactional(readOnly = true)
    public ApiResponse<List<HabilidadCatalogo>> listarHabilidades() {
        return ApiResponse.ok(habilidades.findAllByOrderByCategoriaAscNombreAsc().stream()
                .map(h -> new HabilidadCatalogo(h.getId(), h.getNombre(), h.getCategoria())).toList());
    }
}
