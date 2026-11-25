package com.mineria.reclutamiento.servicio;

import com.mineria.reclutamiento.excepcion.RecursoNoEncontradoException;
import com.mineria.reclutamiento.modelo.Perfil;
import com.mineria.reclutamiento.repositorio.PerfilRepositorio;
import com.mineria.reclutamiento.util.EstandaresIso;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PerfilServicioImpl implements PerfilServicio {
    private final PerfilRepositorio perfiles;
    private final AuditoriaServicio auditoria;

    public PerfilServicioImpl(PerfilRepositorio perfiles, AuditoriaServicio auditoria) {
        this.perfiles = perfiles;
        this.auditoria = auditoria;
    }

    @Override
    public Perfil obtener(UUID usuarioId) {
        return perfiles.findById(usuarioId).orElseThrow(() -> new RecursoNoEncontradoException("Perfil no encontrado"));
    }

    @Override
    @Transactional
    public Perfil actualizar(UUID usuarioId, DatosPerfil d) {
        if (d.aniosExperiencia() < 0 || d.aniosExperiencia() > 70) throw new IllegalArgumentException("Anios de experiencia fuera de rango");
        Perfil p = obtener(usuarioId);
        p.setNombreCompleto(d.nombreCompleto());
        p.setPais(EstandaresIso.pais(d.pais()));
        p.setIdioma(EstandaresIso.idioma(d.idioma()));
        p.setEspecialidad(d.especialidad());
        p.setAniosExperiencia(d.aniosExperiencia());
        auditoria.registrar(usuarioId, "MODIFICACION_PERFIL", "perfil", true);
        return p;
    }

    @Override
    public List<Perfil> filtrar(UUID solicitanteId, String especialidad, int minAnios) {
        auditoria.registrar(solicitanteId, "CONSULTA_PERFILES", "perfiles", true);
        return perfiles.findByEspecialidadContainingIgnoreCaseAndAniosExperienciaGreaterThanEqual(
                especialidad == null ? "" : especialidad.trim(), Math.max(0, minAnios));
    }
}
