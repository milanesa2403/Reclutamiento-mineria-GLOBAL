package com.mineria.reclutamiento.modules.empresa.repository;

import com.mineria.reclutamiento.modules.empresa.entity.EstadoEmpresa;
import com.mineria.reclutamiento.modules.empresa.entity.Representante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RepresentanteRepository extends JpaRepository<Representante, Long> {
    boolean existsByPersona_Dni(String dni);

    Optional<Representante> findByPersona_IdPersona(Long idPersona);

    List<Representante> findByEmpresa_Id(Long idEmpresa);

    @Query("select r from Representante r where r.persona.usuario.correo = ?1 and r.empresa.estado = ?2")
    List<Representante> porCorreoYEstado(String correo, EstadoEmpresa estado);

    Optional<Representante> findFirstByPersona_DniAndEmpresa_Estado(String dni, EstadoEmpresa estado);
}