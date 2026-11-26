package com.mineria.reclutamiento.modules.postulante.dto.request;

import com.mineria.reclutamiento.modules.persona.entity.TipoDocumentoIdentidad;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PostulanteRequest(
        @NotBlank(message = "El país es obligatorio")
        String pais,
        @NotNull(message = "El tipo de documento es obligatorio")
        TipoDocumentoIdentidad tipoDocumento,
        @NotBlank(message = "El número de documento es obligatorio")
        @Size(max = 20, message = "El número de documento no puede superar los 20 caracteres")
        String numeroDocumento,
        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 100, message = "Los nombres no pueden superar los 100 caracteres")
        String nombres,
        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 100, message = "Los apellidos no pueden superar los 100 caracteres")
        String apellidos,
        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "^[+\\d][\\d\\s-]{6,19}$", message = "Ingresa un teléfono válido, por ejemplo +51 987 654 321")
        String telefono,
        @Size(max = 2000, message = "El resumen profesional no puede superar los 2000 caracteres")
        String resumenProfesional
) {
}
