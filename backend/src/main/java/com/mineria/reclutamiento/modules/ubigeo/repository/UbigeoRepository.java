package com.mineria.reclutamiento.modules.ubigeo.repository;

import com.mineria.reclutamiento.modules.ubigeo.dto.LugarResponse;
import com.mineria.reclutamiento.modules.ubigeo.entity.Ubigeo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UbigeoRepository extends JpaRepository<Ubigeo, String> {

    @Query("select distinct new com.mineria.reclutamiento.modules.ubigeo.dto.LugarResponse(u.codDepartamento, u.departamento) "
            + "from Ubigeo u order by u.departamento")
    List<LugarResponse> departamentos();

    @Query("select distinct new com.mineria.reclutamiento.modules.ubigeo.dto.LugarResponse(u.codProvincia, u.provincia) "
            + "from Ubigeo u where u.codDepartamento = :codDepartamento order by u.provincia")
    List<LugarResponse> provincias(@Param("codDepartamento") String codDepartamento);

    @Query("select new com.mineria.reclutamiento.modules.ubigeo.dto.LugarResponse(u.codigo, u.distrito) "
            + "from Ubigeo u where u.codProvincia = :codProvincia order by u.distrito")
    List<LugarResponse> distritos(@Param("codProvincia") String codProvincia);
}
