package com.mineria.reclutamiento.modules.convocatoria.dto;

import com.mineria.reclutamiento.modules.convocatoria.entity.Busqueda;
import java.time.LocalDateTime;

public record BusquedaResponse(Long id, String nombre, LocalDateTime fechaCreacion) {

    public static BusquedaResponse de(Busqueda b) {
        return new BusquedaResponse(b.getId(), b.getNombre(), b.getFechaCreacion());
    }
}
