package com.mineria.reclutamiento.modules.postulante.repository;

import com.mineria.reclutamiento.modules.postulante.entity.FormacionAcademica;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FormacionAcademicaRepository extends JpaRepository<FormacionAcademica, Long> {

    @Query("select x from FormacionAcademica x where x.postulante.idPostulante = ?1 order by x.fechaInicio desc, x.id")
    List<FormacionAcademica> delPostulante(Long idPostulante);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from FormacionAcademica x where x.postulante.idPostulante = :idPostulante")
    int borrarDelPostulante(@Param("idPostulante") Long idPostulante);
}
