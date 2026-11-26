package com.mineria.reclutamiento.modules.postulacion.service;

import com.mineria.reclutamiento.core.exception.ResourceNotFoundException;
import com.mineria.reclutamiento.modules.empresa.service.EmpresaActual;
import com.mineria.reclutamiento.modules.postulacion.entity.Entrevista;
import com.mineria.reclutamiento.modules.postulacion.entity.Postulacion;
import com.mineria.reclutamiento.modules.postulacion.repository.PostulacionRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EntrevistaService {

    private final EmpresaActual empresaActual;
    private final PostulacionRepository postulacionRepository;
    private final Clock clock;

    public EntrevistaService(EmpresaActual empresaActual, PostulacionRepository postulacionRepository,
                             Clock clock) {
        this.empresaActual = empresaActual;
        this.postulacionRepository = postulacionRepository;
        this.clock = clock;
    }

    @Transactional
    public void citar(String correo, List<Long> idsPostulacion, Entrevista entrevista) {
        EmpresaActual.Contexto ctx = empresaActual.obtenerAprobada(correo);
        LocalDateTime ahora = LocalDateTime.now(clock);
        for (Long id : idsPostulacion) {
            Postulacion postulacion = postulacionRepository.findById(id)
                    .filter(p -> p.getConvocatoria().getEmpresa().getId().equals(ctx.empresa().getId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Postulación no encontrada"));
            postulacion.citar(entrevista, ahora);
        }
    }
}
