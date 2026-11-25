package com.mineria.reclutamiento.servicio;

import com.mineria.reclutamiento.modelo.EstadoPostulacion;
import com.mineria.reclutamiento.modelo.Postulacion;
import java.util.List;
import java.util.UUID;

public interface PostulacionServicio {
    /** Idempotente: la misma clave (o la misma oferta para el mismo trabajador) devuelve la postulacion ya creada. */
    Postulacion postular(String claveIdempotencia, UUID ofertaId, UUID trabajadorId, UUID cvId);
    Postulacion cambiarEstado(UUID usuarioEmpresaId, String claveIdempotencia, UUID postulacionId, EstadoPostulacion nuevo);
    List<Postulacion> listarPorOferta(UUID usuarioEmpresaId, UUID ofertaId);
    List<Postulacion> listarDeTrabajador(UUID trabajadorId);
}
