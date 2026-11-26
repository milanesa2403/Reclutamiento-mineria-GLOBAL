package com.mineria.reclutamiento.modules.postulacion.service;

import com.mineria.reclutamiento.core.exception.BadRequestException;
import com.mineria.reclutamiento.core.exception.DuplicateResourceException;
import com.mineria.reclutamiento.core.exception.ResourceNotFoundException;
import com.mineria.reclutamiento.modules.convocatoria.entity.Convocatoria;
import com.mineria.reclutamiento.modules.convocatoria.entity.RequisitoCertificado;
import com.mineria.reclutamiento.modules.convocatoria.entity.TipoConvocatoria;
import com.mineria.reclutamiento.modules.convocatoria.service.ConvocatoriaPublicaService;
import com.mineria.reclutamiento.modules.ubigeo.dto.UbicacionResponse;
import java.util.HashMap;
import java.util.Map;
import com.mineria.reclutamiento.modules.convocatoria.repository.ConvocatoriaRepository;
import com.mineria.reclutamiento.modules.postulacion.dto.request.PostularRequest;
import com.mineria.reclutamiento.modules.postulacion.dto.response.MiPostulacionResponse;
import com.mineria.reclutamiento.modules.postulacion.dto.response.RequisitosPostulacionResponse;
import com.mineria.reclutamiento.modules.postulacion.entity.Postulacion;
import com.mineria.reclutamiento.modules.postulacion.repository.PostulacionRepository;
import com.mineria.reclutamiento.modules.postulante.entity.Certificacion;
import com.mineria.reclutamiento.modules.postulante.entity.DocumentoPostulante;
import com.mineria.reclutamiento.modules.postulante.entity.Postulante;
import com.mineria.reclutamiento.modules.postulante.repository.CertificacionRepository;
import com.mineria.reclutamiento.modules.postulante.service.DocumentosPostulanteService;
import com.mineria.reclutamiento.modules.postulante.service.EstadoPerfilProfesional;
import com.mineria.reclutamiento.modules.postulante.service.PostulanteService;
import java.time.Clock;
import com.mineria.reclutamiento.modules.postulacion.entity.EstadoPostulacion;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostulacionService {

    private static final int MARGEN_DIAS_CRUCE = 3;

    private final PostulanteService postulanteService;
    private final ConvocatoriaRepository convocatoriaRepository;
    private final PostulacionRepository postulacionRepository;
    private final CertificacionRepository certificacionRepository;
    private final EstadoPerfilProfesional estadoPerfil;
    private final Clock clock;
    @Value("${app.tareas-programadas:true}")
    private boolean tareasProgramadas;

    public PostulacionService(PostulanteService postulanteService, ConvocatoriaRepository convocatoriaRepository,
                              PostulacionRepository postulacionRepository,
                              CertificacionRepository certificacionRepository, EstadoPerfilProfesional estadoPerfil,
                              Clock clock) {
        this.postulanteService = postulanteService;
        this.convocatoriaRepository = convocatoriaRepository;
        this.postulacionRepository = postulacionRepository;
        this.certificacionRepository = certificacionRepository;
        this.estadoPerfil = estadoPerfil;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public RequisitosPostulacionResponse requisitos(String correo, Long idConvocatoria) {
        Postulante postulante = postulanteService.postulanteDe(correo);
        boolean perfilCompleto = estadoPerfil.pendientes(postulante.getIdPostulante()).isEmpty();
        boolean tieneCv = DocumentosPostulanteService.claveVigente(postulante, DocumentoPostulante.CV).isPresent();
        List<Certificacion> propias = certificacionRepository
                .delPostulante(postulante.getIdPostulante());
        boolean tieneCertificaciones = !propias.isEmpty();

        List<String> faltantes = new ArrayList<>();
        if (!perfilCompleto) {
            faltantes.add("Completa tu perfil profesional (especialidad principal y habilidades).");
        }
        if (!tieneCv) {
            faltantes.add("Sube tu CV en PDF desde \"Mi perfil\" para poder postular.");
        }
        if (!tieneCertificaciones) {
            faltantes.add("Aún no registras certificaciones: agrégalas en tu perfil profesional para destacar tu postulación.");
        }
        List<RequisitosPostulacionResponse.CertificadoRequerido> requeridos = new ArrayList<>();
        if (idConvocatoria != null) {
            Convocatoria convocatoria = convocatoriaRepository.findById(idConvocatoria)
                    .orElseThrow(() -> new ResourceNotFoundException("Convocatoria no encontrada"));
            for (RequisitoCertificado req : convocatoria.getRequisitosCertificado()) {
                List<RequisitosPostulacionResponse.Opcion> opciones = propias.stream()
                        .map(c -> new RequisitosPostulacionResponse.Opcion(c.getId(), c.getNombre(),
                                c.getDocumento() != null, coincide(req, c)))
                        .sorted(Comparator.comparing((RequisitosPostulacionResponse.Opcion o) -> !o.coincide())
                                .thenComparing(o -> req.isRequierePdf() && !o.tienePdf()))
                        .toList();
                boolean cumplible = opciones.stream().anyMatch(o -> !req.isRequierePdf() || o.tienePdf());
                boolean coincideConPdf = opciones.stream().anyMatch(o -> o.coincide() && o.tienePdf());
                if (req.isRequierePdf() && !coincideConPdf) {
                    faltantes.add("Esta convocatoria pide el PDF de \"" + req.getNombre() + "\": súbelo desde \"Mi perfil\".");
                } else if (!req.isRequierePdf() && opciones.stream().noneMatch(RequisitosPostulacionResponse.Opcion::coincide)) {
                    faltantes.add("Esta convocatoria exige \"" + req.getNombre() + "\": regístralo en tu perfil profesional si lo tienes.");
                }
                requeridos.add(new RequisitosPostulacionResponse.CertificadoRequerido(req.getId(), req.getNombre(),
                        req.isRequierePdf(), cumplible, opciones));
            }
        }
        boolean requisitosCumplibles = requeridos.stream().allMatch(RequisitosPostulacionResponse.CertificadoRequerido::cumplible);
        return new RequisitosPostulacionResponse(perfilCompleto, tieneCv, tieneCertificaciones,
                perfilCompleto && tieneCv && requisitosCumplibles, faltantes, requeridos);
    }

    @Transactional
    public MiPostulacionResponse postular(String correo, PostularRequest request) {
        Postulante postulante = postulanteService.postulanteDe(correo);
        Convocatoria convocatoria = convocatoriaRepository.findById(request.idConvocatoria())
                .orElseThrow(() -> new ResourceNotFoundException("Convocatoria no encontrada"));
        exigirDisponible(convocatoria);

        if (!estadoPerfil.pendientes(postulante.getIdPostulante()).isEmpty()) {
            throw new BadRequestException("Completa tu perfil profesional antes de postular.");
        }
        if (DocumentosPostulanteService.claveVigente(postulante, DocumentoPostulante.CV).isEmpty()) {
            throw new BadRequestException("Sube tu CV en PDF desde \"Mi perfil\" antes de postular.");
        }
        if (postulacionRepository.existe(convocatoria.getId(),
                postulante.getIdPostulante())) {
            throw new DuplicateResourceException("Ya postulaste a esta convocatoria.");
        }

        Set<Long> ids = request.idsCertificaciones() == null ? Set.of()
                : new LinkedHashSet<>(request.idsCertificaciones());
        List<Certificacion> certificaciones = ids.isEmpty() ? List.of()
                : certificacionRepository.findByIdInAndPostulante_IdPostulante(ids, postulante.getIdPostulante());
        if (certificaciones.size() != ids.size()) {
            throw new BadRequestException("Solo puedes adjuntar certificaciones de tu propio perfil.");
        }

        Postulacion postulacion = new Postulacion(convocatoria, postulante, new HashSet<>(certificaciones));
        cubrirRequisitos(postulacion, convocatoria, postulante, request.requisitos());

        return mapear(postulacionRepository.saveAndFlush(postulacion));
    }

    @Transactional(readOnly = true)
    public List<MiPostulacionResponse> misPostulaciones(String correo) {
        return postulacionesDe(correo).stream().map(this::mapear).toList();
    }

    @Transactional
    public MiPostulacionResponse confirmar(String correo, Long id) {
        Postulacion p = propia(correo, id);
        LocalDateTime ahora = LocalDateTime.now(clock);
        p.confirmar(ahora);
        for (Postulacion otra : postulacionesDe(correo)) {
            boolean sigueActiva = otra != p && otra.getEstado().activo();
            if (sigueActiva && cruzan(p.getConvocatoria(), otra.getConvocatoria())) {
                otra.retirar("No disponible", ahora);
            }
        }
        return mapear(p);
    }

    @Transactional
    public MiPostulacionResponse retirar(String correo, Long id) {
        Postulacion p = propia(correo, id);
        p.retirar(null, LocalDateTime.now(clock));
        return mapear(p);
    }

    @Transactional
    public void marcarVistas(String correo) {
        postulacionesDe(correo).forEach(Postulacion::marcarVisto);
    }

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "0 5 0 * * *", zone = "America/Lima")
    public void retirarSinConfirmar() {
        if (tareasProgramadas) {
            LocalDateTime ahora = LocalDateTime.now(clock);
            postulacionRepository.retirarSinConfirmar(EstadoPostulacion.ACEPTADO, EstadoPostulacion.RETIRADO,
                    ahora.minusDays(Postulacion.DIAS_PARA_CONFIRMAR), ahora);
        }
    }

    private List<Postulacion> postulacionesDe(String correo) {
        return postulacionRepository.delPostulante(
                postulanteService.postulanteDe(correo).getIdPostulante());
    }

    private Postulacion propia(String correo, Long id) {
        Long idPostulante = postulanteService.postulanteDe(correo).getIdPostulante();
        return postulacionRepository.findById(id)
                .filter(p -> p.getPostulante().getIdPostulante().equals(idPostulante))
                .orElseThrow(() -> new ResourceNotFoundException("Postulación no encontrada"));
    }

    static boolean cruzan(Convocatoria confirmada, Convocatoria otra) {
        if (confirmada.getTipo() != TipoConvocatoria.PARADA_PLANTA || otra.getTipo() != TipoConvocatoria.PARADA_PLANTA) {
            return false;
        }
        LocalDate desde = confirmada.getFechaInicioLabores().minusDays(MARGEN_DIAS_CRUCE);
        LocalDate hasta = confirmada.getFechaFinLabores().plusDays(MARGEN_DIAS_CRUCE);
        boolean empiezaDespues = otra.getFechaInicioLabores().isAfter(hasta);
        boolean terminaAntes = otra.getFechaFinLabores().isBefore(desde);
        return !empiezaDespues && !terminaAntes;
    }


    private void cubrirRequisitos(Postulacion postulacion, Convocatoria convocatoria, Postulante postulante,
                                  List<PostularRequest.RequisitoCubierto> elegidos) {
        Map<Long, Long> porRequisito = new HashMap<>();
        for (PostularRequest.RequisitoCubierto e : elegidos == null ? List.<PostularRequest.RequisitoCubierto>of() : elegidos) {
            if (porRequisito.put(e.idRequisito(), e.idCertificacion()) != null) {
                throw new BadRequestException("Indicaste dos veces el mismo requisito.");
            }
        }
        for (RequisitoCertificado req : convocatoria.getRequisitosCertificado()) {
            Long idCert = porRequisito.remove(req.getId());
            if (idCert == null) {
                throw new BadRequestException("Indica qué certificación de tu perfil cumple \"" + req.getNombre() + "\".");
            }
            Certificacion cert = certificacionRepository.findByIdAndPostulante_IdPostulante(idCert, postulante.getIdPostulante())
                    .orElseThrow(() -> new BadRequestException("Solo puedes usar certificaciones de tu propio perfil."));
            if (req.isRequierePdf() && cert.getDocumento() == null) {
                throw new BadRequestException("Esta convocatoria pide el PDF de \"" + req.getNombre()
                        + "\": súbelo desde \"Mi perfil\" (certificación \"" + cert.getNombre() + "\").");
            }
            postulacion.cubrirRequisito(req, cert);
        }
        if (!porRequisito.isEmpty()) {
            throw new BadRequestException("Indicaste un requisito que no pertenece a esta convocatoria.");
        }
    }

    static boolean coincide(RequisitoCertificado requisito, Certificacion certificacion) {
        return certificacion.getCatalogo() != null
                && certificacion.getCatalogo().getId().equals(requisito.getCatalogo().getId());
    }

    private void exigirDisponible(Convocatoria convocatoria) {
        if (!ConvocatoriaPublicaService.disponible(convocatoria, LocalDate.now(clock))) {
            throw new BadRequestException("Esta convocatoria no está disponible para postular.");
        }
    }

    MiPostulacionResponse mapear(Postulacion p) {
        Convocatoria c = p.getConvocatoria();
        return new MiPostulacionResponse(
                p.getId(), c.getId(), c.getPuesto(), c.getEmpresa().getRazonSocial(), c.getTipo(), c.getCampamento(),
                c.getEstado(), p.getEstado(), p.getFechaPostulacion(), p.getFechaCambioEstado(),
                p.getCertificaciones().stream().map(Certificacion::getNombre).sorted(Comparator.naturalOrder()).toList(),
                UbicacionResponse.de(c.getUbigeo()), c.getFechaCierre(), p.getFechaLimite(),
                p.getMotivoRetiro(), p.isVisto(), p.getEntrevista(),
                null, c.getTurno(), c.getFechaInicioLabores(),
                c.getFechaFinLabores(), c.getDireccion(), c.getReferencia(), c.getDescripcion());
    }
}
