package com.mineria.reclutamiento.modules.persona.service;

import com.mineria.reclutamiento.modules.auth.entity.Usuario;
import com.mineria.reclutamiento.modules.persona.dto.IdentidadPersona;
import com.mineria.reclutamiento.modules.persona.dto.response.PersonaResponse;
import com.mineria.reclutamiento.modules.persona.entity.Persona;
import com.mineria.reclutamiento.modules.persona.repository.PersonaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mineria.reclutamiento.core.exception.BadRequestException;
import com.mineria.reclutamiento.core.exception.DuplicateResourceException;

@Service
public class PersonaService {

    private final PersonaRepository personaRepository;
    public PersonaService(PersonaRepository personaRepository) {
        this.personaRepository = personaRepository;
    }

    @Transactional(readOnly = true)
    public Persona obtenerPorUsuarioId(Long idUsuario) {
        return personaRepository.findByUsuarioId(idUsuario)
                .orElseThrow(() ->
                        new BadRequestException(
                                "Persona no encontrada para el usuario"));
    }

    @Transactional
    public Persona crearPersonaOficial(IdentidadPersona identidad, String telefono, String direccion, Usuario usuario) {
        if (existeDocumento(identidad)) {
            throw new DuplicateResourceException("Este documento ya está asociado a otra cuenta");
        }

        Persona nuevaPersona = new Persona();
        nuevaPersona.setUsuario(usuario);
        nuevaPersona.setPais(identidad.pais());
        nuevaPersona.setTipoDocumento(identidad.tipoDocumento());
        nuevaPersona.setDni(identidad.numeroDocumento());
        nuevaPersona.setNombres(identidad.nombres());
        nuevaPersona.setApellidos(identidad.apellidos());
        nuevaPersona.setTelefono(telefono == null ? null : telefono.trim());
        nuevaPersona.setDireccion(direccion);

        return personaRepository.save(nuevaPersona);
    }

    public PersonaResponse aResponse(Persona persona) {
        return new PersonaResponse(
                persona.getIdPersona(),
                persona.getUsuario() != null ? persona.getUsuario().getId() : null,
                persona.getPais(),
                persona.getTipoDocumento() != null ? persona.getTipoDocumento().name() : null,
                persona.getDni(),
                persona.getNombres(),
                persona.getApellidos(),
                persona.getTelefono(),
                persona.getDireccion());
    }

    @Transactional(readOnly = true)
    public boolean existeDocumento(IdentidadPersona identidad) {
        return personaRepository.existsByPaisAndTipoDocumentoAndDni(
                identidad.pais(), identidad.tipoDocumento(), identidad.numeroDocumento());
    }

    @Transactional(readOnly = true)
    public boolean telefonoExistente(String telefono, Long idPersonaPropia) {
        return personaRepository.existsByTelefonoAndIdPersonaNot(telefono, idPersonaPropia);
    }
}
