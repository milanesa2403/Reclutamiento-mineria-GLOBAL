package com.mineria.reclutamiento.modules.postulante.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ActualizarPostulanteRequest(
        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "^[+\\d][\\d\\s-]{6,19}$", message = "Ingresa un teléfono válido, por ejemplo 987 654 321")
        String telefono,
        @Size(max = 255, message = "La dirección no puede superar los 255 caracteres")
        String direccion,
        @Size(max = 2000, message = "El resumen profesional no puede superar los 2000 caracteres")
        String resumenProfesional
) {
}
