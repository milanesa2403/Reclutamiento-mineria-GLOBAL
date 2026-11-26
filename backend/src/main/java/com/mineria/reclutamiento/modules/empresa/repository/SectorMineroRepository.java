package com.mineria.reclutamiento.modules.empresa.repository;

import com.mineria.reclutamiento.modules.empresa.entity.SectorMinero;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SectorMineroRepository extends JpaRepository<SectorMinero, Long> {

    List<SectorMinero> findByActivoTrueOrderByIdAsc();
}
