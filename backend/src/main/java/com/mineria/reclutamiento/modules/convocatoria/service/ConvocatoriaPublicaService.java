package com.mineria.reclutamiento.modules.convocatoria.service;

import com.mineria.reclutamiento.core.exception.ResourceNotFoundException;
import com.mineria.reclutamiento.modules.convocatoria.dto.ConvocatoriaResponse;
import com.mineria.reclutamiento.modules.convocatoria.entity.Convocatoria;
import com.mineria.reclutamiento.modules.convocatoria.entity.EstadoConvocatoria;
import com.mineria.reclutamiento.modules.convocatoria.entity.TipoConvocatoria;
import com.mineria.reclutamiento.modules.convocatoria.repository.ConvocatoriaRepository;
import com.mineria.reclutamiento.modules.empresa.entity.EstadoEmpresa;
import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConvocatoriaPublicaService {
    public record Filtros(String texto, TipoConvocatoria tipo, String departamento, String provincia, String distrito) {
    }

    private final ConvocatoriaRepository convocatoriaRepository;
    private final Clock clock;

    public ConvocatoriaPublicaService(ConvocatoriaRepository convocatoriaRepository, Clock clock) {
        this.convocatoriaRepository = convocatoriaRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ConvocatoriaResponse> buscar(Filtros f) {
        return convocatoriaRepository.findAll(disponibles(LocalDate.now(clock)).and(filtros(f)),
                        Sort.by(Sort.Direction.DESC, "fechaPublicacion"))
                .stream().map(this::respuesta).toList();
    }

    @Transactional(readOnly = true)
    public ConvocatoriaResponse detalle(Long id) {
        Convocatoria c = convocatoriaRepository.findById(id)
                .filter(x -> disponible(x, LocalDate.now(clock)))
                .orElseThrow(() -> new ResourceNotFoundException("Convocatoria no encontrada o ya no disponible"));
        return respuesta(c);
    }

    @Transactional(readOnly = true)
    public List<ConvocatoriaResponse> deEmpresa(Long idEmpresa) {
        Specification<Convocatoria> deLaEmpresa = (root, query, cb) -> cb.equal(root.get("empresa").get("id"), idEmpresa);
        return convocatoriaRepository.findAll(disponibles(LocalDate.now(clock)).and(deLaEmpresa),
                        Sort.by(Sort.Direction.DESC, "fechaPublicacion"))
                .stream().map(this::respuesta).toList();
    }

    private ConvocatoriaResponse respuesta(Convocatoria c) {
        return ConvocatoriaResponse.de(c, null, null, null);
    }

    public static boolean disponible(Convocatoria c, LocalDate hoy) {
        return c.getEstado() == EstadoConvocatoria.PUBLICADA
                && c.getEmpresa().getEstado() == EstadoEmpresa.APROBADA
                && (c.getFechaCierre() == null || !c.getFechaCierre().isBefore(hoy));
    }

    private static Specification<Convocatoria> disponibles(LocalDate hoy) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("estado"), EstadoConvocatoria.PUBLICADA),
                cb.equal(root.join("empresa").get("estado"), EstadoEmpresa.APROBADA),
                cb.or(cb.isNull(root.get("fechaCierre")), cb.greaterThanOrEqualTo(root.get("fechaCierre"), hoy)));
    }

    private static Specification<Convocatoria> filtros(Filtros f) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (f.tipo() != null) {
                p.add(cb.equal(root.get("tipo"), f.tipo()));
            }
            var ubigeo = root.join("ubigeo");
            if (tieneTexto(f.distrito())) {
                p.add(cb.equal(ubigeo.get("codigo"), f.distrito().trim()));
            } else if (tieneTexto(f.provincia())) {
                p.add(cb.equal(ubigeo.get("codProvincia"), f.provincia().trim()));
            } else if (tieneTexto(f.departamento())) {
                p.add(cb.equal(ubigeo.get("codDepartamento"), f.departamento().trim()));
            }
            if (tieneTexto(f.texto())) {
                String like = "%" + f.texto().trim().toLowerCase(Locale.ROOT) + "%";
                p.add(cb.or(
                        cb.like(cb.lower(root.get("puesto")), like),
                        cb.like(cb.lower(root.get("campamento")), like),
                        cb.like(cb.lower(root.join("empresa").get("razonSocial")), like)));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    private static boolean tieneTexto(String s) {
        return s != null && !s.isBlank();
    }
}
