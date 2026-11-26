package com.mineria.reclutamiento.modules.admin.dto;

import com.mineria.reclutamiento.modules.empresa.entity.EstadoEmpresa;
import java.time.LocalDateTime;

public record EmpresaAdminItemResponse(
        Long idEmpresa,
        String pais,
        String ruc,
        String razonSocial,
        String sectorMinero,
        String correoCorporativo,
        LocalDateTime fechaRegistro,
        EstadoEmpresa estado,
        boolean tieneDocumento
) {
}
