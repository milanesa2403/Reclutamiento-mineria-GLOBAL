package com.mineria.reclutamiento.servicio;

import com.mineria.reclutamiento.excepcion.AccesoDenegadoException;
import com.mineria.reclutamiento.excepcion.RecursoNoEncontradoException;
import com.mineria.reclutamiento.modelo.*;
import com.mineria.reclutamiento.repositorio.*;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostulacionServicioImpl implements PostulacionServicio {
    private final PostulacionRepositorio postulaciones;
    private final OfertaRepositorio ofertas;
    private final DocumentoRepositorio documentos;
    private final EmpresaRepositorio empresas;
    private final OperacionRepositorio operaciones;
    private final AuditoriaServicio auditoria;

    public PostulacionServicioImpl(PostulacionRepositorio postulaciones, OfertaRepositorio ofertas,
            DocumentoRepositorio documentos, EmpresaRepositorio empresas, OperacionRepositorio operaciones,
            AuditoriaServicio auditoria) {
        this.postulaciones = postulaciones;
        this.ofertas = ofertas;
        this.documentos = documentos;
        this.empresas = empresas;
        this.operaciones = operaciones;
        this.auditoria = auditoria;
    }

    /** Todo ocurre en una transaccion: si algo falla a mitad de camino se revierte completo (RNF 23). */
    @Override
    @Transactional
    public Postulacion postular(String clave, UUID ofertaId, UUID trabajadorId, UUID cvId) {
        String k = "postular:" + trabajadorId + ":" + clave;
        var previa = operaciones.findById(k);
        if (previa.isPresent()) return buscar(UUID.fromString(previa.get().getResultado()));

        Oferta o = ofertas.findById(ofertaId).orElseThrow(() -> new RecursoNoEncontradoException("Oferta no encontrada"));
        if (o.getEstado() != EstadoOferta.ABIERTA) throw new IllegalStateException("La oferta no esta abierta");
        Documento cv = documentos.findById(cvId).orElseThrow(() -> new RecursoNoEncontradoException("CV no encontrado"));
        if (!cv.getPropietarioId().equals(trabajadorId) || cv.getTipo() != TipoDocumento.CV) {
            auditoria.registrar(trabajadorId, "ACCESO_DENEGADO", "documento/" + cvId, false);
            throw new AccesoDenegadoException("El CV no le pertenece");
        }
        Postulacion p = postulaciones.findByOfertaIdAndTrabajadorId(ofertaId, trabajadorId)
                .orElseGet(() -> postulaciones.save(new Postulacion(UUID.randomUUID(), ofertaId, trabajadorId, cvId)));
        operaciones.save(new OperacionIdempotente(k, p.getId().toString()));
        return p;
    }

    @Override
    @Transactional
    public Postulacion cambiarEstado(UUID usuarioEmpresaId, String clave, UUID postulacionId, EstadoPostulacion nuevo) {
        Postulacion p = buscar(postulacionId);
        verificarEmpresaDuena(usuarioEmpresaId, p.getOfertaId());
        String k = "estado:" + usuarioEmpresaId + ":" + clave;
        if (operaciones.existsById(k)) return p;
        p.setEstado(nuevo);
        operaciones.save(new OperacionIdempotente(k, p.getId().toString()));
        return p;
    }

    @Override
    public List<Postulacion> listarPorOferta(UUID usuarioEmpresaId, UUID ofertaId) {
        verificarEmpresaDuena(usuarioEmpresaId, ofertaId);
        return postulaciones.findByOfertaId(ofertaId);
    }

    @Override
    public List<Postulacion> listarDeTrabajador(UUID trabajadorId) { return postulaciones.findByTrabajadorId(trabajadorId); }

    private Postulacion buscar(UUID id) {
        return postulaciones.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Postulacion no encontrada"));
    }

    private void verificarEmpresaDuena(UUID usuarioEmpresaId, UUID ofertaId) {
        Oferta o = ofertas.findById(ofertaId).orElseThrow(() -> new RecursoNoEncontradoException("Oferta no encontrada"));
        boolean duena = empresas.findByUsuarioId(usuarioEmpresaId).map(e -> e.getId().equals(o.getEmpresaId())).orElse(false);
        if (!duena) {
            auditoria.registrar(usuarioEmpresaId, "ACCESO_DENEGADO", "oferta/" + ofertaId, false);
            throw new AccesoDenegadoException("La oferta pertenece a otra empresa");
        }
    }
}
