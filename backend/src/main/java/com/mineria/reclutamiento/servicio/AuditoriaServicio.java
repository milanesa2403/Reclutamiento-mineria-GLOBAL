package com.mineria.reclutamiento.servicio;

import com.mineria.reclutamiento.modelo.RegistroAuditoria;
import java.util.List;
import java.util.UUID;

public interface AuditoriaServicio {
    void registrar(UUID usuarioId, String accion, String recurso, boolean permitido);
    List<RegistroAuditoria> recientes();
}
