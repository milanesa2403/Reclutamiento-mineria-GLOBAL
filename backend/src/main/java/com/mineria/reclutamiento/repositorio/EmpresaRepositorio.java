package com.mineria.reclutamiento.repositorio;

import com.mineria.reclutamiento.modelo.Empresa;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaRepositorio extends JpaRepository<Empresa, UUID> {
    Optional<Empresa> findByUsuarioId(UUID usuarioId);
}
