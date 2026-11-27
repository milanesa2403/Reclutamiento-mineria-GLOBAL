package com.mineria.reclutamiento.modules.postulante.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public record PerfilProfesionalRequest(
        @Size(max = 2000, message = "El resumen profesional no puede superar los 2000 caracteres")
        String resumenProfesional,

        @NotNull(message = "Registra al menos una especialidad")
        @Size(min = 1, max = MAX_ELEMENTOS, message = "Registra entre 1 y 20 especialidades")
        List<@Valid EspecialidadItem> especialidades,

        @NotNull(message = "Registra al menos una habilidad")
        @Size(min = 1, max = MAX_ELEMENTOS, message = "Registra entre 1 y 20 habilidades")
        List<@NotBlank(message = "La habilidad no puede estar vacía")
             @Size(max = 100, message = "Máximo 100 caracteres") String> habilidades,

        @Size(max = MAX_ELEMENTOS, message = "Máximo 20 experiencias")
        List<@Valid ExperienciaItem> experiencias,

        @Size(max = MAX_ELEMENTOS, message = "Máximo 20 estudios")
        List<@Valid FormacionItem> formaciones,

        @Size(max = MAX_ELEMENTOS, message = "Máximo 20 certificaciones")
        List<@Valid CertificacionItem> certificaciones
) {
    public static final int MAX_ELEMENTOS = 20;

    public PerfilProfesionalRequest {
        especialidades = vacioSiNull(especialidades);
        habilidades = vacioSiNull(habilidades);
        experiencias = vacioSiNull(experiencias);
        formaciones = vacioSiNull(formaciones);
        certificaciones = vacioSiNull(certificaciones);
    }

    private static <T> List<T> vacioSiNull(List<T> lista) {
        return lista == null ? new ArrayList<>() : lista;
    }

    public record EspecialidadItem(
            @NotBlank(message = "Escribe tu especialidad")
            @Size(max = 120, message = "Máximo 120 caracteres")
            String nombre,
            boolean principal
    ) {
    }

    public record ExperienciaItem(
            @NotBlank(message = "Escribe el puesto")
            @Size(max = 150, message = "Máximo 150 caracteres")
            String puesto,
            @NotBlank(message = "Escribe la empresa")
            @Size(max = 150, message = "Máximo 150 caracteres")
            String empresa,
            @NotNull(message = "Indica la fecha de inicio")
            @PastOrPresent(message = "La fecha de inicio no puede ser futura")
            LocalDate fechaInicio,
            @PastOrPresent(message = "La fecha de fin no puede ser futura")
            LocalDate fechaFin,
            @Size(max = 2000, message = "Máximo 2000 caracteres")
            String descripcion
    ) {
    }

    public record FormacionItem(
            @NotBlank(message = "Escribe la institución")
            @Size(max = 200, message = "Máximo 200 caracteres")
            String institucion,
            @NotBlank(message = "Escribe la carrera o el grado")
            @Size(max = 150, message = "Máximo 150 caracteres")
            String carreraGrado,
            @NotNull(message = "Indica la fecha de inicio")
            @PastOrPresent(message = "La fecha de inicio no puede ser futura")
            LocalDate fechaInicio,
            @PastOrPresent(message = "La fecha de fin no puede ser futura")
            LocalDate fechaFin
    ) {
    }

    public record CertificacionItem(
            Long id,
            Long idCatalogoCertificacion,
            @NotBlank(message = "Escribe el nombre de la certificación")
            @Size(max = 200, message = "Máximo 200 caracteres")
            String nombre,
            @NotBlank(message = "Escribe la entidad que la emitió")
            @Size(max = 150, message = "Máximo 150 caracteres")
            String entidadEmisora,
            @PastOrPresent(message = "La fecha de emisión no puede ser futura")
            LocalDate fechaEmision
    ) {
        public CertificacionItem(String nombre, String entidadEmisora, LocalDate fechaEmision) {
            this(null, null, nombre, entidadEmisora, fechaEmision);
        }
    }
}
