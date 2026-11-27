package com.mineria.reclutamiento.modules.empresa.service;

import com.mineria.reclutamiento.core.exception.BadRequestException;
import com.mineria.reclutamiento.core.exception.ResourceNotFoundException;
import com.mineria.reclutamiento.modules.auth.entity.Usuario;
import com.mineria.reclutamiento.modules.auth.repository.UsuarioRepository;
import com.mineria.reclutamiento.modules.documento.dto.DocumentoResponse;
import com.mineria.reclutamiento.modules.documento.dto.UrlTemporalResponse;
import com.mineria.reclutamiento.modules.documento.entity.Documento;
import com.mineria.reclutamiento.modules.documento.entity.TipoDocumento;
import com.mineria.reclutamiento.modules.documento.repository.DocumentoRepository;
import com.mineria.reclutamiento.modules.documento.service.DocumentoService;
import com.mineria.reclutamiento.core.config.AwsProperties;
import com.mineria.reclutamiento.modules.empresa.dto.response.SolicitudEmpresaResponse;
import com.mineria.reclutamiento.modules.empresa.entity.Empresa;
import com.mineria.reclutamiento.modules.empresa.entity.EstadoEmpresa;
import com.mineria.reclutamiento.modules.empresa.entity.Representante;
import com.mineria.reclutamiento.modules.empresa.repository.EmpresaRepository;
import com.mineria.reclutamiento.modules.empresa.repository.RepresentanteRepository;
import com.mineria.reclutamiento.modules.persona.entity.Persona;
import com.mineria.reclutamiento.modules.persona.repository.PersonaRepository;
import com.mineria.reclutamiento.modules.empresa.entity.TipoActividadEmpresa;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SolicitudEmpresaService {

    static final Set<String> FORMATOS_ACREDITACION =
            Set.of(DocumentoService.PDF, DocumentoService.JPEG, DocumentoService.PNG);

    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final RepresentanteRepository representanteRepository;
    private final EmpresaRepository empresaRepository;
    private final DocumentoRepository documentoRepository;
    private final DocumentoService documentoService;
    private final AwsProperties storageProps;
    private final TransactionTemplate transaccion;
    private final TransactionTemplate lectura;
    private final ActividadEmpresaService actividad;

    public SolicitudEmpresaService(UsuarioRepository usuarioRepository, PersonaRepository personaRepository,
                                   RepresentanteRepository representanteRepository, EmpresaRepository empresaRepository,
                                   DocumentoRepository documentoRepository, DocumentoService documentoService,
                                   AwsProperties storageProps, PlatformTransactionManager transactionManager,
                                   ActividadEmpresaService actividad) {
        this.usuarioRepository = usuarioRepository;
        this.personaRepository = personaRepository;
        this.representanteRepository = representanteRepository;
        this.empresaRepository = empresaRepository;
        this.documentoRepository = documentoRepository;
        this.documentoService = documentoService;
        this.storageProps = storageProps;
        this.transaccion = new TransactionTemplate(transactionManager);
        this.lectura = new TransactionTemplate(transactionManager);
        this.lectura.setReadOnly(true);
        this.actividad = actividad;
    }

    public SolicitudEmpresaResponse obtenerMiSolicitud(String correo) {
        return lectura.execute(status -> mapear(buscarContexto(correo).empresa()));
    }

    public SolicitudEmpresaResponse subirAcreditacion(String correo, MultipartFile archivo) {
        Long idUsuario = lectura.execute(status -> {
            Contexto ctx = buscarContexto(correo);
            exigirQuePuedaSubir(ctx.empresa());
            return ctx.usuario().getId();
        });

        DocumentoService.ArchivoValidado validado = documentoService.validar(archivo, FORMATOS_ACREDITACION);
        String clave = documentoService.subirAlAlmacenamiento(idUsuario, TipoDocumento.ACREDITACION_EMPRESA, validado);

        String[] claveAnterior = new String[1];
        SolicitudEmpresaResponse respuesta;
        try {
            respuesta = transaccion.execute(status -> {
                Contexto ctx = buscarContexto(correo);
                Empresa empresa = ctx.empresa();
                exigirQuePuedaSubir(empresa);

                Documento anterior = empresa.getDocumentoAcreditacion();
                Documento nuevo = documentoService.registrar(ctx.usuario(), TipoDocumento.ACREDITACION_EMPRESA,
                        validado, clave);
                empresa.setDocumentoAcreditacion(nuevo);
                empresa.setEstado(EstadoEmpresa.EN_REVISION);
                if (anterior != null) {
                    claveAnterior[0] = anterior.getClaveAlmacenamiento();
                    documentoRepository.delete(anterior);
                }
                empresaRepository.save(empresa);
                actividad.registrar(empresa, TipoActividadEmpresa.DOCUMENTO_ENVIADO, null, null);
                return mapear(empresa);
            });
        } catch (RuntimeException e) {
            documentoService.borrarArchivo(clave);
            throw e;
        }

        if (claveAnterior[0] != null) {
            documentoService.borrarArchivo(claveAnterior[0]);
        }
        return respuesta;
    }

    public UrlTemporalResponse urlMiDocumento(String correo) {
        Documento documento = lectura.execute(status -> {
            Documento d = buscarContexto(correo).empresa().getDocumentoAcreditacion();
            if (d == null) {
                throw new ResourceNotFoundException("Aún no has subido el documento de acreditación");
            }
            d.getClaveAlmacenamiento();
            return d;
        });
        return new UrlTemporalResponse(documentoService.urlFirmada(documento), storageProps.urlFirmadaSegundos());
    }


    private record Contexto(Usuario usuario, Empresa empresa) {
    }

    private Contexto buscarContexto(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        Persona persona = personaRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Tu cuenta no tiene datos personales registrados"));
        Representante representante = representanteRepository.findByPersona_IdPersona(persona.getIdPersona())
                .orElseThrow(() -> new ResourceNotFoundException("Tu cuenta no está vinculada a una empresa"));
        return new Contexto(usuario, representante.getEmpresa());
    }

    private static void exigirQuePuedaSubir(Empresa empresa) {
        if (!puedeSubir(empresa.getEstado())) {
            throw new BadRequestException(switch (empresa.getEstado()) {
                case EN_REVISION -> "Tu solicitud ya está en revisión. Espera la respuesta del administrador.";
                case APROBADA -> "Tu empresa ya fue aprobada.";
                case RECHAZADA -> "Tu solicitud fue rechazada; no se pueden subir más documentos.";
                case SUSPENDIDA -> "Tu cuenta está suspendida; no se pueden subir documentos.";
                case BLOQUEADA -> "Tu cuenta fue bloqueada; no se pueden subir documentos.";
                default -> "No se puede subir el documento en el estado actual.";
            });
        }
    }

    private static boolean puedeSubir(EstadoEmpresa estado) {
        return estado == EstadoEmpresa.PENDIENTE_DOCUMENTO || estado == EstadoEmpresa.OBSERVADA;
    }

    private SolicitudEmpresaResponse mapear(Empresa empresa) {
        return new SolicitudEmpresaResponse(
                empresa.getPais(),
                empresa.getRuc(),
                empresa.getRazonSocial(),
                empresa.getEstado(),
                empresa.getMotivoRevision(),
                empresa.getFechaRevision(),
                DocumentoResponse.de(empresa.getDocumentoAcreditacion()),
                puedeSubir(empresa.getEstado()),
                null);
    }
}
