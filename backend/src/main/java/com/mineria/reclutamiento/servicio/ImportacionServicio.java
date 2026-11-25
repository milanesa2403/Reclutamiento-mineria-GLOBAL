package com.mineria.reclutamiento.servicio;

import java.util.List;
import java.util.UUID;

/** RNF 30: importacion/exportacion CSV y JSON (max 5 MB) validando el 100% de los registros. */
public interface ImportacionServicio {
    int MAX_BYTES = 5 * 1024 * 1024;

    record ErrorFila(int fila, String motivo) {}
    record Reporte(int total, int importados, List<ErrorFila> errores) {}

    Reporte importarOfertas(UUID usuarioEmpresaId, byte[] archivo, String formato);
    Reporte importarPerfiles(UUID adminId, byte[] archivo, String formato);
    byte[] exportarOfertas(UUID usuarioEmpresaId, String formato);
    byte[] exportarPerfiles(UUID adminId, String formato);
}
