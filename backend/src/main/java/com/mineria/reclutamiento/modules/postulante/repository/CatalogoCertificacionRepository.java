package com.mineria.reclutamiento.modules.postulante.repository;

import com.mineria.reclutamiento.modules.postulante.entity.CatalogoCertificacion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogoCertificacionRepository extends JpaRepository<CatalogoCertificacion, Long> {

    List<CatalogoCertificacion> findAllByOrderByNombreAsc();
}
