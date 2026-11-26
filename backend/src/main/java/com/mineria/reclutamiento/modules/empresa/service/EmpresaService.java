package com.mineria.reclutamiento.modules.empresa.service;

import com.mineria.reclutamiento.core.validacion.DocumentoIdentidad;
import com.mineria.reclutamiento.modules.persona.dto.IdentidadPersona;
import com.mineria.reclutamiento.core.exception.BadRequestException;
import com.mineria.reclutamiento.core.exception.DuplicateResourceException;
import com.mineria.reclutamiento.core.exception.ForbiddenException;
import com.mineria.reclutamiento.core.exception.ResourceNotFoundException;
import com.mineria.reclutamiento.modules.auth.entity.Usuario;
import com.mineria.reclutamiento.modules.auth.repository.UsuarioRepository;
import com.mineria.reclutamiento.modules.empresa.dto.request.EmpresaPerfilUpdateRequest;
import com.mineria.reclutamiento.modules.empresa.dto.request.EmpresaRegistroRequest;
import com.mineria.reclutamiento.modules.empresa.dto.response.EmpresaPerfilResponse;
import com.mineria.reclutamiento.modules.empresa.entity.Empresa;
import com.mineria.reclutamiento.modules.empresa.entity.Representante;
import com.mineria.reclutamiento.modules.empresa.entity.SectorMinero;
import com.mineria.reclutamiento.modules.empresa.repository.EmpresaRepository;
import com.mineria.reclutamiento.modules.empresa.repository.RepresentanteRepository;
import com.mineria.reclutamiento.modules.persona.entity.Persona;
import com.mineria.reclutamiento.modules.persona.service.PersonaService;
import com.mineria.reclutamiento.modules.empresa.entity.TipoActividadEmpresa;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmpresaService {

    private static final int MAX_RAZON_SOCIAL = 200;

    private final EmpresaRepository empresaRepository;
    private final RepresentanteRepository representanteEmpresaRepository;
    private final SectorMineroService sectorMineroService;
    private final PersonaService personaService;
    private final UsuarioRepository usuarioRepository;
    private final ActividadEmpresaService actividad;


    public EmpresaService(
            EmpresaRepository empresaRepository,
            RepresentanteRepository representanteRepository,
            SectorMineroService sectorMineroService,
            PersonaService personaService,
            UsuarioRepository usuarioRepository,
            ActividadEmpresaService actividad) {

        this.empresaRepository = empresaRepository;
        this.representanteEmpresaRepository = representanteRepository;
        this.sectorMineroService = sectorMineroService;
        this.personaService = personaService;
        this.usuarioRepository = usuarioRepository;
        this.actividad = actividad;
    }

    @Transactional
    public void registrarDatos(
            Long idUsuario,
            String correo,
            EmpresaRegistroRequest request,
            IdentidadPersona identidadRepresentante) {

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Usuario no encontrado con ID: " + idUsuario));

        String pais = DocumentoIdentidad.normalizar(request.pais());
        String identificador = DocumentoIdentidad.normalizar(request.identificadorFiscal());
        String razonSocial = request.razonSocial().trim();

        if (existeIdentificadorFiscal(pais, identificador)) {
            throw new DuplicateResourceException(
                    "Ya existe una empresa registrada con ese identificador fiscal");
        }

        if (razonSocial.length() > MAX_RAZON_SOCIAL) {
            throw new BadRequestException("La razón social excede el largo admitido por el sistema");
        }

        SectorMinero sectorMinero =
                sectorMineroService.obtenerActivo(request.idSectorMinero());

        String telefono = request.telefono() == null || request.telefono().isBlank()
                ? null
                : request.telefono().trim();

        String direccion = textoOpcional(request.direccion());
        if (direccion != null && direccion.length() > 255) {
            direccion = direccion.substring(0, 255);
        }

        Empresa empresa = new Empresa(
                identificador,
                razonSocial,
                direccion,
                telefono,
                correo,
                sectorMinero
        );

        empresa.setPais(pais);
        empresaRepository.save(empresa);
        actividad.registrar(empresa, TipoActividadEmpresa.REGISTRADA, null, null);


        Persona representante = personaService.crearPersonaOficial(
                identidadRepresentante,
                request.telefonoRepresentante(),
                request.direccionRepresentante(),
                usuario
        );

        Representante representanteEmpresa = new Representante();
        representanteEmpresa.setEmpresa(empresa);
        representanteEmpresa.setPersona(representante);
        representanteEmpresa.setCargo(request.cargoRepresentante());

        representanteEmpresaRepository.save(representanteEmpresa);
    }

    public boolean existeIdentificadorFiscal(String pais, String identificador) {
        return empresaRepository.existsByPaisAndRuc(pais, identificador);
    }

    public boolean existeDocumentoRepresentante(IdentidadPersona identidad) {
        return personaService.existeDocumento(identidad);
    }

    @Transactional(readOnly = true)
    public EmpresaPerfilResponse perfilDe(String correo) {

        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Usuario no encontrado con correo: " + correo));

        Persona persona = personaService.obtenerPorUsuarioId(
                usuario.getId()
        );

        Representante representante =
                representanteEmpresaRepository
                        .findByPersona_IdPersona(persona.getIdPersona())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Representante no encontrado para el usuario"));

        Empresa empresa = representante.getEmpresa();

        var sector = empresa.getSectorMinero();
        Persona rep = representante.getPersona();

        return new EmpresaPerfilResponse(
                empresa.getId(),
                empresa.getPais(),
                empresa.getRuc(),
                empresa.getRazonSocial(),
                empresa.getDireccion(),
                empresa.getTelefono(),
                empresa.getCorreoCorporativo(),
                sector != null ? sector.getNombre() : null,
                rep.getTipoDocumento() != null ? rep.getTipoDocumento().name() : null,
                rep.getDni(),
                rep.getNombres(),
                rep.getApellidos(),
                rep.getTelefono(),
                rep.getDireccion(),
                representante.getCargo(),
                sector != null ? sector.getId() : null,
                empresa.getDescripcion(),
                null,
                null,
                empresa.getEstado() != null ? empresa.getEstado().name() : null,
                empresa.getMotivoRevision());
    }

    @Transactional
    public EmpresaPerfilResponse actualizarPerfil(
            String correo,
            EmpresaPerfilUpdateRequest request) {

        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() ->
                        new BadRequestException("Usuario no encontrado"));

        Persona persona = personaService.obtenerPorUsuarioId(
                usuario.getId()
        );

        Representante representante =
                representanteEmpresaRepository
                        .findByPersona_IdPersona(persona.getIdPersona())
                        .orElseThrow(() ->
                                new BadRequestException(
                                        "Representante no encontrado"));

        Empresa empresa = representante.getEmpresa();

        if (!empresa.estaAprobada()) {
            throw new ForbiddenException(
                    "Tu empresa aún no está aprobada: podrás editar el perfil cuando el administrador apruebe tu solicitud.");
        }

        if (request.idSectorMinero() != null) {
            empresa.setSectorMinero(sectorMineroService.obtenerActivo(request.idSectorMinero()));
        }
        if (request.telefono() != null) {
            String telefono = textoOpcional(request.telefono());
            if (telefono != null && empresaRepository.existsByTelefonoAndIdNot(telefono, empresa.getId())) {
                throw new DuplicateResourceException("Ese teléfono ya está registrado por otra empresa");
            }
            empresa.setTelefono(telefono);
        }
        if (request.descripcion() != null) {
            empresa.setDescripcion(textoOpcional(request.descripcion()));
        }

        if (request.telefonoRepresentante() != null) {
            String telefonoRep = request.telefonoRepresentante().trim();
            if (personaService.telefonoExistente(telefonoRep, persona.getIdPersona())) {
                throw new DuplicateResourceException("El teléfono del representante ya está asociado a otra persona");
            }
            persona.setTelefono(telefonoRep);
        }
        if (request.direccionRepresentante() != null) {
            persona.setDireccion(textoOpcional(request.direccionRepresentante()));
        }
        if (request.cargoRepresentante() != null) {
            representante.setCargo(request.cargoRepresentante().trim());
        }

        empresaRepository.save(empresa);

        return perfilDe(correo);
    }

    private static String textoOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }
}
