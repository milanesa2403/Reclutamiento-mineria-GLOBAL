package com.mineria.reclutamiento.servicio;

import com.mineria.reclutamiento.modelo.Mensaje;
import java.util.List;
import java.util.UUID;

public interface MensajeServicio {
    Mensaje enviar(String claveIdempotencia, UUID postulacionId, UUID remitenteId, String contenido);
    List<Mensaje> listar(UUID usuarioId, UUID postulacionId);
}
