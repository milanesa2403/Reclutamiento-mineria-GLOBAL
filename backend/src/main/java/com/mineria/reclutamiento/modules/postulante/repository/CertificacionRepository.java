package com.mineria.reclutamiento.modules.postulante.repository;

import com.mineria.reclutamiento.modules.postulacion.entity.EstadoPostulacion;
import com.mineria.reclutamiento.modules.postulante.entity.Certificacion;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CertificacionRepository extends JpaRepository<Certificacion, Long> {

    @Query("select x from Certificacion x where x.postulante.idPostulante = ?1 order by x.id")
    List<Certificacion> delPostulante(Long idPostulante);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select x from Certificacion x where x.id = :id and x.postulante.idPostulante = :idPostulante")
    Optional<Certificacion> bloquearDelPostulante(@Param("id") Long id, @Param("idPostulante") Long idPostulante);

    Optional<Certificacion> findByIdAndPostulante_IdPostulante(Long id, Long idPostulante);

    List<Certificacion> findByIdInAndPostulante_IdPostulante(Collection<Long> ids, Long idPostulante);

    @Query("select distinct p.convocatoria.puesto from Postulacion p join p.certificaciones c "
            + "where c.id in :ids and p.estado in :estados")
    List<String> puestosQueLasUsan(@Param("ids") Collection<Long> ids,
                                   @Param("estados") Collection<EstadoPostulacion> estados);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Certificacion x where x.postulante.idPostulante = :idPostulante")
    int borrarDelPostulante(@Param("idPostulante") Long idPostulante);
}
