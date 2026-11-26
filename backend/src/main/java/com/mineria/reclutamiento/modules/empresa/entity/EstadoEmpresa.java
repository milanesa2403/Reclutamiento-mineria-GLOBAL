package com.mineria.reclutamiento.modules.empresa.entity;
import java.util.List;

public enum EstadoEmpresa {
    PENDIENTE_DOCUMENTO,
    EN_REVISION,
    OBSERVADA,
    APROBADA,
    SUSPENDIDA,
    BLOQUEADA,
    RECHAZADA;

    public static final List<EstadoEmpresa> REGISTRADAS = List.of(APROBADA, SUSPENDIDA, BLOQUEADA);

}