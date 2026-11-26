package com.mineria.reclutamiento.modules.postulacion.service;

import com.mineria.reclutamiento.core.exception.BadRequestException;
import com.mineria.reclutamiento.core.exception.ResourceNotFoundException;
import com.mineria.reclutamiento.modules.convocatoria.entity.Convocatoria;
import com.mineria.reclutamiento.modules.convocatoria.repository.ConvocatoriaRepository;
import com.mineria.reclutamiento.modules.documento.dto.UrlTemporalResponse;
import com.mineria.reclutamiento.modules.documento.service.DocumentoService;
import com.mineria.reclutamiento.modules.empresa.service.EmpresaActual;
import com.mineria.reclutamiento.modules.persona.entity.Persona;
import com.mineria.reclutamiento.modules.postulacion.dto.response.CandidatoResponse;
import com.mineria.reclutamiento.modules.postulacion.entity.EstadoPostulacion;
import com.mineria.reclutamiento.modules.postulacion.entity.Postulacion;
import com.mineria.reclutamiento.modules.postulacion.repository.PostulacionRepository;
import com.mineria.reclutamiento.modules.postulante.entity.Certificacion;
import com.mineria.reclutamiento.modules.postulante.entity.DocumentoPostulante;
import com.mineria.reclutamiento.modules.postulante.entity.Postulante;
import com.mineria.reclutamiento.modules.postulante.dto.response.PerfilProfesionalResponse;
import com.mineria.reclutamiento.modules.postulante.service.DocumentosPostulanteService;
import com.mineria.reclutamiento.modules.postulante.service.PerfilProfesionalService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class CandidatoService {

    private static final Set<EstadoPostulacion> ESTADOS_DE_LA_EMPRESA =
            EnumSet.of(EstadoPostulacion.EN_REVISION, EstadoPostulacion.ACEPTADO, EstadoPostulacion.DESCARTADO);

    private final EmpresaActual empresaActual;
    private final ConvocatoriaRepository convocatoriaRepository;
    private final PostulacionRepository postulacionRepository;
    private final DocumentoService documentoService;
    private final Clock clock;
    private final TransactionTemplate lectura;
    private final PerfilProfesionalService perfilProfesionalService;

    public CandidatoService(EmpresaActual empresaActual, ConvocatoriaRepository convocatoriaRepository,
                                      PostulacionRepository postulacionRepository,
                                      DocumentoService documentoService, Clock clock,
                                      PlatformTransactionManager transactionManager,
                                      PerfilProfesionalService perfilProfesionalService) {
        this.empresaActual = empresaActual;
        this.convocatoriaRepository = convocatoriaRepository;
        this.postulacionRepository = postulacionRepository;
        this.documentoService = documentoService;
        this.clock = clock;
        this.lectura = new TransactionTemplate(transactionManager);
        this.lectura.setReadOnly(true);
        this.perfilProfesionalService = perfilProfesionalService;
    }

    @Transactional(readOnly = true)
    public List<CandidatoResponse> candidatos(String correo, Long idConvocatoria) {
        Long idEmpresa = empresaActual.obtenerAprobada(correo).empresa().getId();
        Convocatoria convocatoria = convocatoriaRepository.findById(idConvocatoria)
                .filter(c -> c.getEmpresa().getId().equals(idEmpresa))
                .orElseThrow(() -> new ResourceNotFoundException("Convocatoria no encontrada"));
        return postulacionRepository.deConvocatoria(convocatoria.getId()).stream()
                .map(this::candidato)
                .toList();
    }

    @Transactional
    public CandidatoResponse cambiarEstado(String correo, Long idPostulacion, EstadoPostulacion nuevo) {
        if (!ESTADOS_DE_LA_EMPRESA.contains(nuevo)) {
            throw new BadRequestException("Estado no válido: usa EN_REVISION, ACEPTADO o DESCARTADO.");
        }
        EmpresaActual.Contexto ctx = empresaActual.obtenerAprobada(correo);
        Postulacion postulacion = buscarPropia(idPostulacion, ctx.empresa().getId());
        postulacion.cambiarEstado(nuevo, LocalDateTime.now(clock));
        return candidato(postulacion);
    }

    public UrlTemporalResponse urlDocumento(String correo, Long idPostulacion, DocumentoPostulante tipo) {
        return urlTemporal(correo, idPostulacion, p -> DocumentosPostulanteService.claveVigente(p.getPostulante(), tipo)
                .orElseThrow(() -> new ResourceNotFoundException("El candidato no tiene cargado " + tipo.nombre() + ".")));
    }

    public UrlTemporalResponse urlCertificado(String correo, Long idPostulacion, Long idCertificacion) {
        return urlTemporal(correo, idPostulacion, p -> {
            Certificacion cert = p.getCertificaciones().stream()
                    .filter(c -> c.getId().equals(idCertificacion))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Certificación no adjunta a esta postulación"));
            if (cert.getDocumento() == null) {
                throw new ResourceNotFoundException("Esta certificación no tiene PDF.");
            }
            return cert.getDocumento().getClaveAlmacenamiento();
        });
    }

    private UrlTemporalResponse urlTemporal(String correo, Long idPostulacion, Function<Postulacion, String> claveDe) {
        String clave = lectura.execute(status ->
                claveDe.apply(buscarPropia(idPostulacion, empresaActual.obtenerAprobada(correo).empresa().getId())));
        return new UrlTemporalResponse(documentoService.urlFirmada(clave), documentoService.segundosUrlFirmada());
    }


    private Postulacion buscarPropia(Long idPostulacion, Long idEmpresa) {
        return postulacionRepository.findById(idPostulacion)
                .filter(p -> p.getConvocatoria().getEmpresa().getId().equals(idEmpresa))
                .orElseThrow(() -> new ResourceNotFoundException("Postulación no encontrada"));
    }

    private CandidatoResponse candidato(Postulacion p) {
        Postulante postulante = p.getPostulante();
        Persona persona = postulante.getPersona();
        PerfilProfesionalResponse perfil = perfilProfesionalService.obtenerDe(postulante);
        String especialidadPrincipal = perfil.especialidades().stream()
                .filter(PerfilProfesionalResponse.Especialidad::principal)
                .map(PerfilProfesionalResponse.Especialidad::nombre)
                .findFirst().orElse(null);
        List<CandidatoResponse.CertificacionAdjunta> certificaciones = p.getCertificaciones().stream()
                .sorted(Comparator.comparing(Certificacion::getId))
                .map(c -> new CandidatoResponse.CertificacionAdjunta(c.getId(), c.getNombre(), c.getEntidadEmisora(),
                        c.getDocumento() != null))
                .toList();
        return new CandidatoResponse(
                p.getId(), persona.getNombres(), persona.getApellidos(), persona.getUsuario().getCorreo(),
                persona.getTelefono(), especialidadPrincipal,
                aniosExperiencia(perfil.experiencias(), LocalDate.now(clock)),
                certificaciones,
                DocumentosPostulanteService.documentos(postulante, persona.getUsuario().getId()),
                p.getEstado(), p.getFechaPostulacion(), p.getFechaCambioEstado(),
                p.getRequisitosCubiertos().stream()
                        .map(r -> new CandidatoResponse.RequisitoCubierto(r.getRequisito().getNombre(),
                                r.getRequisito().isRequierePdf(), r.getCertificacion().getId(),
                                r.getCertificacion().getNombre(), r.getCertificacion().getDocumento() != null,
                                PostulacionService.coincide(r.getRequisito(), r.getCertificacion())))
                        .toList(),
                perfil.resumenProfesional(), perfil.especialidades(), perfil.habilidades(),
                perfil.experiencias(), perfil.formaciones(), p.getMotivoRetiro(), p.getEntrevista());
    }

    record Periodo(LocalDate inicio, LocalDate fin) {
    }

    static int aniosExperiencia(List<PerfilProfesionalResponse.Experiencia> experiencias, LocalDate hoy) {
        List<Periodo> periodos = experiencias.stream()
                .filter(e -> e.fechaInicio() != null)
                .map(e -> new Periodo(e.fechaInicio(), e.fechaFin() != null ? e.fechaFin() : hoy))
                .filter(p -> !p.fin().isBefore(p.inicio()))
                .sorted(Comparator.comparing(Periodo::inicio))
                .toList();

        List<Periodo> unidos = new ArrayList<>();
        for (Periodo actual : periodos) {
            int ultimo = unidos.size() - 1;
            if (ultimo >= 0 && !actual.inicio().isAfter(unidos.get(ultimo).fin())) {
                Periodo anterior = unidos.get(ultimo);
                LocalDate fin = actual.fin().isAfter(anterior.fin()) ? actual.fin() : anterior.fin();
                unidos.set(ultimo, new Periodo(anterior.inicio(), fin));
            } else {
                unidos.add(actual);
            }
        }

        long meses = unidos.stream().mapToLong(p -> ChronoUnit.MONTHS.between(p.inicio(), p.fin())).sum();
        return (int) (meses / 12);
    }
}
