package com.mineria.reclutamiento.repositorio;

import com.mineria.reclutamiento.modelo.Postulacion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostulacionRepositorio extends JpaRepository<Postulacion, UUID> {
    List<Postulacion> findByOfertaId(UUID ofertaId);
    List<Postulacion> findByTrabajadorId(UUID trabajadorId);
    Optional<Postulacion> findByOfertaIdAndTrabajadorId(UUID ofertaId, UUID trabajadorId);

    /** True si la empresa tiene una oferta a la que el documento (CV) fue enviado. */
    @Query("select count(p) from Postulacion p, Oferta o where p.documentoCvId = :doc "
            + "and p.ofertaId = o.id and o.empresaId = :empresa")
    long contarEnviosDeDocumentoAEmpresa(@Param("doc") UUID documentoId, @Param("empresa") UUID empresaId);

    default boolean empresaPuedeVerDocumento(UUID documentoId, UUID empresaId) {
        return contarEnviosDeDocumentoAEmpresa(documentoId, empresaId) > 0;
    }
}
