package com.mineria.reclutamiento.modules.persona.dto;

import com.mineria.reclutamiento.modules.persona.entity.TipoDocumentoIdentidad;

public record IdentidadPersona(
        String pais,
        TipoDocumentoIdentidad tipoDocumento,
        String numeroDocumento,
        String nombres,
        String apellidos
) {
}
