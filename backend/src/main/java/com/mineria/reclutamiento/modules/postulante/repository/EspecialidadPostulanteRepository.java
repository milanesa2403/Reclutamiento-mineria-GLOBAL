package com.mineria.reclutamiento.modules.postulante.repository;

import com.mineria.reclutamiento.modules.postulante.entity.EspecialidadPostulante;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EspecialidadPostulanteRepository extends JpaRepository<EspecialidadPostulante, Long> {

    @Query("select x from EspecialidadPostulante x where x.postulante.idPostulante = ?1 order by x.esPrincipal desc, x.id")
    List<EspecialidadPostulante> delPostulante(Long idPostulante);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from EspecialidadPostulante x where x.postulante.idPostulante = :idPostulante")
    int borrarDelPostulante(@Param("idPostulante") Long idPostulante);

    @Query("select case when count(x) > 0 then true else false end from EspecialidadPostulante x where x.postulante.idPostulante = ?1 and x.esPrincipal = true")
    boolean tienePrincipal(Long idPostulante);
}
