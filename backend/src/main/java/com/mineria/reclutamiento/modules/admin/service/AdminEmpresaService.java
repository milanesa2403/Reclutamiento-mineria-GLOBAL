package com.mineria.reclutamiento.modules.admin.service;

import com.mineria.reclutamiento.core.exception.BadRequestException;
import com.mineria.reclutamiento.core.exception.ResourceNotFoundException;
import com.mineria.reclutamiento.modules.admin.dto.EmpresaAdminDetalleResponse;
import com.mineria.reclutamiento.modules.admin.dto.EmpresaAdminItemResponse;
import com.mineria.reclutamiento.modules.auth.entity.Usuario;
import com.mineria.reclutamiento.modules.auth.repository.UsuarioRepository;
import com.mineria.reclutamiento.modules.documento.dto.DocumentoResponse;
import com.mineria.reclutamiento.modules.documento.dto.UrlTemporalResponse;
import com.mineria.reclutamiento.modules.documento.entity.Documento;
import com.mineria.reclutamiento.modules.documento.service.DocumentoService;
import com.mineria.reclutamiento.core.config.AwsProperties;
import com.mineria.reclutamiento.modules.empresa.entity.Empresa;
import com.mineria.reclutamiento.modules.empresa.entity.EstadoEmpresa;
import com.mineria.reclutamiento.modules.empresa.repository.HistorialRechazoRepository;
import com.mineria.reclutamiento.modules.empresa.repository.EmpresaRepository;
import com.mineria.reclutamiento.modules.empresa.repository.RepresentanteRepository;
import com.mineria.reclutamiento.modules.persona.entity.Persona;
import com.mineria.reclutamiento.modules.empresa.entity.TipoActividadEmpresa;
import com.mineria.reclutamiento.modules.empresa.service.ActividadEmpresaService;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AdminEmpresaService {

    private static final Set<EstadoEmpresa> RECHAZABLES =
            EnumSet.of(EstadoEmpresa.PENDIENTE_DOCUMENTO, EstadoEmpresa.EN_REVISION, EstadoEmpresa.OBSERVADA);

    private final EmpresaRepository empresaRepository;
    private final RepresentanteRepository representanteRepository;
    private final HistorialRechazoRepository historialRepository;
    private final UsuarioRepository usuarioRepository;
    private final DocumentoService documentoService;
    private final AwsProperties storageProps;
    private final TransactionTemplate lectura;
    private final ActividadEmpresaService actividad;

    public AdminEmpresaService(EmpresaRepository empresaRepository, RepresentanteRepository representanteRepository,
                               HistorialRechazoRepository historialRepository,
                               UsuarioRepository usuarioRepository, DocumentoService documentoService,
                               AwsProperties storageProps,
                               PlatformTransactionManager transactionManager,
                               ActividadEmpresaService actividad) {

        this.lectura = new TransactionTemplate(transactionManager);
        this.lectura.setReadOnly(true);
        this.actividad = actividad;
        this.empresaRepository = empresaRepository;
        this.representanteRepository = representanteRepository;
        this.historialRepository = historialRepository;
        this.usuarioRepository = usuarioRepository;
        this.documentoService = documentoService;
        this.storageProps = storageProps;
    }

    @Transactional(readOnly = true)
    public List<EmpresaAdminItemResponse> listar(EstadoEmpresa estado) {
        List<Empresa> empresas = estado == null
                ? empresaRepository.findAllByOrderByIdDesc()
                : empresaRepository.findByEstadoOrderByIdDesc(estado);
        return empresas.stream()
                .map(e -> new EmpresaAdminItemResponse(
                        e.getId(),
                        e.getPais(),
                        e.getRuc(),
                        e.getRazonSocial(),
                        e.getSectorMinero() != null ? e.getSectorMinero().getNombre() : null,
                        e.getCorreoCorporativo(),
                        e.getFechaRegistro(),
                        e.getEstado(),
                        e.getDocumentoAcreditacion() != null))
                .toList();
    }

    @Transactional(readOnly = true)
    public EmpresaAdminDetalleResponse detalle(Long idEmpresa) {
        Empresa e = buscar(idEmpresa);
        List<EmpresaAdminDetalleResponse.Solicitante> solicitantes = representanteRepository.findByEmpresa_Id(e.getId())
                .stream()
                .map(r -> {
                    Persona p = r.getPersona();
                    return new EmpresaAdminDetalleResponse.Solicitante(p.getPais(),
                            p.getTipoDocumento() != null ? p.getTipoDocumento().name() : null, p.getDni(), p.getNombres(), p.getApellidos(),
                            r.getCargo(), p.getUsuario().getCorreo(), p.getTelefono());
                })
                .toList();
        List<EmpresaAdminDetalleResponse.RechazoAnterior> rechazos = historialRepository
                .findByRucOrderByFechaRechazoDesc(e.getRuc()).stream()
                .map(h -> new EmpresaAdminDetalleResponse.RechazoAnterior(h.getMotivo(), h.getFechaRechazo()))
                .toList();
        List<EmpresaAdminDetalleResponse.Evento> historial = actividad.historial(e.getId()).stream()
                .map(a -> new EmpresaAdminDetalleResponse.Evento(
                        a.getTipo(), a.getMotivo(), a.getRealizadoPor(), a.getFecha()))
                .toList();

        return new EmpresaAdminDetalleResponse(
                e.getId(), e.getPais(), e.getRuc(), e.getRazonSocial(), e.getDireccion(), e.getTelefono(), e.getCorreoCorporativo(),
                e.getSectorMinero() != null ? e.getSectorMinero().getNombre() : null,
                e.getDescripcion(), e.getFechaRegistro(), e.getEstado(), e.getMotivoRevision(), e.getFechaRevision(),
                e.getAdminRevisor() != null ? e.getAdminRevisor().getCorreo() : null,
                DocumentoResponse.de(e.getDocumentoAcreditacion()),
                solicitantes, rechazos,
                actividad.contarAdvertencias(e.getId()),
                historial);
    }

    public UrlTemporalResponse urlDocumento(Long idEmpresa) {
        Documento documento = lectura.execute(status -> {
            Documento d = buscar(idEmpresa).getDocumentoAcreditacion();
            if (d == null) {
                throw new ResourceNotFoundException("La empresa aún no subió el documento de acreditación");
            }
            d.getClaveAlmacenamiento();
            return d;
        });
        return new UrlTemporalResponse(documentoService.urlFirmada(documento), storageProps.urlFirmadaSegundos());
    }

    @Transactional
    public EmpresaAdminDetalleResponse aprobar(Long idEmpresa, String correoAdmin) {
        Empresa e = buscar(idEmpresa);
        exigirEstado(e, EnumSet.of(EstadoEmpresa.EN_REVISION), "aprobar");
        decidir(e, EstadoEmpresa.APROBADA, null, correoAdmin);
        return detalle(idEmpresa);
    }

    @Transactional
    public EmpresaAdminDetalleResponse observar(Long idEmpresa, String motivo, String correoAdmin) {
        Empresa e = buscar(idEmpresa);
        exigirEstado(e, EnumSet.of(EstadoEmpresa.EN_REVISION), "observar");
        decidir(e, EstadoEmpresa.OBSERVADA, motivo.trim(), correoAdmin);
        return detalle(idEmpresa);
    }

    @Transactional
    public EmpresaAdminDetalleResponse rechazar(Long idEmpresa, String motivo, String correoAdmin) {
        Empresa e = buscar(idEmpresa);
        exigirEstado(e, RECHAZABLES, "rechazar");
        decidir(e, EstadoEmpresa.RECHAZADA, motivo.trim(), correoAdmin);
        return detalle(idEmpresa);
    }


    private void decidir(Empresa e, EstadoEmpresa nuevo, String motivo, String correoAdmin) {
        Usuario admin = usuarioRepository.findByCorreo(correoAdmin)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario administrador no encontrado"));
        e.setEstado(nuevo);
        e.setMotivoRevision(motivo);
        e.setFechaRevision(LocalDateTime.now());
        e.setAdminRevisor(admin);
        empresaRepository.save(e);
        actividad.registrar(e, TipoActividadEmpresa.deDecision(nuevo), motivo, correoAdmin);
    }

    private static void exigirEstado(Empresa e, Set<EstadoEmpresa> permitidos, String accion) {
        if (!permitidos.contains(e.getEstado())) {
            throw new BadRequestException("No se puede " + accion + " una empresa en estado " + e.getEstado() + ".");
        }
    }

    private Empresa buscar(Long idEmpresa) {
        return empresaRepository.findById(idEmpresa)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));
    }
}
