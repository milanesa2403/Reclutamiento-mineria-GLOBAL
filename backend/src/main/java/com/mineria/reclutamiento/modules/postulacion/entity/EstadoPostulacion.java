package com.mineria.reclutamiento.modules.postulacion.entity;

public enum EstadoPostulacion {
    POSTULADO,
    EN_REVISION,
    ENTREVISTA,
    ACEPTADO,
    DESCARTADO,
    CONFIRMADO,
    RETIRADO;

    public boolean activo() {
        return this == POSTULADO || this == EN_REVISION || this == ENTREVISTA || this == ACEPTADO;
    }
}
