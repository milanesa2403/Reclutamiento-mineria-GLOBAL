package com.mineria.reclutamiento.modules.postulante.dto.response;

import com.mineria.reclutamiento.modules.documento.dto.DocumentoResponse;
import java.time.LocalDate;
import java.util.List;

public record PerfilProfesionalResponse(
        String resumenProfesional,
        List<Especialidad> especialidades,
        List<String> habilidades,
        List<Experiencia> experiencias,
        List<Formacion> formaciones,
        List<Certificacion> certificaciones,
        boolean perfilCompleto,
        List<String> pendientes
) {
    public record Especialidad(String nombre, boolean principal) {}
    public record Experiencia(String puesto, String empresa, LocalDate fechaInicio, LocalDate fechaFin, String descripcion) {}
    public record Formacion(String institucion, String carreraGrado, LocalDate fechaInicio, LocalDate fechaFin) {}
    public record Certificacion(Long id, Long idCatalogoCertificacion, String nombre, String entidadEmisora, LocalDate fechaEmision,
                                DocumentoResponse archivo) {}
}
