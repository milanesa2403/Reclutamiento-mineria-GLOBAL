package com.mineria.reclutamiento.modules.ubigeo.dto;

import com.mineria.reclutamiento.modules.ubigeo.entity.Ubigeo;

public record UbicacionResponse(String codigo, String departamento, String provincia, String distrito) {

    public static UbicacionResponse de(Ubigeo u) {
        return u == null ? null
                : new UbicacionResponse(u.getCodigo(), u.getDepartamento(), u.getProvincia(), u.getDistrito());
    }
}
