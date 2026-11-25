package com.mineria.reclutamiento.repositorio;

import com.mineria.reclutamiento.modelo.Perfil;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerfilRepositorio extends JpaRepository<Perfil, UUID> {
    List<Perfil> findByEspecialidadContainingIgnoreCaseAndAniosExperienciaGreaterThanEqual(String especialidad, int minAnios);
}
