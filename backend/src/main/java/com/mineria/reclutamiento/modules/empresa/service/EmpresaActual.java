package com.mineria.reclutamiento.modules.empresa.service;

import com.mineria.reclutamiento.core.exception.ForbiddenException;
import com.mineria.reclutamiento.core.exception.ResourceNotFoundException;
import com.mineria.reclutamiento.modules.auth.entity.Usuario;
import com.mineria.reclutamiento.modules.auth.repository.UsuarioRepository;
import com.mineria.reclutamiento.modules.empresa.entity.Empresa;
import com.mineria.reclutamiento.modules.empresa.entity.Representante;
import com.mineria.reclutamiento.modules.empresa.repository.RepresentanteRepository;
import com.mineria.reclutamiento.modules.persona.entity.Persona;
import com.mineria.reclutamiento.modules.persona.repository.PersonaRepository;
import org.springframework.stereotype.Component;

@Component
public class EmpresaActual {

    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final RepresentanteRepository representanteRepository;

    public EmpresaActual(UsuarioRepository usuarioRepository, PersonaRepository personaRepository,
                         RepresentanteRepository representanteRepository) {
        this.usuarioRepository = usuarioRepository;
        this.personaRepository = personaRepository;
        this.representanteRepository = representanteRepository;
    }

    public record Contexto(Usuario usuario, Empresa empresa) {
    }

    public Contexto obtener(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        Persona persona = personaRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Tu cuenta no tiene datos personales registrados"));
        Representante representante = representanteRepository.findByPersona_IdPersona(persona.getIdPersona())
                .orElseThrow(() -> new ResourceNotFoundException("Tu cuenta no está vinculada a una empresa"));
        return new Contexto(usuario, representante.getEmpresa());
    }

    public Contexto obtenerAprobada(String correo) {
        Contexto ctx = obtener(correo);
        if (!ctx.empresa().estaAprobada()) {
            throw new ForbiddenException(
                    "Tu empresa aún no está aprobada: podrás gestionar convocatorias cuando el administrador apruebe tu solicitud.");
        }
        return ctx;
    }
}
