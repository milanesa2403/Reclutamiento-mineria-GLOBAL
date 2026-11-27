package com.mineria.reclutamiento.modules.convocatoria.repository;

import com.mineria.reclutamiento.modules.convocatoria.entity.Convocatoria;
import com.mineria.reclutamiento.modules.convocatoria.entity.EstadoConvocatoria;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ConvocatoriaRepository extends JpaRepository<Convocatoria, Long>, JpaSpecificationExecutor<Convocatoria>  {

    @Query("select x from Convocatoria x where x.empresa.id = ?1 order by x.fechaCreacion desc")
    List<Convocatoria> deEmpresa(Long idEmpresa);

    @Query("select x from Convocatoria x where x.empresa.id = ?1 and x.estado = ?2 order by x.fechaCreacion desc")
    List<Convocatoria> deEmpresaEnEstado(Long idEmpresa, EstadoConvocatoria estado);

    long countByEstado(EstadoConvocatoria estado);

    List<Convocatoria> findByEstadoAndFechaCierreBefore(EstadoConvocatoria estado, LocalDate fecha);

    @Query("select x from Convocatoria x where x.empresa.id = ?1 and x.estado = ?2 and x.fechaCierre < ?3")
    List<Convocatoria> vencidasDeEmpresa(Long idEmpresa, EstadoConvocatoria estado,
                                                                     LocalDate fecha);
}
