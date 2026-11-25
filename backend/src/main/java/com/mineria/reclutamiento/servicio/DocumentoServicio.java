package com.mineria.reclutamiento.servicio;

import com.mineria.reclutamiento.modelo.Documento;
import com.mineria.reclutamiento.modelo.Rol;
import com.mineria.reclutamiento.modelo.TipoDocumento;
import java.util.List;
import java.util.UUID;

public interface DocumentoServicio {
    int MAX_BYTES = 10 * 1024 * 1024; // 10 MB

    record Descarga(String nombreArchivo, byte[] contenido) {}

    Documento subir(UUID propietarioId, TipoDocumento tipo, String nombre, byte[] contenido);
    Descarga descargar(UUID solicitanteId, Rol rol, UUID documentoId);
    List<Documento> listar(UUID propietarioId);
    String calcularSha256(byte[] contenido);
    boolean existeDuplicado(String sha256);
}
