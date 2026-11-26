package com.mineria.reclutamiento.modules.empresa.repository;

import com.mineria.reclutamiento.modules.empresa.entity.EmpresaRechazoHistorial;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistorialRechazoRepository extends JpaRepository<EmpresaRechazoHistorial, Long> {

    List<EmpresaRechazoHistorial> findByRucOrderByFechaRechazoDesc(String ruc);
}
