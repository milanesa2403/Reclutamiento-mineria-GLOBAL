package com.mineria.reclutamiento.modules.postulacion.dto.request;

import com.mineria.reclutamiento.modules.postulacion.entity.Entrevista;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record EntrevistaRequest(
        @NotEmpty(message = "Elige al menos un postulante.") List<Long> postulaciones,
        @NotNull(message = "Indica los datos de la entrevista.") Entrevista entrevista
) {
}
