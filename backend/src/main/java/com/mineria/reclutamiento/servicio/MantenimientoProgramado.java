package com.mineria.reclutamiento.servicio;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Tareas diarias: archivar ofertas cerradas hace 6 meses, borrar cuentas solicitadas y depurar auditoria. */
@Component
public class MantenimientoProgramado {
    private final OfertaServicio ofertas;
    private final PrivacidadServicio privacidad;

    public MantenimientoProgramado(OfertaServicio ofertas, PrivacidadServicio privacidad) {
        this.ofertas = ofertas;
        this.privacidad = privacidad;
    }

    @Scheduled(cron = "0 30 3 * * *", zone = "UTC")
    public void ejecutar() {
        ofertas.archivarAntiguas();
        privacidad.purgar();
    }
}
