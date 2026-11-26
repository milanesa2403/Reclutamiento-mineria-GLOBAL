package com.mineria.reclutamiento.modules.convocatoria.repository;

import com.mineria.reclutamiento.modules.convocatoria.entity.ConvocatoriaGuardada;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ConvocatoriaGuardadaRepository extends JpaRepository<ConvocatoriaGuardada, Long> {

    @Query("select x from ConvocatoriaGuardada x where x.postulante.idPostulante = ?1 and x.convocatoria.id = ?2")
    Optional<ConvocatoriaGuardada> buscar(Long idPostulante, Long idConvocatoria);

    @Query("select x from ConvocatoriaGuardada x where x.postulante.idPostulante = ?1 order by x.fechaGuardado desc")
    List<ConvocatoriaGuardada> delPostulante(Long idPostulante);
}
