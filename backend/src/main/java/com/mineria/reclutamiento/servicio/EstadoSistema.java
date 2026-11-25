package com.mineria.reclutamiento.servicio;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;

/** RNF 24: modo de solo lectura (la BD no acepta escrituras). Se activa desde el panel admin o por monitoreo. */
@Component
public class EstadoSistema {
    private final AtomicBoolean soloLectura = new AtomicBoolean(false);

    public boolean isSoloLectura() { return soloLectura.get(); }
    public void setSoloLectura(boolean v) { soloLectura.set(v); }
}
