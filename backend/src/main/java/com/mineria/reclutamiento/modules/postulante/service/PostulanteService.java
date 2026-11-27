package com.mineria.reclutamiento.modules.postulante.service;


import com.mineria.reclutamiento.core.exception.DuplicateResourceException;
import com.mineria.reclutamiento.core.exception.ResourceNotFoundException;
import com.mineria.reclutamiento.modules.auth.entity.Usuario;
import com.mineria.reclutamiento.modules.auth.repository.UsuarioRepository;
import com.mineria.reclutamiento.modules.persona.dto.IdentidadPersona;
import com.mineria.reclutamiento.modules.persona.entity.Persona;
import com.mineria.reclutamiento.modules.persona.repository.PersonaRepository;
import com.mineria.reclutamiento.modules.persona.service.PersonaService;
import com.mineria.reclutamiento.modules.postulante.dto.request.ActualizarPostulanteRequest;
import com.mineria.reclutamiento.modules.postulante.dto.request.PostulanteRequest;
import com.mineria.reclutamiento.modules.postulante.dto.response.PerfilPostulanteResponse;
import com.mineria.reclutamiento.modules.postulante.dto.response.PostulanteResponse;
import com.mineria.reclutamiento.modules.postulante.entity.Postulante;
import com.mineria.reclutamiento.modules.postulante.repository.PostulanteRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostulanteService {

    private final PostulanteRepository postulanteRepository;
    private final PersonaRepository personaRepository;
    private final PersonaService personaService;
    private final UsuarioRepository usuarioRepository;
    private final EstadoPerfilProfesional estadoPerfil;

    public PostulanteService(PostulanteRepository postulanteRepository,
                             PersonaRepository personaRepository,
                             PersonaService personaService,
                             UsuarioRepository usuarioRepository,
                             EstadoPerfilProfesional estadoPerfil) {
        this.postulanteRepository = postulanteRepository;
        this.personaRepository = personaRepository;
        this.personaService = personaService;
        this.usuarioRepository = usuarioRepository;
        this.estadoPerfil = estadoPerfil;
    }

    @Transactional
    public void registrarDatos(Long usuarioId, PostulanteRequest request, IdentidadPersona identidad) {
        Usuario usuarioEntity = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + usuarioId));

        Persona persona = new Persona();
        persona.setUsuario(usuarioEntity);
        persona.setPais(identidad.pais());
        persona.setTipoDocumento(identidad.tipoDocumento());
        persona.setDni(identidad.numeroDocumento());
        persona.setNombres(identidad.nombres());
        persona.setApellidos(identidad.apellidos());
        persona.setTelefono(request.telefono().trim());

        Persona personaGuardada = personaRepository.save(persona);

        Postulante postulante = new Postulante();
        postulante.setPersona(personaGuardada);
        postulante.setResumenProfesional(textoOpcional(request.resumenProfesional()));

        postulanteRepository.save(postulante);
    }

    @Transactional(readOnly = true)
    public PostulanteResponse obtenerPorId(Long idPostulante) {
        Postulante postulante = postulanteRepository.findById(idPostulante)
                .orElseThrow(() -> new ResourceNotFoundException("Postulante no encontrado con ID: " + idPostulante));
        return aResponse(postulante);
    }

    @Transactional(readOnly = true)
    public PostulanteResponse obtenerPorPersonaId(Long idPersona) {
        Postulante postulante = postulanteRepository.findByPersonaIdPersona(idPersona)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró perfil de postulante para la persona con ID: " + idPersona));
        return aResponse(postulante);
    }


    @Transactional(readOnly = true)
    public PerfilPostulanteResponse obtenerMiPerfil(String correo) {
        Postulante postulante = postulanteDe(correo);
        return aPerfil(postulante);
    }

    @Transactional
    public PerfilPostulanteResponse actualizarMiPerfil(String correo, ActualizarPostulanteRequest request) {
        Postulante postulante = postulanteDe(correo);
        Persona persona = postulante.getPersona();

        String telefono = request.telefono().trim();
        if (personaRepository.existsByTelefonoAndIdPersonaNot(telefono, persona.getIdPersona())) {
            throw new DuplicateResourceException("Ese teléfono ya está registrado por otra persona.");
        }

        persona.setTelefono(telefono);
        persona.setDireccion(textoOpcional(request.direccion()));
        postulante.setResumenProfesional(textoOpcional(request.resumenProfesional()));

        return aPerfil(postulante);
    }

    public Postulante postulanteDe(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        Persona persona = personaRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Tu cuenta no tiene datos personales registrados"));
        return postulanteRepository.findByPersonaIdPersona(persona.getIdPersona())
                .orElseThrow(() -> new ResourceNotFoundException("Tu cuenta no tiene un perfil de postulante"));
    }

    private PerfilPostulanteResponse aPerfil(Postulante postulante) {
        Persona persona = postulante.getPersona();
        List<String> pendientes = estadoPerfil.pendientes(postulante.getIdPostulante());
        return new PerfilPostulanteResponse(
                persona.getPais(),
                persona.getTipoDocumento() != null ? persona.getTipoDocumento().name() : null,
                persona.getDni(),
                persona.getNombres(),
                persona.getApellidos(),
                persona.getUsuario().getCorreo(),
                persona.getTelefono(),
                persona.getDireccion(),
                postulante.getResumenProfesional(),
                DocumentosPostulanteService.documentos(postulante, persona.getUsuario().getId()),
                pendientes.isEmpty(),
                pendientes);
    }

    private static String textoOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }

    public PostulanteResponse aResponse(Postulante postulante) {
        var persona = postulante.getPersona();
        return new PostulanteResponse(
                postulante.getIdPostulante(),
                persona != null ? persona.getIdPersona() : null,
                persona != null ? personaService.aResponse(persona) : null,
                postulante.getResumenProfesional(),
                postulante.getUrlCv(),
                postulante.getUrlFotoDni(),
                postulante.getUrlCul());
    }
}
