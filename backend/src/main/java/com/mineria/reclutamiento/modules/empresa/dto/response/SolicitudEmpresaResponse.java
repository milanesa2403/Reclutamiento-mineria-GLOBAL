package com.mineria.reclutamiento.modules.empresa.dto.response;

import com.mineria.reclutamiento.modules.documento.dto.DocumentoResponse;
import com.mineria.reclutamiento.modules.empresa.entity.EstadoEmpresa;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SolicitudEmpresaResponse(
        String pais,
        String ruc,
        String razonSocial,
        EstadoEmpresa estado,
        String motivoRevision,
        LocalDateTime fechaRevision,
        DocumentoResponse documentoAcreditacion,
        boolean puedeSubirDocumento,
        LocalDate disponibleDesde
) {
}
