package com.mineria.reclutamiento.repositorio;

import com.mineria.reclutamiento.modelo.EstadoOferta;
import com.mineria.reclutamiento.modelo.Oferta;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OfertaRepositorio extends JpaRepository<Oferta, UUID> {
    @Query("select o from Oferta o where o.estado = com.mineria.reclutamiento.modelo.EstadoOferta.ABIERTA "
            + "and lower(coalesce(o.especialidadRequerida,'')) like lower(concat('%', :esp, '%')) "
            + "and (:pais = '' or o.pais = :pais) order by o.fechaPublicacion desc")
    List<Oferta> buscar(@Param("esp") String especialidad, @Param("pais") String pais, Pageable pageable);

    List<Oferta> findByEmpresaIdOrderByFechaPublicacionDesc(UUID empresaId);
    List<Oferta> findByEmpresaId(UUID empresaId);
    List<Oferta> findByEstadoAndFechaCierreBefore(EstadoOferta estado, Instant limite);
}
