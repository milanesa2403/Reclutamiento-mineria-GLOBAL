package com.mineria.reclutamiento.modules.postulante.repository;

import com.mineria.reclutamiento.modules.postulante.entity.HabilidadPostulante;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HabilidadPostulanteRepository extends JpaRepository<HabilidadPostulante, Long> {

    @Query("select x from HabilidadPostulante x where x.postulante.idPostulante = ?1 order by x.id")
    List<HabilidadPostulante> delPostulante(Long idPostulante);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from HabilidadPostulante x where x.postulante.idPostulante = :idPostulante")
    int borrarDelPostulante(@Param("idPostulante") Long idPostulante);

    boolean existsByPostulante_IdPostulante(Long idPostulante);
}
