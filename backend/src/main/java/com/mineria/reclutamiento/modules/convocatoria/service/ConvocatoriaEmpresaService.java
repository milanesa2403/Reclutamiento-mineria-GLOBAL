package com.mineria.reclutamiento.modules.convocatoria.service;

import com.mineria.reclutamiento.core.exception.BadRequestException;
import com.mineria.reclutamiento.core.exception.CamposInvalidosException;
import com.mineria.reclutamiento.core.exception.ResourceNotFoundException;
import com.mineria.reclutamiento.modules.convocatoria.dto.ConvocatoriaRequest;
import com.mineria.reclutamiento.modules.convocatoria.dto.ConvocatoriaResponse;
import com.mineria.reclutamiento.modules.convocatoria.entity.Busqueda;
import com.mineria.reclutamiento.modules.convocatoria.entity.Convocatoria;
import com.mineria.reclutamiento.modules.convocatoria.entity.EstadoConvocatoria;
import com.mineria.reclutamiento.modules.convocatoria.entity.RequisitoCertificado;
import com.mineria.reclutamiento.modules.convocatoria.entity.TipoConvocatoria;
import com.mineria.reclutamiento.modules.convocatoria.repository.BusquedaRepository;
import com.mineria.reclutamiento.modules.convocatoria.repository.ConvocatoriaRepository;
import com.mineria.reclutamiento.modules.empresa.service.EmpresaActual;
import com.mineria.reclutamiento.modules.postulacion.entity.EstadoPostulacion;
import com.mineria.reclutamiento.modules.postulacion.repository.PostulacionRepository;
import com.mineria.reclutamiento.modules.postulante.entity.CatalogoCertificacion;
import com.mineria.reclutamiento.modules.postulante.repository.CatalogoCertificacionRepository;
import com.mineria.reclutamiento.modules.ubigeo.entity.Ubigeo;
import com.mineria.reclutamiento.modules.ubigeo.repository.UbigeoRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConvocatoriaEmpresaService {

    private static final Set<EstadoPostulacion> PENDIENTES = Set.of(EstadoPostulacion.POSTULADO, EstadoPostulacion.EN_REVISION);

    private final EmpresaActual empresaActual;
    private final ConvocatoriaRepository convocatoriaRepository;
    private final UbigeoRepository ubigeoRepository;
    private final PostulacionRepository postulacionRepository;
    private final BusquedaRepository busquedaRepository;
    private final CatalogoCertificacionRepository catalogoRepository;
    private final Clock clock;

    public ConvocatoriaEmpresaService(EmpresaActual empresaActual, ConvocatoriaRepository convocatoriaRepository,
                                      UbigeoRepository ubigeoRepository, PostulacionRepository postulacionRepository,
                                      BusquedaRepository busquedaRepository,
                                      CatalogoCertificacionRepository catalogoRepository, Clock clock) {
        this.empresaActual = empresaActual;
        this.convocatoriaRepository = convocatoriaRepository;
        this.ubigeoRepository = ubigeoRepository;
        this.postulacionRepository = postulacionRepository;
        this.busquedaRepository = busquedaRepository;
        this.catalogoRepository = catalogoRepository;
        this.clock = clock;
    }

    @Transactional
    public List<ConvocatoriaResponse> listar(String correo, EstadoConvocatoria estado) {
        Long idEmpresa = empresaActual.obtenerAprobada(correo).empresa().getId();
        convocatoriaRepository.vencidasDeEmpresa(idEmpresa, EstadoConvocatoria.PUBLICADA,
                LocalDate.now(clock)).forEach(Convocatoria::cerrar);
        convocatoriaRepository.flush();
        List<Convocatoria> lista = estado == null
                ? convocatoriaRepository.deEmpresa(idEmpresa)
                : convocatoriaRepository.deEmpresaEnEstado(idEmpresa, estado);
        return lista.stream().map(this::respuesta).toList();
    }

    @Transactional(readOnly = true)
    public ConvocatoriaResponse detalle(String correo, Long id) {
        return respuesta(buscarPropia(correo, id));
    }

    @Transactional
    public ConvocatoriaResponse crear(String correo, ConvocatoriaRequest request) {
        EmpresaActual.Contexto ctx = empresaActual.obtenerAprobada(correo);
        Validado v = validar(request);
        Convocatoria c = new Convocatoria(ctx.empresa(), ctx.usuario(), request.tipo());
        aplicar(c, request, v);
        return respuesta(convocatoriaRepository.saveAndFlush(c));
    }

    @Transactional
    public ConvocatoriaResponse actualizar(String correo, Long id, ConvocatoriaRequest request) {
        Convocatoria c = buscarPropia(correo, id);
        if (c.getEstado() != EstadoConvocatoria.BORRADOR) {
            throw new BadRequestException("Solo se puede editar una convocatoria en borrador.");
        }
        Validado v = validar(request);
        c.reemplazarRequisitos(List.of());
        convocatoriaRepository.saveAndFlush(c);
        aplicar(c, request, v);
        return respuesta(convocatoriaRepository.saveAndFlush(c));
    }

    @Transactional
    public ConvocatoriaResponse publicar(String correo, Long id) {
        EmpresaActual.Contexto ctx = empresaActual.obtenerAprobada(correo);
        Convocatoria c = buscarPropia(ctx, id);
        if (c.getEstado() != EstadoConvocatoria.BORRADOR) {
            throw new BadRequestException("Solo se puede publicar una convocatoria en borrador.");
        }
        if (c.getFechaCierre() != null && c.getFechaCierre().isBefore(LocalDate.now(clock))) {
            throw new BadRequestException("La fecha de cierre ya pasó: actualízala antes de publicar.");
        }
        c.publicar(ctx.usuario(), LocalDateTime.now(clock));
        return respuesta(c);
    }

    @Transactional
    public ConvocatoriaResponse cerrar(String correo, Long id) {
        Convocatoria c = buscarPropia(correo, id);
        if (c.getEstado() != EstadoConvocatoria.PUBLICADA) {
            throw new BadRequestException("Solo se puede cerrar una convocatoria publicada.");
        }
        c.cerrar();
        return respuesta(c);
    }

    @Transactional
    public ConvocatoriaResponse cambiarFechaCierre(String correo, Long id, LocalDate fechaCierre) {
        Convocatoria c = buscarPropia(correo, id);
        if (c.getEstado() == EstadoConvocatoria.BORRADOR) {
            throw new BadRequestException("Un borrador se edita completo: usa Editar.");
        }
        if (fechaCierre == null || fechaCierre.isBefore(LocalDate.now(clock))) {
            throw new CamposInvalidosException(Map.of("fechaCierre", "La fecha de cierre debe ser hoy o posterior."));
        }
        c.setFechaCierre(fechaCierre);
        if (c.getEstado() == EstadoConvocatoria.CERRADA) {
            c.reabrir();
        }
        return respuesta(c);
    }

    @Transactional
    public ConvocatoriaResponse mover(String correo, Long id, Long idBusqueda) {
        Convocatoria c = buscarPropia(correo, id);
        c.setBusqueda(busquedaPropia(c.getEmpresa().getId(), idBusqueda));
        return respuesta(c);
    }

    @Transactional
    public void eliminar(String correo, Long id) {
        Convocatoria c = buscarPropia(correo, id);
        if (c.getEstado() != EstadoConvocatoria.BORRADOR && postulacionRepository.countByConvocatoria_Id(c.getId()) > 0) {
            throw new BadRequestException("No se puede eliminar una convocatoria que ya tiene postulantes: ciérrala.");
        }
        convocatoriaRepository.delete(c);
    }


    private Convocatoria buscarPropia(String correo, Long id) {
        return buscarPropia(empresaActual.obtenerAprobada(correo), id);
    }

    private Convocatoria buscarPropia(EmpresaActual.Contexto ctx, Long id) {
        Long idEmpresa = ctx.empresa().getId();
        return convocatoriaRepository.findById(id)
                .filter(c -> c.getEmpresa().getId().equals(idEmpresa))
                .orElseThrow(() -> new ResourceNotFoundException("Convocatoria no encontrada"));
    }

    private Busqueda busquedaPropia(Long idEmpresa, Long idBusqueda) {
        if (idBusqueda == null) {
            return null;
        }
        return busquedaRepository.findById(idBusqueda)
                .filter(b -> b.getEmpresa().getId().equals(idEmpresa))
                .orElseThrow(() -> new CamposInvalidosException(Map.of("idBusqueda", "Búsqueda no válida.")));
    }

    private ConvocatoriaResponse respuesta(Convocatoria c) {
        return ConvocatoriaResponse.de(c, postulacionRepository.countByConvocatoria_Id(c.getId()),
                postulacionRepository.countByConvocatoria_IdAndEstadoIn(c.getId(), PENDIENTES),
                postulacionRepository.countByConvocatoria_IdAndEstadoIn(c.getId(), Set.of(EstadoPostulacion.CONFIRMADO)), null,
                null);
    }

    private record Validado(Ubigeo ubigeo, Map<Long, CatalogoCertificacion> tipos) {
    }

    private Validado validar(ConvocatoriaRequest r) {
        Map<String, String> errores = new LinkedHashMap<>();
        boolean parada = r.tipo() == TipoConvocatoria.PARADA_PLANTA;
        if (parada) {
            if (r.fechaInicioLabores() == null) {
                errores.put("fechaInicioLabores", "En una parada de planta indica cuándo empiezan las labores.");
            }
            if (r.fechaFinLabores() == null) {
                errores.put("fechaFinLabores", "En una parada de planta indica cuándo terminan las labores.");
            }
            if (r.direccion() == null || r.direccion().isBlank()) {
                errores.put("direccion", "En una parada de planta indica la dirección exacta donde debe presentarse.");
            }
        }
        if (r.fechaInicioLabores() != null && r.fechaFinLabores() != null
                && r.fechaFinLabores().isBefore(r.fechaInicioLabores())) {
            errores.put("fechaFinLabores", "La fecha de fin debe ser posterior a la de inicio.");
        }
        if (r.fechaCierre() != null && r.fechaCierre().isBefore(LocalDate.now(clock))) {
            errores.put("fechaCierre", "La fecha de cierre no puede estar en el pasado.");
        }
        List<ConvocatoriaRequest.RequisitoCertificadoRequest> certs = certificados(r);
        Map<Long, CatalogoCertificacion> tipos = catalogoRepository.findAllById(
                        certs.stream().map(ConvocatoriaRequest.RequisitoCertificadoRequest::idCatalogoCertificacion).toList())
                .stream().collect(Collectors.toMap(CatalogoCertificacion::getId, Function.identity()));
        Set<Long> vistos = new HashSet<>();
        for (int i = 0; i < certs.size(); i++) {
            Long idTipo = certs.get(i).idCatalogoCertificacion();
            String campo = "certificadosRequeridos[" + i + "].idCatalogoCertificacion";
            if (!tipos.containsKey(idTipo)) {
                errores.put(campo, "Certificado no válido.");
            } else if (!vistos.add(idTipo)) {
                errores.put(campo, "Este certificado ya está en la lista.");
            }
        }
        Ubigeo ubigeo = ubigeoRepository.findById(r.codigoUbigeo()).orElse(null);
        if (ubigeo == null) {
            errores.put("codigoUbigeo", "Distrito no válido.");
        }
        if (!errores.isEmpty()) {
            throw new CamposInvalidosException(errores);
        }
        return new Validado(ubigeo, tipos);
    }

    private void aplicar(Convocatoria c, ConvocatoriaRequest r, Validado v) {
        boolean parada = r.tipo() == TipoConvocatoria.PARADA_PLANTA;
        c.setTipo(r.tipo());
        c.setPuesto(r.puesto().trim());
        c.setCampamento(r.campamento().trim());
        c.setTurno(r.turno().trim());
        c.setDescripcion(r.descripcion().trim());
        c.setRequisitosObligatorios(r.requisitosObligatorios().trim());
        c.setRequisitosDeseables(texto(r.requisitosDeseables()));
        c.setExperienciaMinimaAnios(r.experienciaMinimaAnios() == null ? null : r.experienciaMinimaAnios().shortValue());
        c.setFechaInicioLabores(parada ? r.fechaInicioLabores() : null);
        c.setFechaFinLabores(parada ? r.fechaFinLabores() : null);
        c.setFechaCierre(r.fechaCierre());
        c.setUbigeo(v.ubigeo());
        c.setDireccion(texto(r.direccion()));
        c.setReferencia(texto(r.referencia()));
        c.setVacantes(r.vacantes() == null ? 1 : r.vacantes().shortValue());
        c.reemplazarRequisitos(certificados(r).stream()
                .map(x -> new RequisitoCertificado(c, v.tipos().get(x.idCatalogoCertificacion()), x.requierePdf()))
                .toList());
        c.setBusqueda(busquedaPropia(c.getEmpresa().getId(), r.idBusqueda()));
    }

    private static List<ConvocatoriaRequest.RequisitoCertificadoRequest> certificados(ConvocatoriaRequest r) {
        return r.certificadosRequeridos() == null ? List.of() : r.certificadosRequeridos();
    }

    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
