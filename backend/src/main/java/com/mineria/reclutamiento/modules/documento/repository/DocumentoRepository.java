package com.mineria.reclutamiento.modules.documento.repository;

import com.mineria.reclutamiento.modules.documento.entity.Documento;
import com.mineria.reclutamiento.modules.documento.entity.TipoDocumento;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DocumentoRepository extends JpaRepository<Documento, Long> {

    List<Documento> findByPropietario_Id(Long idUsuario);

    @Query("select x from Documento x where x.propietario.id in ?1 and x.tipo = ?2 order by x.fechaSubida desc, x.id desc")
    List<Documento> dePropietarios(Collection<Long> idsUsuarios,
                                                                                TipoDocumento tipo);

    default Optional<Documento> vigente(Collection<Long> idsUsuarios, TipoDocumento tipo) {
        return dePropietarios(idsUsuarios, tipo).stream().findFirst();
    }
}
