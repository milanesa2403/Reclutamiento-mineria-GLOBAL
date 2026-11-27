package com.mineria.reclutamiento.modules.convocatoria.dto;

import com.mineria.reclutamiento.modules.convocatoria.entity.TipoConvocatoria;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record ConvocatoriaRequest(
        @NotNull(message = "Elige el tipo de convocatoria") TipoConvocatoria tipo,
        @NotBlank(message = "Escribe el puesto") @Size(max = 150, message = "Máximo 150 caracteres") String puesto,
        @NotBlank(message = "Escribe el campamento o unidad minera") @Size(max = 150, message = "Máximo 150 caracteres") String campamento,
        @NotBlank(message = "Escribe el turno (p. ej. 14x7)") @Size(max = 50, message = "Máximo 50 caracteres") String turno,
        @NotBlank(message = "Escribe la descripción") @Size(max = 4000, message = "Máximo 4000 caracteres") String descripcion,
        @NotBlank(message = "Escribe los requisitos obligatorios") @Size(max = 4000, message = "Máximo 4000 caracteres") String requisitosObligatorios,
        @Size(max = 4000, message = "Máximo 4000 caracteres") String requisitosDeseables,
        @Min(value = 0, message = "No puede ser negativa") @Max(value = 50, message = "Máximo 50 años") Integer experienciaMinimaAnios,
        LocalDate fechaInicioLabores,
        LocalDate fechaFinLabores,
        LocalDate fechaCierre,
        @NotBlank(message = "Elige el distrito") @Pattern(regexp = "\\d{6}", message = "Distrito no válido") String codigoUbigeo,
        @Size(max = 255, message = "Máximo 255 caracteres") String direccion,
        @Size(max = 255, message = "Máximo 255 caracteres") String referencia,
        @Valid @Size(max = 20, message = "Máximo 20 certificados") List<RequisitoCertificadoRequest> certificadosRequeridos,
        @Min(value = 1, message = "Al menos 1 vacante") @Max(value = 999, message = "Máximo 999 vacantes") Integer vacantes,
        Long idBusqueda
) {

    public record RequisitoCertificadoRequest(
            @NotNull(message = "Elige el certificado") Long idCatalogoCertificacion,
            boolean requierePdf
    ) {
    }
}
