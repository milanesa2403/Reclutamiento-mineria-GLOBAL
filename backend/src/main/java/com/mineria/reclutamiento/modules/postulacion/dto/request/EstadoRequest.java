package com.mineria.reclutamiento.modules.postulacion.dto.request;

import com.mineria.reclutamiento.modules.postulacion.entity.EstadoPostulacion;
import jakarta.validation.constraints.NotNull;

public record EstadoRequest(
        @NotNull(message = "Indica el nuevo estado") EstadoPostulacion estado
) {
}
