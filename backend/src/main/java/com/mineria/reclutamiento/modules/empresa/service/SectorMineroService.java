package com.mineria.reclutamiento.modules.empresa.service;

import com.mineria.reclutamiento.core.exception.BadRequestException;
import com.mineria.reclutamiento.modules.empresa.dto.response.SectorMineroResponse;
import com.mineria.reclutamiento.modules.empresa.entity.SectorMinero;
import com.mineria.reclutamiento.modules.empresa.repository.SectorMineroRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SectorMineroService {

    private final SectorMineroRepository repository;

    public SectorMineroService(SectorMineroRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<SectorMineroResponse> listarActivos() {
        return repository.findByActivoTrueOrderByIdAsc().stream()
                .map(s -> new SectorMineroResponse(s.getId(), s.getNombre()))
                .toList();
    }

    @Transactional(readOnly = true)
    public SectorMinero obtenerActivo(Long idSector) {
        return repository.findById(idSector)
                .filter(SectorMinero::isActivo)
                .orElseThrow(() -> new BadRequestException("El sector minero seleccionado no es válido"));
    }
}
