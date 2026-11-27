package com.mineria.reclutamiento.modules.postulacion.dto.response;

import com.mineria.reclutamiento.modules.postulacion.entity.Entrevista;
import com.mineria.reclutamiento.modules.postulacion.entity.EstadoPostulacion;
import com.mineria.reclutamiento.modules.postulante.dto.response.DocumentosPostulanteResponse;
import com.mineria.reclutamiento.modules.postulante.dto.response.PerfilProfesionalResponse;
import java.time.LocalDateTime;
import java.util.List;

public record CandidatoResponse(
        Long idPostulacion,
        String nombres,
        String apellidos,
        String correo,
        String telefono,
        String especialidadPrincipal,
        int aniosExperiencia,
        List<CertificacionAdjunta> certificaciones,
        DocumentosPostulanteResponse documentos,
        EstadoPostulacion estado,
        LocalDateTime fechaPostulacion,
        LocalDateTime fechaCambioEstado,
        List<RequisitoCubierto> requisitosCubiertos,
        String resumenProfesional,
        List<PerfilProfesionalResponse.Especialidad> especialidades,
        List<String> habilidades,
        List<PerfilProfesionalResponse.Experiencia> experiencias,
        List<PerfilProfesionalResponse.Formacion> formaciones,
        String motivoRetiro,
        Entrevista entrevista
) {

    public record RequisitoCubierto(String requisito, boolean requierePdf, Long idCertificacion, String certificacion,
                                    boolean tienePdf, boolean coincide) {
    }

    public record CertificacionAdjunta(Long idCertificacion, String nombre, String entidadEmisora, boolean tienePdf) {
    }
}
