package com.mineria.reclutamiento.modules.empresa.repository;

import com.mineria.reclutamiento.modules.empresa.entity.Empresa;
import com.mineria.reclutamiento.modules.empresa.entity.EstadoEmpresa;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    boolean existsByPaisAndRuc(String pais, String ruc);
    boolean existsByTelefono(String telefono);

    boolean existsByTelefonoAndIdNot(String telefono, Long id);


    @EntityGraph(attributePaths = "sectorMinero")
    List<Empresa> findAllByOrderByIdDesc();

    @EntityGraph(attributePaths = "sectorMinero")
    List<Empresa> findByEstadoOrderByIdDesc(EstadoEmpresa estado);

    List<Empresa> findByEstadoAndFechaRevisionBefore(EstadoEmpresa estado, LocalDateTime limite);

    List<Empresa> findByEstadoAndFechaRegistroBefore(EstadoEmpresa estado, LocalDateTime limite);

    @Query(value = "SELECT COUNT(*) FROM empresa WHERE fecha_registro >= :desde", nativeQuery = true)
    long contarRegistradasDesde(@Param("desde") LocalDateTime desde);

    @Query("select count(x) from Empresa x where x.estado in ?1 and x.fechaRegistro >= ?2")
    long contarDesde(Collection<EstadoEmpresa> estados, LocalDateTime desde);

    long countByEstado(EstadoEmpresa estado);

    long countByEstadoIn(Collection<EstadoEmpresa> estados);
}