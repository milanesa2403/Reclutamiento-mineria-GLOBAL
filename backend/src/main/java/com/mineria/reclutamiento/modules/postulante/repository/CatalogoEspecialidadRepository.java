package com.mineria.reclutamiento.modules.postulante.repository;

import com.mineria.reclutamiento.modules.postulante.entity.CatalogoEspecialidad;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogoEspecialidadRepository extends JpaRepository<CatalogoEspecialidad, Integer> {

    List<CatalogoEspecialidad> findAllByOrderByNombreAsc();
}
