package com.mineria.reclutamiento.modules.postulante.repository;

import com.mineria.reclutamiento.modules.postulante.entity.Postulante;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;

import java.util.Optional;

public interface PostulanteRepository extends JpaRepository<Postulante, Long> {

    Optional<Postulante> findByPersonaIdPersona(Long idPersona);

    boolean existsByPersonaIdPersona(Long idPersona);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Postulante p where p.idPostulante = :id")
    Optional<Postulante> bloquear(@Param("id") Long id);

    @Query(value = """
            SELECT COUNT(*) FROM postulante p
            JOIN persona pe ON pe.id_persona = p.id_persona
            JOIN usuario u ON u.id_usuario = pe.id_usuario
            WHERE u.fecha_registro >= :desde
            """, nativeQuery = true)
    long contarRegistradosDesde(@Param("desde") LocalDateTime desde);

}
