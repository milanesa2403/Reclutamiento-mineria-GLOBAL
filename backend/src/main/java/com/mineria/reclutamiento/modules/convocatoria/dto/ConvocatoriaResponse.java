package com.mineria.reclutamiento.modules.convocatoria.dto;

import com.mineria.reclutamiento.modules.convocatoria.entity.Convocatoria;
import com.mineria.reclutamiento.modules.convocatoria.entity.EstadoConvocatoria;
import com.mineria.reclutamiento.modules.convocatoria.entity.TipoConvocatoria;
import com.mineria.reclutamiento.modules.ubigeo.dto.UbicacionResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ConvocatoriaResponse(
        Long id,
        TipoConvocatoria tipo,
        String puesto,
        String campamento,
        String turno,
        String descripcion,
        String requisitosObligatorios,
        String requisitosDeseables,
        Short experienciaMinimaAnios,
        LocalDate fechaInicioLabores,
        LocalDate fechaFinLabores,
        LocalDate fechaCierre,
        EstadoConvocatoria estado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaPublicacion,
        UbicacionResponse ubicacion,
        String direccion,
        String referencia,
        List<CertificadoRequerido> certificadosRequeridos,
        Empresa empresa,
        Long totalPostulaciones,
        Long postulacionesPendientes,
        Long postulacionesAceptadas,
        Boolean guardada,
        int vacantes,
        Busqueda busqueda
) {

    public record CertificadoRequerido(Long idRequisito, Long idCatalogoCertificacion, String nombre, boolean requierePdf) {
    }

    public record Empresa(Long id, String razonSocial, String sectorMinero, String descripcion, String direccion,
                          String telefono, String correoCorporativo, String logoUrl) {
    }

    public record Busqueda(Long id, String nombre) {
    }

    public static ConvocatoriaResponse de(Convocatoria c, Long totalPostulaciones, Boolean guardada, String logoUrl) {
        return de(c, totalPostulaciones, null, null, guardada, logoUrl);
    }

    public static ConvocatoriaResponse de(Convocatoria c, Long totalPostulaciones, Long postulacionesPendientes,
                                          Long postulacionesAceptadas, Boolean guardada, String logoUrl) {
        var e = c.getEmpresa();
        return new ConvocatoriaResponse(
                c.getId(), c.getTipo(), c.getPuesto(), c.getCampamento(), c.getTurno(), c.getDescripcion(),
                c.getRequisitosObligatorios(), c.getRequisitosDeseables(), c.getExperienciaMinimaAnios(),
                c.getFechaInicioLabores(), c.getFechaFinLabores(), c.getFechaCierre(), c.getEstado(),
                c.getFechaCreacion(), c.getFechaPublicacion(), UbicacionResponse.de(c.getUbigeo()),
                c.getDireccion(), c.getReferencia(),
                c.getRequisitosCertificado().stream()
                        .map(r -> new CertificadoRequerido(r.getId(), r.getCatalogo().getId(), r.getNombre(),
                                r.isRequierePdf())).toList(),
                new Empresa(e.getId(), e.getRazonSocial(),
                        e.getSectorMinero() != null ? e.getSectorMinero().getNombre() : null, e.getDescripcion(),
                        e.getDireccion(), e.getTelefono(), e.getCorreoCorporativo(), logoUrl),
                totalPostulaciones, postulacionesPendientes, postulacionesAceptadas, guardada, c.getVacantes(),
                c.getBusqueda() != null ? new Busqueda(c.getBusqueda().getId(), c.getBusqueda().getNombre()) : null);
    }
}
