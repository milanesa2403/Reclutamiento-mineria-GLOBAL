package com.mineria.reclutamiento.repositorio;

import com.mineria.reclutamiento.modelo.Mensaje;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MensajeRepositorio extends JpaRepository<Mensaje, UUID> {
    List<Mensaje> findByPostulacionIdOrderByFechaAsc(UUID postulacionId);
    void deleteByPostulacionId(UUID postulacionId);
    void deleteByRemitenteId(UUID remitenteId);
}
