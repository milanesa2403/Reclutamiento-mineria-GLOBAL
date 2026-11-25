package com.mineria.reclutamiento.web;

import com.mineria.reclutamiento.modelo.Oferta;
import com.mineria.reclutamiento.servicio.OfertaServicio;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

/** Consulta de convocatorias (cualquier usuario autenticado). */
@RestController
@RequestMapping("/v1/ofertas")
public class OfertaControlador {
    private final OfertaServicio ofertas;

    public OfertaControlador(OfertaServicio ofertas) { this.ofertas = ofertas; }

    @GetMapping
    public List<Oferta> buscar(@RequestParam(defaultValue = "") String especialidad, @RequestParam(defaultValue = "") String pais) {
        return ofertas.buscar(especialidad, pais);
    }

    @GetMapping("/{id}")
    public Oferta obtener(@PathVariable UUID id) { return ofertas.obtener(id); }
}
