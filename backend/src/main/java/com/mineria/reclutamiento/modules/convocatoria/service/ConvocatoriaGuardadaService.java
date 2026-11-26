package com.mineria.reclutamiento.modules.convocatoria.service;

import com.mineria.reclutamiento.core.exception.ResourceNotFoundException;
import com.mineria.reclutamiento.modules.convocatoria.dto.ConvocatoriaResponse;
import com.mineria.reclutamiento.modules.convocatoria.entity.Convocatoria;
import com.mineria.reclutamiento.modules.convocatoria.entity.ConvocatoriaGuardada;
import com.mineria.reclutamiento.modules.convocatoria.repository.ConvocatoriaGuardadaRepository;
import com.mineria.reclutamiento.modules.convocatoria.repository.ConvocatoriaRepository;
import com.mineria.reclutamiento.modules.postulante.entity.Postulante;
import com.mineria.reclutamiento.modules.postulante.service.PostulanteService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConvocatoriaGuardadaService {

    private final PostulanteService postulanteService;
    private final ConvocatoriaRepository convocatoriaRepository;
    private final ConvocatoriaGuardadaRepository guardadaRepository;
    private final Clock clock;

    public ConvocatoriaGuardadaService(PostulanteService postulanteService, ConvocatoriaRepository convocatoriaRepository,
                                       ConvocatoriaGuardadaRepository guardadaRepository, Clock clock) {
        this.postulanteService = postulanteService;
        this.convocatoriaRepository = convocatoriaRepository;
        this.guardadaRepository = guardadaRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ConvocatoriaResponse> listar(String correo) {
        Postulante p = postulanteService.postulanteDe(correo);
        return guardadaRepository.delPostulante(p.getIdPostulante()).stream()
                .map(g -> ConvocatoriaResponse.de(g.getConvocatoria(), null, true,
                        null))
                .toList();
    }

    @Transactional
    public void guardar(String correo, Long idConvocatoria) {
        Postulante p = postulanteService.postulanteDe(correo);
        if (guardadaRepository.buscar(p.getIdPostulante(), idConvocatoria)
                .isPresent()) {
            return;
        }
        Convocatoria c = convocatoriaRepository.findById(idConvocatoria)
                .filter(x -> ConvocatoriaPublicaService.disponible(x, LocalDate.now(clock)))
                .orElseThrow(() -> new ResourceNotFoundException("Convocatoria no encontrada o ya no disponible"));
        guardadaRepository.saveAndFlush(new ConvocatoriaGuardada(p, c));
    }

    @Transactional
    public void quitar(String correo, Long idConvocatoria) {
        Postulante p = postulanteService.postulanteDe(correo);
        guardadaRepository.buscar(p.getIdPostulante(), idConvocatoria)
                .ifPresent(guardadaRepository::delete);
    }
}
