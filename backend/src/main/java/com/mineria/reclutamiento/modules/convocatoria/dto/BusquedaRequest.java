package com.mineria.reclutamiento.modules.convocatoria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BusquedaRequest(
        @NotBlank(message = "Escribe el nombre de la búsqueda") @Size(max = 120, message = "Máximo 120 caracteres") String nombre
) {
}
