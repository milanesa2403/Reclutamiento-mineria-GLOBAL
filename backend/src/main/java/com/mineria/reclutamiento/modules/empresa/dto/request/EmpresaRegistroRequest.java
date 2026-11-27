package com.mineria.reclutamiento.modules.empresa.dto.request;

import com.mineria.reclutamiento.modules.persona.entity.TipoDocumentoIdentidad;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EmpresaRegistroRequest(
        @NotBlank(message = "El país de la empresa es obligatorio")
        String pais,
        @NotBlank(message = "El identificador fiscal es obligatorio")
        @Size(max = 20, message = "El identificador fiscal no puede superar los 20 caracteres")
        String identificadorFiscal,
        @NotBlank(message = "La razón social es obligatoria")
        @Size(max = 200, message = "La razón social no puede superar los 200 caracteres")
        String razonSocial,
        @Size(max = 255, message = "La dirección no puede superar los 255 caracteres")
        String direccion,
        @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
        String telefono,
        @NotNull(message = "Debe seleccionar un sector minero")
        Long idSectorMinero,
        @NotBlank(message = "El país del representante es obligatorio")
        String paisRepresentante,
        @NotNull(message = "El tipo de documento del representante es obligatorio")
        TipoDocumentoIdentidad tipoDocumentoRepresentante,
        @NotBlank(message = "El número de documento del representante es obligatorio")
        @Size(max = 20, message = "El número de documento no puede superar los 20 caracteres")
        String numeroDocumentoRepresentante,
        @NotBlank(message = "Los nombres del representante son obligatorios")
        @Size(max = 100, message = "Los nombres del representante no pueden superar los 100 caracteres")
        String nombresRepresentante,
        @NotBlank(message = "Los apellidos del representante son obligatorios")
        @Size(max = 100, message = "Los apellidos del representante no pueden superar los 100 caracteres")
        String apellidosRepresentante,
        @NotBlank(message = "El teléfono del representante es obligatorio")
        @Size(max = 20, message = "El teléfono del representante no puede superar los 20 caracteres")
        String telefonoRepresentante,
        @Size(max = 255, message = "La dirección del representante no puede superar los 255 caracteres")
        String direccionRepresentante,
        @NotBlank(message = "El cargo del representante es obligatorio")
        @Size(max = 100, message = "El cargo del representante no puede superar los 100 caracteres")
        String cargoRepresentante
) {
}
