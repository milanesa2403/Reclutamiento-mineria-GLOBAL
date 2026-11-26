package com.mineria.reclutamiento.modules.persona.repository;

import com.mineria.reclutamiento.modules.persona.entity.Persona;
import com.mineria.reclutamiento.modules.persona.entity.TipoDocumentoIdentidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PersonaRepository extends JpaRepository<Persona, Long> {


    boolean existsByPaisAndTipoDocumentoAndDni(String pais, TipoDocumentoIdentidad tipoDocumento, String dni);

    Optional<Persona> findByUsuarioId(Long idUsuario);

    boolean existsByUsuarioId(Long idUsuario);

    boolean existsByTelefono(String telefono);

    boolean existsByTelefonoAndIdPersonaNot(String telefono, Long idPersona);
}