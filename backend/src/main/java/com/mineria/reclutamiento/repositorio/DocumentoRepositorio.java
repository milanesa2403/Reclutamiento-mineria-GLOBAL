package com.mineria.reclutamiento.repositorio;

import com.mineria.reclutamiento.modelo.Documento;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentoRepositorio extends JpaRepository<Documento, UUID> {
    boolean existsBySha256(String sha256);
    List<Documento> findByPropietarioId(UUID propietarioId);
}
