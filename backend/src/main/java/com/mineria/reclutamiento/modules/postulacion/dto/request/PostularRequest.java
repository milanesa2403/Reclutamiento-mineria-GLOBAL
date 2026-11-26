package com.mineria.reclutamiento.modules.postulacion.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PostularRequest(
        @NotNull(message = "Indica la convocatoria") Long idConvocatoria,
        @Size(max = 20, message = "Puedes adjuntar como máximo 20 certificaciones") List<Long> idsCertificaciones,
        @Valid @Size(max = 20, message = "Máximo 20 requisitos") List<RequisitoCubierto> requisitos
) {

    public record RequisitoCubierto(
            @NotNull(message = "Indica el requisito") Long idRequisito,
            @NotNull(message = "Elige la certificación que lo cumple") Long idCertificacion
    ) {
    }
}
