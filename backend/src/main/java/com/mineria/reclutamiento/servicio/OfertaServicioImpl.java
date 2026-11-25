package com.mineria.reclutamiento.servicio;

import com.mineria.reclutamiento.excepcion.AccesoDenegadoException;
import com.mineria.reclutamiento.excepcion.RecursoNoEncontradoException;
import com.mineria.reclutamiento.modelo.*;
import com.mineria.reclutamiento.repositorio.EmpresaRepositorio;
import com.mineria.reclutamiento.repositorio.OfertaRepositorio;
import com.mineria.reclutamiento.util.EstandaresIso;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfertaServicioImpl implements OfertaServicio {
    private final OfertaRepositorio ofertas;
    private final EmpresaRepositorio empresas;
    private final AuditoriaServicio auditoria;

    public OfertaServicioImpl(OfertaRepositorio ofertas, EmpresaRepositorio empresas, AuditoriaServicio auditoria) {
        this.ofertas = ofertas;
        this.empresas = empresas;
        this.auditoria = auditoria;
    }

    @Override
    public Empresa empresaDe(UUID usuarioId) {
        return empresas.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new AccesoDenegadoException("El usuario no tiene una empresa asociada"));
    }

    @Override
    @Transactional(noRollbackFor = IllegalArgumentException.class) // validaciones fallan antes de guardar
    public Oferta publicar(UUID usuarioEmpresaId, DatosOferta d) {
        Empresa e = empresaDe(usuarioEmpresaId);
        if (d.titulo() == null || d.titulo().isBlank()) throw new IllegalArgumentException("El titulo es obligatorio");
        if (d.salario() < 0) throw new IllegalArgumentException("El salario no puede ser negativo");
        Oferta o = new Oferta(UUID.randomUUID(), e.getId(), d.titulo().trim());
        o.setDescripcion(d.descripcion());
        o.setEspecialidadRequerida(d.especialidadRequerida());
        o.setPais(EstandaresIso.pais(d.pais()));
        o.setSalario(d.salario());
        o.setMoneda(EstandaresIso.moneda(d.moneda()));
        return ofertas.save(o);
    }

    @Override
    public Oferta obtener(UUID ofertaId) {
        return ofertas.findById(ofertaId).orElseThrow(() -> new RecursoNoEncontradoException("Oferta no encontrada"));
    }

    @Override
    public List<Oferta> buscar(String especialidad, String pais) {
        String p = (pais == null || pais.isBlank()) ? "" : pais.trim().toUpperCase();
        return ofertas.buscar(especialidad == null ? "" : especialidad.trim(), p, PageRequest.of(0, 100));
    }

    @Override
    public List<Oferta> listarDeEmpresa(UUID usuarioEmpresaId) {
        return ofertas.findByEmpresaIdOrderByFechaPublicacionDesc(empresaDe(usuarioEmpresaId).getId());
    }

    @Override
    @Transactional
    public Oferta cerrar(UUID usuarioEmpresaId, UUID ofertaId) {
        Empresa e = empresaDe(usuarioEmpresaId);
        Oferta o = obtener(ofertaId);
        if (!o.getEmpresaId().equals(e.getId())) {
            auditoria.registrar(usuarioEmpresaId, "ACCESO_DENEGADO", "oferta/" + ofertaId, false);
            throw new AccesoDenegadoException("La oferta pertenece a otra empresa");
        }
        if (o.getEstado() == EstadoOferta.ABIERTA) {
            o.setEstado(EstadoOferta.CERRADA);
            o.setFechaCierre(Instant.now());
        }
        return o;
    }

    @Override
    @Transactional
    public int archivarAntiguas() {
        List<Oferta> viejas = ofertas.findByEstadoAndFechaCierreBefore(
                EstadoOferta.CERRADA, Instant.now().minus(Duration.ofDays(180)));
        viejas.forEach(o -> o.setEstado(EstadoOferta.ARCHIVADA));
        return viejas.size();
    }
}
