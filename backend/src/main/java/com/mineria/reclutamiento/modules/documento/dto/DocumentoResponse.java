package com.mineria.reclutamiento.modules.documento.dto;

import com.mineria.reclutamiento.modules.documento.entity.Documento;
import java.time.LocalDateTime;

public record DocumentoResponse(
        String nombreOriginal,
        String tipoMime,
        long tamanoBytes,
        LocalDateTime fechaSubida
) {
    public static DocumentoResponse de(Documento d) {
        return d == null ? null
                : new DocumentoResponse(d.getNombreOriginal(), d.getTipoMime(), d.getTamanoBytes(), d.getFechaSubida());
    }
}
