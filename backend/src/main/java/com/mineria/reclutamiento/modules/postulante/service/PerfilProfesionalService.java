package com.mineria.reclutamiento.modules.postulante.service;

import com.mineria.reclutamiento.core.exception.BadRequestException;
import com.mineria.reclutamiento.core.exception.CamposInvalidosException;
import com.mineria.reclutamiento.modules.documento.dto.DocumentoResponse;
import com.mineria.reclutamiento.modules.documento.entity.Documento;
import com.mineria.reclutamiento.modules.documento.repository.DocumentoRepository;
import com.mineria.reclutamiento.modules.documento.service.DocumentoService;
import com.mineria.reclutamiento.modules.postulacion.entity.EstadoPostulacion;
import com.mineria.reclutamiento.modules.postulante.dto.request.PerfilProfesionalRequest.CertificacionItem;
import com.mineria.reclutamiento.modules.postulante.dto.request.PerfilProfesionalRequest;
import com.mineria.reclutamiento.modules.postulante.dto.request.PerfilProfesionalRequest.EspecialidadItem;
import com.mineria.reclutamiento.modules.postulante.dto.request.PerfilProfesionalRequest.ExperienciaItem;
import com.mineria.reclutamiento.modules.postulante.dto.request.PerfilProfesionalRequest.FormacionItem;
import com.mineria.reclutamiento.modules.postulante.dto.response.PerfilProfesionalResponse;
import com.mineria.reclutamiento.modules.postulante.entity.CatalogoCertificacion;
import com.mineria.reclutamiento.modules.postulante.entity.Certificacion;
import com.mineria.reclutamiento.modules.postulante.entity.EspecialidadPostulante;
import com.mineria.reclutamiento.modules.postulante.entity.ExperienciaLaboral;
import com.mineria.reclutamiento.modules.postulante.entity.FormacionAcademica;
import com.mineria.reclutamiento.modules.postulante.entity.HabilidadPostulante;
import com.mineria.reclutamiento.modules.postulante.entity.Postulante;
import com.mineria.reclutamiento.modules.postulante.repository.CatalogoCertificacionRepository;
import com.mineria.reclutamiento.modules.postulante.repository.CertificacionRepository;
import com.mineria.reclutamiento.modules.postulante.repository.EspecialidadPostulanteRepository;
import com.mineria.reclutamiento.modules.postulante.repository.ExperienciaLaboralRepository;
import com.mineria.reclutamiento.modules.postulante.repository.FormacionAcademicaRepository;
import com.mineria.reclutamiento.modules.postulante.repository.HabilidadPostulanteRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PerfilProfesionalService {

    private final PostulanteService postulanteService;
    private final EspecialidadPostulanteRepository especialidadRepository;
    private final HabilidadPostulanteRepository habilidadRepository;
    private final ExperienciaLaboralRepository experienciaRepository;
    private final FormacionAcademicaRepository formacionRepository;
    private final CertificacionRepository certificacionRepository;
    private final CatalogoCertificacionRepository catalogoRepository;
    private final EstadoPerfilProfesional estadoPerfil;
    private final DocumentoRepository documentoRepository;
    private final DocumentoService documentoService;

    public PerfilProfesionalService(PostulanteService postulanteService,
                                    EspecialidadPostulanteRepository especialidadRepository,
                                    HabilidadPostulanteRepository habilidadRepository,
                                    ExperienciaLaboralRepository experienciaRepository,
                                    FormacionAcademicaRepository formacionRepository,
                                    CertificacionRepository certificacionRepository,
                                    CatalogoCertificacionRepository catalogoRepository,
                                    EstadoPerfilProfesional estadoPerfil,
                                    DocumentoRepository documentoRepository,
                                    DocumentoService documentoService) {
        this.postulanteService = postulanteService;
        this.especialidadRepository = especialidadRepository;
        this.habilidadRepository = habilidadRepository;
        this.experienciaRepository = experienciaRepository;
        this.formacionRepository = formacionRepository;
        this.certificacionRepository = certificacionRepository;
        this.catalogoRepository = catalogoRepository;
        this.estadoPerfil = estadoPerfil;
        this.documentoRepository = documentoRepository;
        this.documentoService = documentoService;
    }

    @Transactional(readOnly = true)
    public PerfilProfesionalResponse obtener(String correo) {
        return construirRespuesta(postulanteService.postulanteDe(correo));
    }

    public PerfilProfesionalResponse obtenerDe(Postulante postulante) {
        return construirRespuesta(postulante);
    }

    @Transactional
    public PerfilProfesionalResponse reemplazar(String correo, PerfilProfesionalRequest request) {
        List<EspecialidadItem> especialidades = normalizarEspecialidades(request.especialidades());
        List<String> habilidades = request.habilidades().stream().map(String::trim).toList();
        validarReglasCruzadas(especialidades, habilidades, request);

        Postulante postulante = postulanteService.postulanteDe(correo);
        Long id = postulante.getIdPostulante();
        if (request.resumenProfesional() != null) {
            postulante.setResumenProfesional(textoOpcional(request.resumenProfesional()));
        }

        especialidadRepository.borrarDelPostulante(id);
        habilidadRepository.borrarDelPostulante(id);
        experienciaRepository.borrarDelPostulante(id);
        formacionRepository.borrarDelPostulante(id);

        especialidadRepository.saveAll(especialidades.stream()
                .map(e -> new EspecialidadPostulante(postulante, e.nombre(), e.principal())).toList());
        habilidadRepository.saveAll(habilidades.stream().map(h -> new HabilidadPostulante(postulante, h)).toList());
        experienciaRepository.saveAll(request.experiencias().stream()
                .map(x -> new ExperienciaLaboral(postulante, x.puesto().trim(), x.empresa().trim(),
                        x.fechaInicio(), x.fechaFin(), textoOpcional(x.descripcion()))).toList());
        formacionRepository.saveAll(request.formaciones().stream()
                .map(x -> new FormacionAcademica(postulante, x.institucion().trim(), x.carreraGrado().trim(),
                        x.fechaInicio(), x.fechaFin())).toList());
        guardarCertificaciones(postulante, request.certificaciones());
        especialidadRepository.flush();

        return construirRespuesta(postulante);
    }


    private void guardarCertificaciones(Postulante postulante, List<CertificacionItem> items) {
        Map<Long, CatalogoCertificacion> tipos = tiposElegidos(items);
        Map<Long, Certificacion> actuales = certificacionRepository
                .delPostulante(postulante.getIdPostulante()).stream()
                .collect(Collectors.toMap(Certificacion::getId, Function.identity()));
        Set<Long> conservadas = new HashSet<>();
        List<Certificacion> nuevas = new ArrayList<>();
        for (CertificacionItem x : items) {
            CatalogoCertificacion tipo = x.idCatalogoCertificacion() != null ? tipos.get(x.idCatalogoCertificacion()) : null;
            String nombre = tipo != null ? tipo.getNombre() : x.nombre().trim();
            Certificacion existente = x.id() != null ? actuales.get(x.id()) : null;
            if (existente != null && conservadas.add(existente.getId())) {
                existente.actualizar(tipo, nombre, x.entidadEmisora().trim(), x.fechaEmision());
            } else {
                nuevas.add(new Certificacion(postulante, tipo, nombre, x.entidadEmisora().trim(), x.fechaEmision()));
            }
        }

        List<Certificacion> quitadas = actuales.values().stream()
                .filter(c -> !conservadas.contains(c.getId())).toList();
        if (!quitadas.isEmpty()) {
            List<String> puestos = certificacionRepository.puestosQueLasUsan(
                    quitadas.stream().map(Certificacion::getId).toList(),
                    List.of(EstadoPostulacion.POSTULADO, EstadoPostulacion.EN_REVISION));
            if (!puestos.isEmpty()) {
                throw new BadRequestException("No puedes quitar una certificación que respalda tu postulación en curso a: "
                        + String.join(", ", puestos) + ".");
            }
        }
        List<Documento> pdfs = quitadas.stream().map(Certificacion::getDocumento).filter(Objects::nonNull).toList();
        List<String> claves = pdfs.stream().map(Documento::getClaveAlmacenamiento).toList();
        certificacionRepository.deleteAll(quitadas);
        documentoRepository.deleteAll(pdfs);
        certificacionRepository.saveAll(nuevas);

        if (!claves.isEmpty() && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    claves.forEach(documentoService::borrarArchivo);
                }
            });
        }
    }

    private Map<Long, CatalogoCertificacion> tiposElegidos(List<CertificacionItem> items) {
        Map<Long, CatalogoCertificacion> tipos = catalogoRepository.findAllById(items.stream()
                        .map(CertificacionItem::idCatalogoCertificacion).filter(Objects::nonNull).toList())
                .stream().collect(Collectors.toMap(CatalogoCertificacion::getId, Function.identity()));
        Map<String, String> errores = new LinkedHashMap<>();
        for (int i = 0; i < items.size(); i++) {
            Long idTipo = items.get(i).idCatalogoCertificacion();
            if (idTipo != null && !tipos.containsKey(idTipo)) {
                errores.put("certificaciones[" + i + "].idCatalogoCertificacion", "Certificado no válido.");
            }
        }
        if (!errores.isEmpty()) {
            throw new CamposInvalidosException(errores);
        }
        return tipos;
    }

    private static List<EspecialidadItem> normalizarEspecialidades(List<EspecialidadItem> recibidas) {
        boolean unaSinMarcar = recibidas.size() == 1 && !recibidas.get(0).principal();
        return recibidas.stream()
                .map(e -> new EspecialidadItem(e.nombre().trim(), e.principal() || unaSinMarcar))
                .toList();
    }

    private static void validarReglasCruzadas(List<EspecialidadItem> especialidades, List<String> habilidades,
                                              PerfilProfesionalRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();

        long principales = especialidades.stream().filter(EspecialidadItem::principal).count();
        if (principales != 1) {
            errores.put("especialidades", "Marca exactamente una especialidad como principal.");
        }
        marcarDuplicados(especialidades.stream().map(EspecialidadItem::nombre).toList(), "especialidades[%d].nombre",
                "Ya agregaste esta especialidad.", errores);
        marcarDuplicados(habilidades, "habilidades[%d]", "Ya agregaste esta habilidad.", errores);

        List<ExperienciaItem> experiencias = request.experiencias();
        for (int i = 0; i < experiencias.size(); i++) {
            validarPeriodo(experiencias.get(i).fechaInicio(), experiencias.get(i).fechaFin(),
                    "experiencias[" + i + "].fechaFin", errores);
        }
        List<FormacionItem> formaciones = request.formaciones();
        for (int i = 0; i < formaciones.size(); i++) {
            validarPeriodo(formaciones.get(i).fechaInicio(), formaciones.get(i).fechaFin(),
                    "formaciones[" + i + "].fechaFin", errores);
        }

        if (!errores.isEmpty()) {
            throw new CamposInvalidosException(errores);
        }
    }

    private static void marcarDuplicados(List<String> valores, String formatoClave, String mensaje,
                                         Map<String, String> errores) {
        Set<String> vistos = new HashSet<>();
        for (int i = 0; i < valores.size(); i++) {
            if (!vistos.add(valores.get(i).toLowerCase(Locale.ROOT))) {
                errores.put(String.format(formatoClave, i), mensaje);
            }
        }
    }

    private static void validarPeriodo(LocalDate inicio, LocalDate fin, String clave, Map<String, String> errores) {
        if (inicio != null && fin != null && fin.isBefore(inicio)) {
            errores.put(clave, "La fecha de fin debe ser posterior a la de inicio.");
        }
    }

    private PerfilProfesionalResponse construirRespuesta(Postulante postulante) {
        Long id = postulante.getIdPostulante();
        List<String> pendientes = estadoPerfil.pendientes(id);
        return new PerfilProfesionalResponse(
                postulante.getResumenProfesional(),
                especialidadRepository.delPostulante(id).stream()
                        .map(e -> new PerfilProfesionalResponse.Especialidad(e.getNombre(), e.isEsPrincipal())).toList(),
                habilidadRepository.delPostulante(id).stream()
                        .map(HabilidadPostulante::getNombre).toList(),
                experienciaRepository.delPostulante(id).stream()
                        .map(x -> new PerfilProfesionalResponse.Experiencia(x.getPuesto(), x.getEmpresa(),
                                x.getFechaInicio(), x.getFechaFin(), x.getDescripcion())).toList(),
                formacionRepository.delPostulante(id).stream()
                        .map(x -> new PerfilProfesionalResponse.Formacion(x.getInstitucion(), x.getCarreraGrado(),
                                x.getFechaInicio(), x.getFechaFin())).toList(),
                certificacionRepository.delPostulante(id).stream()
                        .map(x -> new PerfilProfesionalResponse.Certificacion(x.getId(),
                                x.getCatalogo() != null ? x.getCatalogo().getId() : null, x.getNombre(),
                                x.getEntidadEmisora(), x.getFechaEmision(), DocumentoResponse.de(x.getDocumento())))
                        .toList(),
                pendientes.isEmpty(),
                pendientes);
    }

    private static String textoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
