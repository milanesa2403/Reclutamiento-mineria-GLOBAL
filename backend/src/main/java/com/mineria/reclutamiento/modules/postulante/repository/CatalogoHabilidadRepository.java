package com.mineria.reclutamiento.modules.postulante.repository;

import com.mineria.reclutamiento.modules.postulante.entity.CatalogoHabilidad;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogoHabilidadRepository extends JpaRepository<CatalogoHabilidad, Integer> {

    List<CatalogoHabilidad> findAllByOrderByCategoriaAscNombreAsc();
}
