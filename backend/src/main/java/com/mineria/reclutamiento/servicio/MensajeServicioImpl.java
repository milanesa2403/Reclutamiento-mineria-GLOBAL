package com.mineria.reclutamiento.servicio;

import com.mineria.reclutamiento.excepcion.AccesoDenegadoException;
import com.mineria.reclutamiento.excepcion.RecursoNoEncontradoException;
import com.mineria.reclutamiento.modelo.Mensaje;
import com.mineria.reclutamiento.modelo.OperacionIdempotente;
import com.mineria.reclutamiento.modelo.Postulacion;
import com.mineria.reclutamiento.repositorio.*;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Mensajes de entrevista entre el trabajador y la empresa duena de la oferta. */
@Service
public class MensajeServicioImpl implements MensajeServicio {
    private final MensajeRepositorio mensajes;
    private final PostulacionRepositorio postulaciones;
    private final OfertaRepositorio ofertas;
    private final EmpresaRepositorio empresas;
    private final OperacionRepositorio operaciones;
    private final AuditoriaServicio auditoria;

    public MensajeServicioImpl(MensajeRepositorio mensajes, PostulacionRepositorio postulaciones, OfertaRepositorio ofertas,
            EmpresaRepositorio empresas, OperacionRepositorio operaciones, AuditoriaServicio auditoria) {
        this.mensajes = mensajes;
        this.postulaciones = postulaciones;
        this.ofertas = ofertas;
        this.empresas = empresas;
        this.operaciones = operaciones;
        this.auditoria = auditoria;
    }

    @Override
    @Transactional
    public Mensaje enviar(String clave, UUID postulacionId, UUID remitenteId, String contenido) {
        if (contenido == null || contenido.isBlank() || contenido.length() > 2000)
            throw new IllegalArgumentException("El mensaje debe tener entre 1 y 2000 caracteres");
        verificarParticipante(remitenteId, postulacionId);
        String k = "mensaje:" + remitenteId + ":" + clave;
        var previa = operaciones.findById(k);
        if (previa.isPresent()) return mensajes.findById(UUID.fromString(previa.get().getResultado())).orElseThrow();
        Mensaje m = mensajes.save(new Mensaje(UUID.randomUUID(), postulacionId, remitenteId, contenido.trim()));
        operaciones.save(new OperacionIdempotente(k, m.getId().toString()));
        return m;
    }

    @Override
    public List<Mensaje> listar(UUID usuarioId, UUID postulacionId) {
        verificarParticipante(usuarioId, postulacionId);
        return mensajes.findByPostulacionIdOrderByFechaAsc(postulacionId);
    }

    private void verificarParticipante(UUID usuarioId, UUID postulacionId) {
        Postulacion p = postulaciones.findById(postulacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Postulacion no encontrada"));
        boolean esTrabajador = p.getTrabajadorId().equals(usuarioId);
        boolean esEmpresa = ofertas.findById(p.getOfertaId()).flatMap(o ->
                empresas.findByUsuarioId(usuarioId).map(e -> e.getId().equals(o.getEmpresaId()))).orElse(false);
        if (!esTrabajador && !esEmpresa) {
            auditoria.registrar(usuarioId, "ACCESO_DENEGADO", "postulacion/" + postulacionId + "/mensajes", false);
            throw new AccesoDenegadoException("No participa en esta postulacion");
        }
    }
}
