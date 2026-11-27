package com.mineria.reclutamiento.modules.postulacion.dto.response;

import java.util.List;

public record RequisitosPostulacionResponse(
        boolean perfilCompleto,
        boolean tieneCv,
        boolean tieneCertificaciones,
        boolean puedePostular,
        List<String> faltantes,
        List<CertificadoRequerido> certificadosRequeridos
) {

    public record CertificadoRequerido(Long idRequisito, String nombre, boolean requierePdf, boolean cumplible,
                                       List<Opcion> opciones) {
    }

    public record Opcion(Long idCertificacion, String nombre, boolean tienePdf, boolean coincide) {
    }
}
