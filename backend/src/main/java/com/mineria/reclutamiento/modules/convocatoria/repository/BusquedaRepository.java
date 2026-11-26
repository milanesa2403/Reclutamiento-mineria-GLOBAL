package com.mineria.reclutamiento.modules.convocatoria.repository;

import com.mineria.reclutamiento.modules.convocatoria.entity.Busqueda;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BusquedaRepository extends JpaRepository<Busqueda, Long> {

    @Query("select x from Busqueda x where x.empresa.id = ?1 order by x.fechaCreacion desc")
    List<Busqueda> deEmpresa(Long idEmpresa);

    @Query("select case when count(x) > 0 then true else false end from Busqueda x where x.empresa.id = ?1 and lower(x.nombre) = lower(?2)")
    boolean nombreUsado(Long idEmpresa, String nombre);

    @Query("select case when count(x) > 0 then true else false end from Busqueda x where x.empresa.id = ?1 and lower(x.nombre) = lower(?2) and x.id <> ?3")
    boolean nombreUsadoEnOtra(Long idEmpresa, String nombre, Long id);
}
