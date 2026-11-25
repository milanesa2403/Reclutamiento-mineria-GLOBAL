package com.mineria.reclutamiento.servicio;

import java.util.UUID;

/** RNF 12: Ley 29733 y RGPD (consentimiento, derecho de supresion). */
public interface PrivacidadServicio {
    void solicitarBorrado(UUID usuarioId);
    /** Borra cuentas cuya solicitud supero el periodo de gracia y depura auditoria con mas de 90 dias. */
    int purgar();
}
