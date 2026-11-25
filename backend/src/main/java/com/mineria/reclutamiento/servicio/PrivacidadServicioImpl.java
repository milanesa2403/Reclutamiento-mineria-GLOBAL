package com.mineria.reclutamiento.servicio;

import com.mineria.reclutamiento.excepcion.AccesoDenegadoException;
import com.mineria.reclutamiento.excepcion.RecursoNoEncontradoException;
import com.mineria.reclutamiento.modelo.*;
import com.mineria.reclutamiento.repositorio.*;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrivacidadServicioImpl implements PrivacidadServicio {
    private final UsuarioRepositorio usuarios;
    private final PerfilRepositorio perfiles;
    private final EmpresaRepositorio empresas;
    private final OfertaRepositorio ofertas;
    private final PostulacionRepositorio postulaciones;
    private final MensajeRepositorio mensajes;
    private final DocumentoRepositorio documentos;
    private final AuditoriaRepositorio auditoriaRepo;
    private final AlmacenArchivos almacen;
    private final AuditoriaServicio auditoria;
    private final int diasGracia;

    public PrivacidadServicioImpl(UsuarioRepositorio usuarios, PerfilRepositorio perfiles, EmpresaRepositorio empresas,
            OfertaRepositorio ofertas, PostulacionRepositorio postulaciones, MensajeRepositorio mensajes,
            DocumentoRepositorio documentos, AuditoriaRepositorio auditoriaRepo, AlmacenArchivos almacen,
            AuditoriaServicio auditoria, @Value("${privacidad.dias-gracia-borrado:7}") int diasGracia) {
        this.usuarios = usuarios;
        this.perfiles = perfiles;
        this.empresas = empresas;
        this.ofertas = ofertas;
        this.postulaciones = postulaciones;
        this.mensajes = mensajes;
        this.documentos = documentos;
        this.auditoriaRepo = auditoriaRepo;
        this.almacen = almacen;
        this.auditoria = auditoria;
        this.diasGracia = diasGracia;
    }

    @Override
    @Transactional
    public void solicitarBorrado(UUID usuarioId) {
        Usuario u = usuarios.findById(usuarioId).orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        if (u.getRol() == Rol.ADMINISTRADOR) throw new AccesoDenegadoException("Un administrador no puede solicitar su borrado");
        if (u.getBorradoSolicitadoEn() == null) u.setBorradoSolicitadoEn(Instant.now());
        auditoria.registrar(usuarioId, "SOLICITUD_BORRADO", "usuario", true);
    }

    @Override
    @Transactional
    public int purgar() {
        List<Usuario> vencidos = usuarios.findByBorradoSolicitadoEnBefore(Instant.now().minus(Duration.ofDays(diasGracia)));
        vencidos.forEach(this::borrar);
        auditoriaRepo.deleteByFechaBefore(Instant.now().minus(Duration.ofDays(90)));
        return vencidos.size();
    }

    private void borrar(Usuario u) {
        UUID id = u.getId();
        if (u.getRol() == Rol.TRABAJADOR) {
            for (Postulacion p : postulaciones.findByTrabajadorId(id)) {
                mensajes.deleteByPostulacionId(p.getId());
                postulaciones.delete(p);
            }
            for (Documento d : documentos.findByPropietarioId(id)) {
                documentos.delete(d);
                documentos.flush();
                if (!documentos.existsBySha256(d.getSha256())) almacen.eliminar(d.getSha256());
            }
            perfiles.deleteById(id);
        } else {
            empresas.findByUsuarioId(id).ifPresent(e -> {
                ofertas.findByEmpresaId(e.getId()).forEach(o -> o.setEstado(EstadoOferta.ARCHIVADA));
                empresas.delete(e);
            });
        }
        mensajes.deleteByRemitenteId(id);
        usuarios.delete(u);
        auditoria.registrar(null, "BORRADO_DATOS", "usuario/" + id, true);
    }
}
