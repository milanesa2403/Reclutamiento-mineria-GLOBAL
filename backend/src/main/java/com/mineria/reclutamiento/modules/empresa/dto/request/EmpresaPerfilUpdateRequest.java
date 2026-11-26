package com.mineria.reclutamiento.modules.empresa.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmpresaPerfilUpdateRequest(
        @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
        String descripcion,
        @Pattern(regexp = "^$|" + TELEFONO, message = "Ingresa un teléfono válido, por ejemplo 01 444 5555")
        String telefono,
        Long idSectorMinero,
        @Pattern(regexp = TELEFONO, message = "Ingresa un teléfono válido para el representante")
        String telefonoRepresentante,
        @Size(max = 255, message = "La dirección del representante no puede superar los 255 caracteres")
        String direccionRepresentante,
        @Pattern(regexp = "^(?!\\s*$).+", message = "El cargo del representante no puede quedar vacío")
        @Size(max = 100, message = "El cargo no puede superar los 100 caracteres")
        String cargoRepresentante
) {
    private static final String TELEFONO = "^[+\\d][\\d\\s-]{6,19}$";
}
