package com.mineria.reclutamiento.modules.postulante.repository;

import com.mineria.reclutamiento.modules.postulante.entity.ExperienciaLaboral;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExperienciaLaboralRepository extends JpaRepository<ExperienciaLaboral, Long> {

    @Query("select x from ExperienciaLaboral x where x.postulante.idPostulante = ?1 order by x.fechaInicio desc, x.id")
    List<ExperienciaLaboral> delPostulante(Long idPostulante);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ExperienciaLaboral x where x.postulante.idPostulante = :idPostulante")
    int borrarDelPostulante(@Param("idPostulante") Long idPostulante);
}
