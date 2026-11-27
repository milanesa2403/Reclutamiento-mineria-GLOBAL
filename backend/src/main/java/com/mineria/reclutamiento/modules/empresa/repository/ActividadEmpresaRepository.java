package com.mineria.reclutamiento.modules.empresa.repository;

import com.mineria.reclutamiento.modules.empresa.entity.ActividadEmpresa;
import com.mineria.reclutamiento.modules.empresa.entity.TipoActividadEmpresa;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActividadEmpresaRepository extends JpaRepository<ActividadEmpresa, Long> {

    List<ActividadEmpresa> findAllByOrderByFechaDescIdDesc(Pageable pageable);

    List<ActividadEmpresa> findByIdEmpresaOrderByFechaDescIdDesc(Long idEmpresa);

    long countByIdEmpresaAndTipo(Long idEmpresa, TipoActividadEmpresa tipo);
}