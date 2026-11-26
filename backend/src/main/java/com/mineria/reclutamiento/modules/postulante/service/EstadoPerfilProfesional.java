package com.mineria.reclutamiento.modules.postulante.service;

import com.mineria.reclutamiento.modules.postulante.repository.EspecialidadPostulanteRepository;
import com.mineria.reclutamiento.modules.postulante.repository.HabilidadPostulanteRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class EstadoPerfilProfesional {

    public static final String PENDIENTE_ESPECIALIDAD = "especialidad";
    public static final String PENDIENTE_HABILIDADES = "habilidades";

    private final EspecialidadPostulanteRepository especialidadRepository;
    private final HabilidadPostulanteRepository habilidadRepository;

    public EstadoPerfilProfesional(EspecialidadPostulanteRepository especialidadRepository,
                                   HabilidadPostulanteRepository habilidadRepository) {
        this.especialidadRepository = especialidadRepository;
        this.habilidadRepository = habilidadRepository;
    }

    public List<String> pendientes(Long idPostulante) {
        List<String> pendientes = new ArrayList<>();
        if (!especialidadRepository.tienePrincipal(idPostulante)) {
            pendientes.add(PENDIENTE_ESPECIALIDAD);
        }
        if (!habilidadRepository.existsByPostulante_IdPostulante(idPostulante)) {
            pendientes.add(PENDIENTE_HABILIDADES);
        }
        return pendientes;
    }
}
