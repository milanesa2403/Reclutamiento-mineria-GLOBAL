package com.mineria.reclutamiento.modules.empresa.entity;

public enum TipoActividadEmpresa {
    REGISTRADA,
    DOCUMENTO_ENVIADO,
    APROBADA,
    OBSERVADA,
    RECHAZADA,
    ADVERTENCIA,
    SUSPENDIDA,
    REACTIVADA,
    BLOQUEADA;

    public static TipoActividadEmpresa deDecision(EstadoEmpresa estado) {
        return switch (estado) {
            case APROBADA -> APROBADA;
            case OBSERVADA -> OBSERVADA;
            case RECHAZADA -> RECHAZADA;
            case SUSPENDIDA -> SUSPENDIDA;
            case BLOQUEADA -> BLOQUEADA;
            default -> throw new IllegalArgumentException(
                    "El estado " + estado + " no es una decisión del administrador");
        };
    }
}