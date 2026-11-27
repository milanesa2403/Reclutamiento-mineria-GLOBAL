package com.mineria.reclutamiento.modules.postulacion.repository;

import com.mineria.reclutamiento.modules.postulacion.entity.EstadoPostulacion;
import com.mineria.reclutamiento.modules.postulacion.entity.Postulacion;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface PostulacionRepository extends JpaRepository<Postulacion, Long> {

    @Query("select case when count(x) > 0 then true else false end from Postulacion x where x.convocatoria.id = ?1 and x.postulante.idPostulante = ?2")
    boolean existe(Long idConvocatoria, Long idPostulante);

    @Query("select x from Postulacion x where x.postulante.idPostulante = ?1 order by x.fechaPostulacion desc")
    List<Postulacion> delPostulante(Long idPostulante);

    @Query("select x from Postulacion x where x.convocatoria.id = ?1 order by x.fechaPostulacion")
    List<Postulacion> deConvocatoria(Long idConvocatoria);

    long countByConvocatoria_Id(Long idConvocatoria);

    long countByConvocatoria_IdAndEstadoIn(Long idConvocatoria, Collection<EstadoPostulacion> estados);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Postulacion p set p.estado = :retirado, p.motivoRetiro = 'No confirmó a tiempo', "
            + "p.fechaCambioEstado = :ahora where p.estado = :aceptado and p.fechaAceptacion < :limite")
    int retirarSinConfirmar(@Param("aceptado") EstadoPostulacion aceptado, @Param("retirado") EstadoPostulacion retirado,
                            @Param("limite") LocalDateTime limite, @Param("ahora") LocalDateTime ahora);
}
